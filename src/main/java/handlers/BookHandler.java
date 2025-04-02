package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dbSystem.LibraryManagementSystem;
import dbSystem.LibraryManagementSystemImpl;
import utils.DatabaseConnector;

import java.io.*;

public class BookHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public BookHandler(DatabaseConnector connector) {
        this.library = new LibraryManagementSystemImpl(connector);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            handlePostRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            // handleOptionsRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        OutputStream outputStream = exchange.getResponseBody();
        String response = "[{\"cardId\": 1, \"name\": \"John Doe\", \"department\": \"Computer Science\", \"type\": \"Student\"}," +
                "{\"cardId\": 2, \"name\": \"Jane Smith\", \"department\": \"Electrical Engineering\", \"type\": \"Faculty\"}]";

        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Card created successfully".getBytes());
        outputStream.close();
    }
}