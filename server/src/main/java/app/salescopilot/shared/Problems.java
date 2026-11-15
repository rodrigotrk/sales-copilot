package app.salescopilot.shared;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.json.JsonMapper;

/**
 * Converte erros em Problem Details da RFC 9457 (AD-057), com as extensões `code` e `errors`. Os erros
 * do próprio Spring (JSON malformado, parâmetro ausente, método ou tipo de conteúdo não aceito) passam
 * pelo {@link ResponseEntityExceptionHandler} e ganham o mesmo `code` e `type`.
 */
@RestControllerAdvice
public class Problems extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Problems.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handle(ApiException e, HttpServletRequest request) {
        return of(e.status(), e.code(), e.getMessage(), e.errors(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(HttpServletRequest request) {
        return of(HttpStatus.BAD_REQUEST, "invalid_parameter", "Parâmetro inválido.", List.of(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), e);
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Erro inesperado. Tente de novo.", List.of(),
                request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(of(HttpStatus.CONTENT_TOO_LARGE,
                "file_too_large", "O arquivo passa de 10 MB.", List.of(), path(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail : ProblemDetail.forStatus(status);
        String code = codeFor(e);
        problem.setType(URI.create("/problems/" + code));
        problem.setProperty("code", code);
        problem.setDetail(detailFor(code));
        if (problem.getInstance() == null) {
            problem.setInstance(URI.create(path(request)));
        }
        return super.handleExceptionInternal(e, problem, headers, status, request);
    }

    private static String codeFor(Exception e) {
        return switch (e) {
            case HttpMessageNotReadableException ignored -> "malformed_request";
            case MissingServletRequestParameterException ignored -> "missing_parameter";
            case HttpRequestMethodNotSupportedException ignored -> "method_not_allowed";
            case HttpMediaTypeNotSupportedException ignored -> "unsupported_media_type";
            default -> "invalid_request";
        };
    }

    /** Texto do `detail` para os erros que o contrato não descreve um a um; o Surface exige o membro sempre. */
    public static String detailFor(String code) {
        return switch (code) {
            case "malformed_request" -> "O corpo da requisição não é um JSON válido.";
            case "missing_parameter" -> "Falta um parâmetro obrigatório.";
            case "method_not_allowed" -> "Método não permitido neste endereço.";
            case "unsupported_media_type" -> "Tipo de conteúdo não aceito.";
            case "unauthenticated" -> "Entre para continuar.";
            case "forbidden" -> "Sem permissão para esta ação.";
            case "not_found" -> "Endereço não encontrado.";
            case "file_too_large" -> "O arquivo passa de 10 MB.";
            case "internal_error" -> "Erro inesperado. Tente de novo.";
            default -> "Requisição inválida.";
        };
    }

    /** Código do contrato para um erro que só traz o status, como os despachados ao /error ou recusados pelo Tomcat. */
    public static String codeForStatus(int status) {
        return switch (status) {
            case 400 -> "invalid_request";
            case 401 -> "unauthenticated";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 405 -> "method_not_allowed";
            case 413 -> "file_too_large";
            case 415 -> "unsupported_media_type";
            default -> status >= 500 ? "internal_error" : "invalid_request";
        };
    }

    private static String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
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
