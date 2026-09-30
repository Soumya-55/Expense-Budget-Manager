package com.expense;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Starts the web server: serves /web files and a small REST API under /api. */
public class Main {

    public static void main(String[] args) throws Exception {
        try {
            Db.init();
        } catch (Exception e) {
            System.err.println("\nCOULD NOT CONNECT TO MYSQL: " + e.getMessage());
            System.err.println("Check: MySQL is running, config.properties user/password is right, and the mysql-connector-j jar is in lib/\n");
            return;
        }
        int port = Integer.parseInt(Config.get("server.port", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", Main::api);
        server.createContext("/", Main::staticFile);
        server.start();
        System.out.println("Server started -> open  http://localhost:" + port);
    }

    // ---------- API ----------
    private static void api(HttpExchange ex) throws IOException {
        int code = 200;
        String body;
        try {
            Map<String, String> q = parse(ex.getRequestURI().getRawQuery());
            Map<String, String> f = parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            body = Json.of(route(ex.getRequestMethod(), ex.getRequestURI().getPath(), q, f));
        } catch (IllegalArgumentException e) {
            code = 400; body = Json.of(Map.of("error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            code = 500; body = Json.of(Map.of("error", "Server error: " + e.getMessage()));
        }
        send(ex, code, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static Object route(String m, String p, Map<String, String> q, Map<String, String> f) throws Exception {
        Map<String, Object> ok = Map.of("ok", true);
        switch (p) {
            case "/api/transactions":
                if (m.equals("GET")) return Dao.transactions(q.get("month"), q.get("type"));
                if (m.equals("POST")) {
                    Dao.addTransaction(type(f), amount(f), req(f, "category"), f.getOrDefault("description", ""), date(f));
                    return ok;
                }
                if (m.equals("PUT")) {
                    Dao.updateTransaction(id(q), type(f), amount(f), req(f, "category"), f.getOrDefault("description", ""), date(f));
                    return ok;
                }
                if (m.equals("DELETE")) { Dao.deleteTransaction(id(q)); return ok; }
                break;
            case "/api/budgets":
                if (m.equals("GET")) return Dao.budgets(req(q, "month"));
                if (m.equals("POST")) { Dao.saveBudget(req(f, "category"), req(f, "month"), amount(f)); return ok; }
                if (m.equals("DELETE")) { Dao.deleteBudget(id(q)); return ok; }
                break;
            case "/api/summary":
                if (m.equals("GET")) return Dao.summary(req(q, "month"));
                break;
        }
        throw new IllegalArgumentException("Unknown endpoint: " + m + " " + p);
    }

    // ---------- Validation helpers ----------
    private static String req(Map<String, String> f, String k) {
        String v = f.get(k);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(k + " is required");
        return v.trim();
    }
    private static String type(Map<String, String> f) {
        String t = req(f, "type").toUpperCase();
        if (!t.equals("INCOME") && !t.equals("EXPENSE")) throw new IllegalArgumentException("type must be INCOME or EXPENSE");
        return t;
    }
    private static BigDecimal amount(Map<String, String> f) {
        try {
            BigDecimal b = new BigDecimal(req(f, "amount"));
            if (b.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
            return b;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid amount"); }
    }
    private static LocalDate date(Map<String, String> f) {
        try { return LocalDate.parse(req(f, "date")); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Invalid date"); }
    }
    private static int id(Map<String, String> q) {
        try { return Integer.parseInt(req(q, "id")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid id"); }
    }
    private static Map<String, String> parse(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                  URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    // ---------- Static files (web folder) ----------
    private static void staticFile(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        Path root = Paths.get("web").toAbsolutePath().normalize();
        Path file = root.resolve(p.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain", "Not found".getBytes());
            return;
        }
        String n = file.getFileName().toString();
        String type = n.endsWith(".html") ? "text/html; charset=utf-8"
                    : n.endsWith(".css") ? "text/css; charset=utf-8"
                    : n.endsWith(".js") ? "application/javascript; charset=utf-8" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(file));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
