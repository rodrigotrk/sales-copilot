package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Arquivos em importação no disco da VPS (AD-028). O nome é `<jobId>.<extensão>`. */
@Component
class ImportFiles {

    private final Path directory;

    ImportFiles(@Value("${app.import.directory}") Path directory) {
        this.directory = directory;
    }

    String write(UUID jobId, String extension, InputStream content) {
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(jobId + "." + extension);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return target.getFileName().toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path resolve(String fileName) {
        return directory.resolve(fileName);
    }

    void delete(String fileName) {
        try {
            Files.deleteIfExists(directory.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record StoredFile(String fileName, UUID jobId, Instant modifiedAt) {
    }

    List<StoredFile> list() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> {
                String name = path.getFileName().toString();
                int dot = name.indexOf('.');
                UUID jobId;
                try {
                    jobId = UUID.fromString(dot < 0 ? name : name.substring(0, dot));
                } catch (IllegalArgumentException e) {
                    jobId = null;
                }
                try {
                    return new StoredFile(name, jobId, Files.getLastModifiedTime(path).toInstant());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
