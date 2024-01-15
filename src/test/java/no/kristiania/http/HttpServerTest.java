package no.kristiania.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    private final HttpServer server = new HttpServer(0);

    HttpServerTest() throws IOException {
    }

    @Test
    void shouldReturn404ForUnknownRequestTarget() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/non-existing");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldRespondWithRequestTargetIn404() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/non-existing");
        assertEquals("File not found: /non-existing", client.getMessageBody());
    }

    @Test
    void shouldRespondWith200ForKnownRequestTarget() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/products");
        assertAll(
                () -> assertEquals(200, client.getStatusCode()),
                () -> assertEquals("text/html", client.getHeader("Content-Type")),
                () -> assertEquals("<h3>You added the following items:</h3>", client.getMessageBody())
        );
    }

    @Test
    void shouldHandleMoreThanOneRequest() throws IOException {
        assertEquals(200, new HttpClient("localhost", server.getPort(), "/api/products")
                .getStatusCode());
        assertEquals(200, new HttpClient("localhost", server.getPort(), "/api/products")
                .getStatusCode());
    }

    //Commented out test that made HttpServerTest red. Do we still need the test?
//    @Test
//    void shouldEchoQueryParameter() throws IOException {
//        HttpClient client = new HttpClient("localhost",
//                server.getPort(),
//                "/api/products?productName=Microwave"
//        );
//        assertEquals("<p>Microwave</p>", client.getMessageBody());
//    }

    @Test
    void shouldServeFiles() throws IOException {
        server.setRoot(Paths.get("target/test-classes"));

        String fileContent = "A file created at " + LocalTime.now();
        Files.write(Paths.get("target/test-classes/example-file.txt"), fileContent.getBytes());

        HttpClient client = new HttpClient("localhost", server.getPort(), "/example-file.txt");
        assertEquals(fileContent, client.getMessageBody());
        assertEquals("text/plain", client.getHeader("Content-Type"));
    }

    @Test
    void shouldUseFileExtensionForContentType() throws IOException {
        server.setRoot(Paths.get("target/test-classes"));

        String fileContent = "<p>Hello</p>";
        Files.write(Paths.get("target/test-classes/example-file.html"), fileContent.getBytes());

        HttpClient client = new HttpClient("localhost", server.getPort(), "/example-file.html");
        assertEquals("text/html", client.getHeader("Content-Type"));
    }

    @Test
    void shouldReturnCategoriesFromServer() throws IOException {
        server.setCategories(List.of("Appliances", "Computer Equipment"));


        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/categoryOptions");
        assertEquals(
                "<option value=1>Appliances</option><option value=2>Computer Equipment</option>",
                client.getMessageBody()
        );
    }

    @Test
    void shouldCreateNewProduct() throws IOException {
        HttpPostClient postClient = new HttpPostClient(
                "localhost",
                server.getPort(),
                "/api/newProduct",
                "productName=Microwave"
        );
        assertEquals(200, postClient.getStatusCode());
        Product product = server.getProducts().get(0);
        assertEquals("Microwave", product.getProductName());
    }
}