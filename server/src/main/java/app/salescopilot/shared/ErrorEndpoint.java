package app.salescopilot.shared;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Substitui o /error do Boot: o que o contêiner despacha para cá (método como TRACE, erro fora do MVC)
 * também sai em RFC 9457 (AD-057).
 */
@RestController
class ErrorEndpoint implements ErrorController {

    @RequestMapping("/error")
    ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.resolve(code instanceof Integer value ? value : 500);
        if (status == null || !status.isError()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        ProblemDetail problem = Problems.of(status, Problems.codeForStatus(status.value()), status.getReasonPhrase(),
                java.util.List.of(), uri == null ? "/error" : uri.toString());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
