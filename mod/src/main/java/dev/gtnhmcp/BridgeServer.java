package dev.gtnhmcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A deliberately small loopback-only transport. Game state access is never done
 * on its worker threads; currently unsupported requests say so rather than
 * exposing unsynchronised client state.
 */
public final class BridgeServer implements HttpHandler {

    public static final int PORT = 8765;
    private static final int MAX_BODY = 262144;
    private final Gson gson = new Gson();
    private HttpServer server;
    private ExecutorService executor;
    private final QueryRouter router;

    public BridgeServer(ClientDispatcher dispatcher) {
        router = new QueryRouter(dispatcher);
    }

    public synchronized boolean start() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 16);
            server.createContext("/v1/query", this);
            executor = Executors.newFixedThreadPool(1);
            server.setExecutor(executor);
            server.start();
            return true;
        } catch (IOException e) {
            stop();
            return false;
        }
    }

    public void handle(HttpExchange x) throws IOException {
        if (!"POST".equals(x.getRequestMethod())) {
            x.getResponseHeaders()
                .set("Allow", "POST");
            x.sendResponseHeaders(405, -1);
            return;
        }
        String contentType = x.getRequestHeaders()
            .getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase()
            .startsWith("application/json")) {
            x.sendResponseHeaders(415, -1);
            return;
        }
        byte[] body = readBounded(x.getRequestBody());
        if (body == null) {
            x.sendResponseHeaders(413, -1);
            return;
        }
        JsonObject request;
        try {
            request = gson.fromJson(new String(body, "UTF-8"), JsonObject.class);
        } catch (Exception e) {
            send(x, 400, error("invalid JSON"));
            return;
        }
        if (request == null || !request.has("method")
            || !request.get("method")
                .isJsonPrimitive()) {
            send(x, 400, error("method required"));
            return;
        }
        String method = request.get("method")
            .getAsString();
        if (!method.matches("[a-z0-9_.]{1,80}")) {
            send(x, 400, error("invalid method"));
            return;
        }
        try {
            send(
                x,
                200,
                router.query(
                    method,
                    request.has("params") && request.get("params")
                        .isJsonObject() ? request.getAsJsonObject("params") : new JsonObject()));
        } catch (Exception e) {
            Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            int status = cause instanceof IllegalArgumentException ? 400
                : cause instanceof TimeoutException ? 504 : 500;
            String message = cause.getMessage();
            send(
                x,
                status,
                error(
                    message == null ? cause.getClass()
                        .getSimpleName() : message));
        }
    }

    private JsonObject error(String m) {
        JsonObject o = new JsonObject();
        o.addProperty("error", m);
        return o;
    }

    private void send(HttpExchange x, int code, JsonObject value) throws IOException {
        byte[] b = gson.toJson(value)
            .getBytes(Charset.forName("UTF-8"));
        x.getResponseHeaders()
            .set("Content-Type", "application/json");
        x.sendResponseHeaders(code, b.length);
        try {
            x.getResponseBody()
                .write(b);
        } finally {
            x.close();
        }
    }

    private static byte[] readBounded(InputStream in) throws IOException {
        byte[] b = new byte[MAX_BODY + 1];
        int n = 0, r;
        while ((r = in.read(b, n, b.length - n)) > 0) {
            n += r;
            if (n > MAX_BODY) return null;
        }
        byte[] out = new byte[n];
        System.arraycopy(b, 0, out, 0, n);
        return out;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
