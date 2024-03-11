package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    @Override
    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String firstLine = br.readLine();
            if (firstLine == null) {
                return;
            }

            String url = HttpRequestUtils.getUrl(firstLine);
            if (url.startsWith("/user/create")) {
                int questionMarkIndex = url.indexOf("?");
                if (questionMarkIndex != -1) {
                    String queryString = url.substring(questionMarkIndex + 1);
                    log.debug("queryString: {}", queryString);

                    Map<String, String> createUserMap = HttpRequestUtils.parseQueryString(queryString);
                    User user = new User(createUserMap.get("userId"), createUserMap.get("password"),
                            createUserMap.get("name"), createUserMap.get("email"));

                    log.debug("User : {}", user);
                    url= "/index.html";
                } else {
                    log.debug("no query string");
                }
            }

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./web-application-server-gradle/webapp" + url).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
