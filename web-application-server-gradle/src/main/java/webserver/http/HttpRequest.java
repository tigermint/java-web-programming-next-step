package webserver.http;


import util.HttpRequestUtils;
import util.HttpRequestUtils.Pair;
import util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {
    private final StartLine startLine;
    private final Map<String, String> headers;
    private final String body;
    private final Map<String, String> parameters;
    private final Map<String, String> cookies;


    public HttpRequest(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }

        this.startLine = new StartLine(lines.get(0));
        this.headers = extractHeaders(lines);
        this.cookies = extractCookies(headers.get("Cookie"));
        this.body = extractBody(br, headers.get("Content-Length"));
        this.parameters = extractParameters(startLine.getQueryString(), body);

    }

    private Map<String, String> extractHeaders(List<String> lines) {
        Map<String, String> tmpHeaders = new HashMap<>();

        for (int i = 1; i < lines.size() - 1; i++) {
            if(lines.get(i).isEmpty()) {
                break;
            }
            String line = lines.get(i);
            Pair entry = HttpRequestUtils.parseHeader(line);
            tmpHeaders.put(entry.getKey(), entry.getValue());
        }

        return tmpHeaders;
    }

    private static Map<String, String> extractParameters(Map<String, String> requestLineParams, String requestBody) {
        if (!requestLineParams.isEmpty()) {
            return requestLineParams;
        }
        return extractParametersFromBody(requestBody);
    }

    private static Map<String, String> extractParametersFromBody(String requestBody) {
        if (!"".equals(requestBody)) {
            return HttpRequestUtils.parseQueryString(requestBody);
        }
        return Collections.emptyMap();
    }

    private static String extractBody(BufferedReader br, String contentLength) throws IOException {
        if (contentLength != null) {
            return IOUtils.readData(br, Integer.parseInt(contentLength));
        }
        return null;
    }

    private static Map<String, String> extractCookies(String cookies) {
        if (cookies == null) {
            return Collections.emptyMap();
        }
        return HttpRequestUtils.parseCookies(cookies);
    }

    @Override
    public String toString() {
        return "Http:" + startLine.getMethod()
                + " " + startLine.getPath()
                + ", Parameters: " + (parameters == null ? "-" : parameters.toString())
                + "\n Headers:" + (headers == null ? "-" : headers.toString())
                + "\n Cookie:" + (cookies == null ? "-" : cookies.toString())
                + "\n Body:" + body;
    }

    public HttpMethod getMethod() {
        return startLine.getMethod();
    }

    public String getRequestPath() {
        return startLine.getPath();
    }

    public String getHeader(String connection) {
        return headers.get(connection);
    }

    public String getParameter(String userId) {
        return parameters.get(userId);
    }
}
