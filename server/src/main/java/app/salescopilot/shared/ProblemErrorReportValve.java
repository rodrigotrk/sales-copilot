package app.salescopilot.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import tools.jackson.databind.json.JsonMapper;

/**
 * Erros que o Tomcat responde antes de chegar ao Spring (endereço com `%2F`, `%00`, codificação
 * inválida) saem em RFC 9457 em vez da página HTML do Tomcat.
 */
public class ProblemErrorReportValve extends ErrorReportValve {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        HttpStatus resolved = HttpStatus.resolve(status);
        String code = Problems.codeForStatus(status);
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "/problems/" + code);
        problem.put("title", resolved == null ? "Error" : resolved.getReasonPhrase());
        problem.put("status", status);
        problem.put("detail", Problems.detailFor(code));
        // O endereço cru, como chegou: decodificá-lo é justamente o que falhou. Quando o Tomcat recusa a
        // própria linha do pedido (byte não ASCII no caminho), nenhum caminho foi guardado e vale "/".
        String uri = request.getRequestURI();
        problem.put("instance", uri == null || uri.isEmpty() ? "/" : uri);
        problem.put("code", code);
        try {
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getOutputStream().write(JSON.writeValueAsBytes(problem));
            response.finishResponse();
        } catch (IOException | IllegalStateException e) {
            // A conexão já caiu ou a resposta já foi enviada: não há a quem responder.
        }
    }
}
