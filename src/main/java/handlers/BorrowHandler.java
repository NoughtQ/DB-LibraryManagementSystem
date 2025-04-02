package handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dbSystem.LibraryManagementSystem;
import dbSystem.LibraryManagementSystemImpl;
import queries.ApiResult;
import queries.BorrowHistories;
import utils.DatabaseConnector;

import java.io.*;

public class BorrowHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public BorrowHandler(DatabaseConnector connector) {
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
        // 获取请求参数中的 cardID
        String query = exchange.getRequestURI().getQuery();
        int cardId = -1;
        if (query != null && query.startsWith("cardId=")) {
            cardId = Integer.parseInt(query.substring(7));
        }

        // 调用 library 接口查询借阅历史
        ApiResult result = library.showBorrowHistory(cardId);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        OutputStream outputStream = exchange.getResponseBody();

        String response;
        int statusCode;

        if (result.ok) {
            // 成功获取借阅历史
            ObjectMapper objectMapper = new ObjectMapper();
            BorrowHistories borrowHistories = (BorrowHistories) result.payload;
            response = objectMapper.writeValueAsString(borrowHistories.getItems());
            statusCode = 200;
        } else {
            response = "";
            statusCode = 500;
        }

        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        outputStream.write(responseBytes);
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