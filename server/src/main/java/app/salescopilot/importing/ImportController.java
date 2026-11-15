package app.salescopilot.importing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.salescopilot.business.api.BusinessAccess;
import app.salescopilot.playbook.api.PlaybookDrafts;
import app.salescopilot.shared.ApiException;
import app.salescopilot.shared.Tenancy;

@RestController
@RequestMapping("/api/businesses/{businessId}/playbook/imports")
class ImportController {

    static final Set<String> EXTENSIONS = Set.of("docx", "pdf", "txt", "md");
    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    record Source(UUID playbookId, String text, String googleDocsUrl) {
    }

    private interface FileWriter {
        String write(UUID jobId);
    }

    private final Tenancy tenancy;
    private final ImportJobs jobs;
    private final ImportFiles files;
    private final PlaybookDrafts drafts;

    ImportController(Tenancy tenancy, ImportJobs jobs, ImportFiles files, PlaybookDrafts drafts) {
        this.tenancy = tenancy;
        this.jobs = jobs;
        this.files = files;
        this.drafts = drafts;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, UUID> fromFile(BusinessAccess access, @RequestParam UUID playbookId, @RequestParam MultipartFile file) {
        access.requireGestor();
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "file_too_large", "O arquivo passa de 10 MB.");
        }
        String extension = extension(file.getOriginalFilename());
        if (!EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_format",
                    "Formato não aceito. Envie .docx, .pdf, .txt ou .md.");
        }
        return enqueue(access, playbookId, "file", null, jobId -> {
            try {
                return files.write(jobId, extension, file.getInputStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, UUID> fromSource(BusinessAccess access, @RequestBody Source source) {
        access.requireGestor();
        boolean hasText = source.text() != null && !source.text().isBlank();
        boolean hasUrl = source.googleDocsUrl() != null && !source.googleDocsUrl().isBlank();
        if (hasText == hasUrl) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid_source",
                    "Envie um arquivo, um texto ou um link do Google Docs.");
        }
        if (hasText) {
            byte[] bytes = source.text().getBytes(StandardCharsets.UTF_8);
            return enqueue(access, source.playbookId(), "text", null,
                    jobId -> files.write(jobId, "txt", new ByteArrayInputStream(bytes)));
        }
        return enqueue(access, source.playbookId(), "google_docs", source.googleDocsUrl().strip(), jobId -> null);
    }

    /** Trava o playbook antes de conferir a importação ativa, como a gravação do rascunho (AD-050). */
    private Map<String, UUID> enqueue(BusinessAccess access, UUID playbookId, String kind, String url, FileWriter writer) {
        if (playbookId == null) {
            throw ApiException.notFound();
        }
        UUID jobId = UUID.randomUUID();
        tenancy.runInBusiness(access.userId(), access.businessId(), () -> {
            drafts.lock(playbookId);
            if (jobs.isActive(playbookId)) {
                throw new ApiException(HttpStatus.CONFLICT, "import_running", "Há uma importação em andamento.");
            }
            String fileName = writer.write(jobId);
            jobs.insert(new ImportJobs.Job(jobId, access.businessId(), playbookId, "queued", kind, fileName, url));
        });
        return Map.of("jobId", jobId);
    }

    private static String extension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') < 0) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
