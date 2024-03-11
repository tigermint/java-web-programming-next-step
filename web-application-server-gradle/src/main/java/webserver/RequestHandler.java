package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    @Override
    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = br.readLine();
            if (line == null) {
                return;
            }

            String url = HttpRequestUtils.getUrl(line);
            HashMap<String, String> headers = new HashMap<>();

            while (!line.isEmpty()) {
                log.debug("header: {}", line);
                line = br.readLine();
                String[] headerTokens = line.split(": ");
                if (headerTokens.length == 2) {
                    headers.put(headerTokens[0], headerTokens[1]);
                }
            }

            //URL 분기 처리
            if (url.startsWith("/user/create")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                String requestBody = IOUtils.readData(br, contentLength);
                log.debug("Request Body: {}", requestBody);
                Map<String, String> paramMap = HttpRequestUtils.parseQueryString(requestBody);
                User user = new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email"));

                //User 저장
                DataBase.addUser(user);
                log.debug("User : {}", user);
                log.debug("DataBase : {}", DataBase.findAll());

                url = "/index.html";
                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos, url);

            } else if (url.equals("/user/login")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                String requestBody = IOUtils.readData(br, contentLength);
                log.debug("Request Body: {}", requestBody);
                Map<String, String> paramMap = HttpRequestUtils.parseQueryString(requestBody);

                //DataBase에서 찾기
                Optional<User> optionalUser = Optional.ofNullable(DataBase.findUserById(paramMap.get("userId")));
                String redirectUrl = "";
                String cookie = "";
                if (optionalUser.isPresent() && optionalUser.get().getPassword().equals(paramMap.get("password"))) {
                    //쿠키 저장
                    redirectUrl = "/index.html";
                    cookie = "logined=true";
                }else{
                    redirectUrl = "/user/login_failed.html";
                    cookie = "logined=false";
                }
                DataOutputStream dos = new DataOutputStream(out);
                response302HeaderWithCookie(dos, redirectUrl, cookie);

            } else {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./web-application-server-gradle/webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String location, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
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

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
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
