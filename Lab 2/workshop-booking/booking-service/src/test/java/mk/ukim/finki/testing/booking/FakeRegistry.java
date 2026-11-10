package mk.ukim.finki.testing.booking;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class FakeRegistry {

    private final HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int status;
    private volatile String body;
    private volatile long delayMs;
    private volatile String lastPath;

    FakeRegistry() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/members/", exchange -> {
            calls.incrementAndGet();
            lastPath = exchange.getRequestURI().getPath();
            String memberId = lastPath.substring("/members/".length());
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.replace("{id}", memberId).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        reset();
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void reset() {
        status = 200;
        body = "{\"memberId\":\"{id}\",\"eligible\":true}";
        delayMs = 0;
        lastPath = null;
        calls.set(0);
    }

    void respond(int status, String body) {
        this.status = status;
        this.body = body;
    }

    void delay(long millis) {
        this.delayMs = millis;
    }

    int calls() {
        return calls.get();
    }

    String lastPath() {
        return lastPath;
    }

    void stop() {
        server.stop(0);
    }
}