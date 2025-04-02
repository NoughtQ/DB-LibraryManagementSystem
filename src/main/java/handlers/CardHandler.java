package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.Card;
import queries.ApiResult;
import queries.CardList;
import utils.DatabaseConnector;
import dbSystem.LibraryManagementSystem;
import dbSystem.LibraryManagementSystemImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.List;

public class CardHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public CardHandler(DatabaseConnector connector) {
        this.library = new LibraryManagementSystemImpl(connector);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String requestMethod = exchange.getRequestMethod();
        // System.out.println(requestMethod);
        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            // System.out.println(exchange.getRequestURI());
            handlePostRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            // handleOptionsRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {        // 使用LibraryManagementSystem接口获取所有借书证
        ApiResult result = library.showCards();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        OutputStream outputStream = exchange.getResponseBody();

        String response;
        int statusCode;

        if (result.ok) {
            // 成功获取借书证列表
            ObjectMapper objectMapper = new ObjectMapper();
//            StringBuilder tmp_response = new StringBuilder();
//            tmp_response.append("[");
            CardList cardList = (CardList) result.payload;
            List<Card> cards = cardList.getCards();
//            for (Card card : cards) {
//                tmp_response.append(objectMapper.writeValueAsString(card));
//                tmp_response.append(", ");
//            }
//            tmp_response.append("]");
//            response = tmp_response.toString();
            response = objectMapper.writeValueAsString(cards);

            statusCode = 200;
        } else {
            // response = "{\"error\":\"" + result.message + "\"}";
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

        ObjectMapper objectMapper = new ObjectMapper();
        // System.out.println(requestBodyBuilder.toString());
        Card card = objectMapper.readValue(requestBodyBuilder.toString(), Card.class);

        ApiResult result = library.registerCard(card);

        // OutputStream outputStream = exchange.getResponseBody();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        if (result.ok) {
            // outputStream.write("Card registered successfully".getBytes());
            exchange.sendResponseHeaders(200, 0);
        } else {
            // outputStream.write("Failed to register card".getBytes());
            exchange.sendResponseHeaders(500, 0);
        }

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.close();
    }
}