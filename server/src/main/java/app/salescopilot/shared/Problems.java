package app.salescopilot.shared;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import tools.jackson.databind.json.JsonMapper;

/** Converte erros em Problem Details da RFC 9457 (AD-057). */
@RestControllerAdvice
public class Problems {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handle(ApiException e, HttpServletRequest request) {
        return of(e.status(), e.code(), e.getMessage(), e.errors(), request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(HttpServletRequest request) {
        return of(HttpStatus.CONTENT_TOO_LARGE, "file_too_large", "O arquivo passa de 10 MB.", List.of(),
                request.getRequestURI());
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail, List<FieldProblem> errors,
            String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", code);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    /** Para filtros e handlers do Spring Security, que escrevem a resposta fora do MVC. */
    public static void write(HttpServletResponse response, JsonMapper json, HttpStatus status, String code,
            String detail, String instance) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), of(status, code, detail, List.of(), instance));
    }
}
