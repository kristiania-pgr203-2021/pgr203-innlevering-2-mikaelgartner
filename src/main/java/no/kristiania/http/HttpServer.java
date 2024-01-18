package no.kristiania.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpServer {

    private final ServerSocket serverSocket;
    private Path rootDirectory;
    private List<String> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();

    public HttpServer(int serverPort) throws IOException {
        serverSocket = new ServerSocket(serverPort);

        new Thread(this::handleClients).start();
    }

    private void handleClients() {
        try {
            while (true) {
            handleClient();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient() throws IOException {
        Socket clientSocket = serverSocket.accept();

        HttpMessage httpMessage = new HttpMessage(clientSocket);
        String[] requestLine = httpMessage.startLine.split(" ");
        String requestTarget = requestLine[1];

        int questionPos = requestTarget.indexOf('?');
        String fileTarget;
        String query = null;
        if (questionPos != -1) {
            fileTarget = requestTarget.substring(0, questionPos);
            query = requestTarget.substring(questionPos+1);
        } else {
            fileTarget = requestTarget;
        }


        if (fileTarget.equals("/api/products")) {
            String yourProductName = "<h3>You added the following items:</h3>";

            if (query != null) {
                Map<String, String> queryMap = parseRequestParameters(query);
                yourProductName = queryMap.get("productName");
            }

            String responseText = yourProductName;
            for (Product product : products) {
                responseText += "<h4><li>Product: " + product.getProductName() + "</li></h4>";
            }

            writeOkResponse(responseText, "text/html", clientSocket);

            //In the else if below
            //It looks like products are added to the "products" ArrayList when pressing "Submit" on "newProduct" page,
            //using the dev tool it has payload with the category that is chosen, and the productName that is typed in
            //how do we display the newly added "product" from the ListArray to the listProducts.html page?
        } else if (fileTarget.equals("/api/newProduct")) {
            Map<String, String> queryMap = parseRequestParameters(httpMessage.messageBody);
            Product product = new Product();
            product.setProductName(queryMap.get("productName"));
            products.add(product);
            writeOkResponse("<h4>Product was successfully added to the list.</h4>", "text/html", clientSocket);

        } else if (fileTarget.equals("/api/categoryOptions")) {
            String responseText = "";

            int value = 1;
            for (String category : categories) {
                responseText += "<option value=" + (value++) + ">" + category + "</option>";
            }

            writeOkResponse(responseText, "text/html", clientSocket);
        } else {
            if (rootDirectory != null && Files.exists(rootDirectory.resolve(fileTarget.substring(1)))) {
                String responseText = Files.readString(rootDirectory.resolve(fileTarget.substring(1)));

                String contentType = "text/plain";
                if (requestTarget.endsWith(".html")) {
                    contentType = "text/html";
                }
                writeOkResponse(responseText, contentType, clientSocket);
                return;
            }

            String responseText = "File not found: " + requestTarget;

            String response = "HTTP/1.1 404 Not found\r\n" +
                    "Content-Length: " + responseText.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseText;
            clientSocket.getOutputStream().write(response.getBytes());
        }
    }

    private static Map<String, String> parseRequestParameters(String query) {
        Map<String, String> queryMap = new HashMap<>();
        for (String queryParameter : query.split("&")) {
            int equalsPos = queryParameter.indexOf('=');
            String parameterName = queryParameter.substring(0, equalsPos);
            String parameterValue = queryParameter.substring(equalsPos+1);
            queryMap.put(parameterName, parameterValue);
        }
        return queryMap;
    }

    private static void writeOkResponse(String responseText, String contentType, Socket clientSocket) throws IOException {
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + responseText.length() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                responseText;
        clientSocket.getOutputStream().write(response.getBytes());
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = new HttpServer(1962);
        httpServer.setCategories(List.of("Appliances", "Computer Equipment", "Gaming", "TV & Home Theater"));
        httpServer.setRoot(Paths.get("."));

    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void setRoot(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<Product> getProducts() {
        return products;
    }
}

