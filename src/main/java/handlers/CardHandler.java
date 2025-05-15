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
import java.util.Map;

public class CardHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public CardHandler(DatabaseConnector connector) {
        this.library = new LibraryManagementSystemImpl(connector);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置CORS头
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        // 处理预检请求
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            handlePostRequest(exchange);
        } else if (requestMethod.equals("PUT")) {
            handlePutRequest(exchange);
        } else if (requestMethod.equals("DELETE")) {
            handleDeleteRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    // 借书证查询（显示）
    private void handleGetRequest(HttpExchange exchange) throws IOException {
        ApiResult result = library.showCards();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        OutputStream outputStream = exchange.getResponseBody();

        String response;
        int statusCode;

        if (result.ok) {
            ObjectMapper objectMapper = new ObjectMapper();
            CardList cardList = (CardList) result.payload;
            List<Card> cards = cardList.getCards();
            response = objectMapper.writeValueAsString(cards);
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

    // 借书证注册
    private void handlePostRequest(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        String jsonStr = requestBodyBuilder.toString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(jsonStr, Map.class);
        Card card = new Card();
        if (jsonMap.containsKey("name")) {
            card.setName((String) jsonMap.get("name"));
        }
        if (jsonMap.containsKey("department")) {
            card.setDepartment((String) jsonMap.get("department"));
        }
        if (jsonMap.containsKey("type")) {
            String typeStr = (String) jsonMap.get("type");
            if ("Student".equals(typeStr)) {
                card.setType(Card.CardType.Student);
            } else if ("Teacher".equals(typeStr)) {
                card.setType(Card.CardType.Teacher);
            }
        }

        ApiResult result = library.registerCard(card);
        System.out.println(result.message);
        OutputStream outputStream = exchange.getResponseBody();
        String message;
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        if (result.ok) {
            message = "借书证注册成功";
            exchange.sendResponseHeaders(200, message.getBytes().length);
        } else {
            message = "借书证注册失败（很可能是因为借书证已经存在）";
            exchange.sendResponseHeaders(400, message.getBytes().length);
        }
        outputStream.write(message.getBytes());
        outputStream.close();
    }

    // 借书证更新
    private void handlePutRequest(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        String jsonStr = requestBodyBuilder.toString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(jsonStr, Map.class);

        Card card = new Card();

        if (jsonMap.containsKey("cardId")) {
            card.setCardId(((Number) jsonMap.get("cardId")).intValue());
        } else {
            String errorMessage = "缺少借书证ID";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(400, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
            return;
        }

        if (jsonMap.containsKey("name")) {
            card.setName((String) jsonMap.get("name"));
        }
        if (jsonMap.containsKey("department")) {
            card.setDepartment((String) jsonMap.get("department"));
        }
        if (jsonMap.containsKey("type")) {
            String typeStr = (String) jsonMap.get("type");
            System.out.println(typeStr);
            if ("Student".equals(typeStr)) {
                card.setType(Card.CardType.Student);
            } else if ("Teacher".equals(typeStr)) {
                card.setType(Card.CardType.Teacher);
            }
        }

        System.out.println(card);

        ApiResult result = library.modifyCardInfo(card);

        String message;
        exchange.getResponseHeaders().set("Content-Type", "text/plain");

        if (result.ok) {
            message = "借书证修改成功";
            exchange.sendResponseHeaders(200, message.getBytes().length);
        } else {
            message = result.message != null ? result.message : "借书证修改失败";
            exchange.sendResponseHeaders(400, message.getBytes().length);
        }

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(message.getBytes());
        outputStream.close();
    }

    // 删除借书证
    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int cardId = -1;

        if (query != null && query.startsWith("cardId=")) {
            try {
                cardId = Integer.parseInt(query.substring(7));
            } catch (NumberFormatException e) {
                String errorMessage = "借书证ID格式不正确";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                byte[] responseBytes = errorMessage.getBytes();
                exchange.sendResponseHeaders(400, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
                return;
            }
        } else {
            String errorMessage = "缺少借书证ID参数";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(400, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
            return;
        }

        ApiResult result = library.removeCard(cardId);

        String message;
        exchange.getResponseHeaders().set("Content-Type", "text/plain");

        if (result.ok) {
            message = "借书证删除成功";
            exchange.sendResponseHeaders(200, message.getBytes().length);
        } else {
            message = result.message != null ? result.message : "借书证删除失败";
            exchange.sendResponseHeaders(400, message.getBytes().length);
        }

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(message.getBytes());
        outputStream.close();
    }
}