package webserver.http;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestTest {
    private static final String testDirectory = "./src/test/resources/";

    @Test
    void request_GET() throws Exception {
        InputStream in = Files.newInputStream(Paths.get(testDirectory + "Http_GET.txt"));
        HttpRequest request = new HttpRequest(in);

        assertAll(() -> {
            assertTrue(request.getMethod().isGet());
            assertEquals("/user/create", request.getRequestPath());
            assertEquals("keep-alive", request.getHeader("Connection"));
            assertEquals("myid", request.getParameter("userId"));
        });
    }

    @Test
    void request_POST() throws Exception {
        InputStream in = Files.newInputStream(Paths.get(testDirectory + "Http_POST.txt"));
        HttpRequest request = new HttpRequest(in);

        assertAll(() -> {
            assertTrue(request.getMethod().isPost());
            assertEquals("/user/create", request.getRequestPath());
            assertEquals("keep-alive", request.getHeader("Connection"));
            assertEquals("myii", request.getParameter("userId"));
        });
    }

}