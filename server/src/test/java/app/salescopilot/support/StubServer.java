package app.salescopilot.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Faz o papel do OpenRouter (`/v1/chat/completions`) e da exportação do Google Docs
 * (`/document/d/<id>/export`), guardando o corpo de cada chamada ao modelo.
 */
public final class StubServer {

    public record Reply(int status, String contentType, byte[] body) {

        public static Reply json(int status, String body) {
            return new Reply(status, "application/json", body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private final HttpServer server;
    private final ConcurrentLinkedQueue<Reply> modelReplies = new ConcurrentLinkedQueue<>();
    private final List<String> modelRequests = new CopyOnWriteArrayList<>();
    private final Map<String, Reply> documents = new ConcurrentHashMap<>();
    private volatile long modelDelayMillis;

    private StubServer(HttpServer server) {
        this.server = server;
        server.createContext("/v1/chat/completions", this::model);
        server.createContext("/document/d/", this::document);
        server.start();
    }

    public static StubServer start() {
        try {
            return new StubServer(HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        modelDelayMillis = 0;
        modelReplies.clear();
        modelRequests.clear();
        documents.clear();
    }

    /** O modelo responde com este JSON como conteúdo da mensagem. */
    public void modelAnswers(String playbookJson) {
        modelReplies.add(Reply.json(200, completion(playbookJson)));
    }

    public void modelFails(int status) {
        modelReplies.add(Reply.json(status, "{\"error\":{\"message\":\"falha simulada\"}}"));
    }

    /** Atraso antes de cada resposta do modelo, para provar o tempo limite. */
    public void modelDelay(java.time.Duration delay) {
        modelDelayMillis = delay.toMillis();
    }

    public List<String> modelRequests() {
        return modelRequests;
    }

    public void document(String id, Reply reply) {
        documents.put(id, reply);
    }

    private void model(HttpExchange exchange) throws IOException {
        modelRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (modelDelayMillis > 0) {
            try {
                Thread.sleep(modelDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Reply reply = modelReplies.poll();
        respond(exchange, reply == null ? Reply.json(500, "{\"error\":{\"message\":\"sem resposta programada\"}}") : reply);
    }

    private void document(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String id = path.substring("/document/d/".length(), path.indexOf("/export"));
        Reply reply = documents.getOrDefault(id, new Reply(404, "text/html", "<html>login</html>".getBytes()));
        respond(exchange, reply);
    }

    private static void respond(HttpExchange exchange, Reply reply) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", reply.contentType());
        exchange.sendResponseHeaders(reply.status(), reply.body().length);
        exchange.getResponseBody().write(reply.body());
        exchange.close();
    }

    private static String completion(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return """
                {"id":"chatcmpl-test","object":"chat.completion","created":1700000000,"model":"anthropic/claude-sonnet-test",
                 "choices":[{"index":0,"message":{"role":"assistant","content":"%s","refusal":null},"finish_reason":"stop","logprobs":null}],
                 "usage":{"prompt_tokens":10,"completion_tokens":10,"total_tokens":20}}""".formatted(escaped);
    }
}
