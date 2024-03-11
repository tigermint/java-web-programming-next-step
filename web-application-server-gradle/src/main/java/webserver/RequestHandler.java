package webserver;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            Map<String, String> headerMap = new HashMap<>();
            boolean logined = false;

            while (!line.isEmpty()) {
//                log.debug("header: {}", line);
                line = br.readLine();
                if(line.contains("Content-Length")) {
                    headerMap.put("Content-Length", line.split(":")[1].trim());
                }
                if (line.contains("Cookie")) {
                    logined = isLogin(line);
                }
            }

            //URL 분기 처리
            if (url.startsWith("/user/create")) {
                int contentLength = Integer.parseInt(headerMap.get("Content-Length"));
                String requestBody = IOUtils.readData(br, contentLength);
//                log.debug("Request Body: {}", requestBody);
                Map<String, String> paramMap = HttpRequestUtils.parseQueryString(requestBody);
                User user = new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email"));

                //User 저장
                DataBase.addUser(user);
//                log.debug("User : {}", user);
//                log.debug("DataBase : {}", DataBase.findAll());

                url = "/index.html";
                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos, url);

            } else if (url.equals("/user/login")) {
                int contentLength = Integer.parseInt(headerMap.get("Content-Length"));
                String requestBody = IOUtils.readData(br, contentLength);
//                log.debug("Request Body: {}", requestBody);
                Map<String, String> paramMap = HttpRequestUtils.parseQueryString(requestBody);

                //DataBase에서 찾기
                Optional<User> optionalUser = Optional.ofNullable(DataBase.findUserById(paramMap.get("userId")));
                String redirectUrl = "";
                String cookie = "";
                if (optionalUser.isPresent() && optionalUser.get().getPassword().equals(paramMap.get("password"))) {
                    //쿠키 저장
                    redirectUrl = "/index.html";
                    cookie = "logined=true";
                } else {
                    redirectUrl = "/user/login_failed.html";
                    cookie = "logined=false";
                }
                DataOutputStream dos = new DataOutputStream(out);
                response302HeaderWithCookie(dos, redirectUrl, cookie);

            } else if (url.equals("/user/list")) {
                if (!logined) {
                    DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos, "/user/login.html");
                    return;
                }
                Collection<User> users = DataBase.findAll();
                StringBuilder sb = new StringBuilder();
                sb.append("<table border='1'>");
                for (User user : users) {
                    sb.append("<tr>");
                    sb.append("<td>").append(user.getUserId()).append("</td>");
                    sb.append("<td>").append(user.getName()).append("</td>");
                    sb.append("<td>").append(user.getEmail()).append("</td>");
                    sb.append("</tr>");
                }
                sb.append("</table>");
                byte[] body = sb.toString().getBytes();
                DataOutputStream dos = new DataOutputStream(out);
                response200Header(dos, body.length);
                responseBody(dos, body);
            } else if (url.endsWith(".css")) {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./web-application-server-gradle/webapp" + url).toPath());
                response200CssHeader(dos, body.length);
                responseBody(dos, body);
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

    private boolean isLogin(String line) {
        String[] split = line.split(":");
        Map<String, String> cookieMap = HttpRequestUtils.parseCookies(split[1].trim());
        return Boolean.parseBoolean(cookieMap.get("logined"));
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

    private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
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

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
