package webserver.http;

import util.HttpRequestUtils;

import java.util.Collections;
import java.util.Map;

public class StartLine {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> queryString;

    public StartLine(String startLine) {
        String[] tokens = startLine.split(" ");
        this.method = HttpMethod.valueOf(tokens[0]);
        String url = tokens[1];

        int index = url.indexOf("?");
        if (index != -1) {
            path = url.substring(0, index);
            queryString = HttpRequestUtils.parseQueryString(url.substring(index + 1));
        } else {
            path = url;
            queryString = Collections.emptyMap();
        }
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryString() {
        return queryString;
    }
}
