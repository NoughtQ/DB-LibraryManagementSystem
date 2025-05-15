package handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dbSystem.LibraryManagementSystem;
import dbSystem.LibraryManagementSystemImpl;
import entities.Book;
import entities.Borrow;
import queries.ApiResult;
import queries.BorrowHistories;
import utils.DatabaseConnector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BorrowHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public BorrowHandler(DatabaseConnector connector) {
        this.library = new LibraryManagementSystemImpl(connector);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置CORS头
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        String requestMethod = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 处理预检请求
        if (requestMethod.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // 显示全部借阅记录
        if (requestMethod.equals("GET") && path.endsWith("/borrow/all")) {
            handleGetAllRequest(exchange);
            return;
        }

        if (requestMethod.equals("GET")) {
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            handlePostRequest(exchange);
        } else if (requestMethod.equals("PUT")) {
            handlePutRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    // 根据借书证 ID 查询特定借阅记录
    private void handleGetRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int cardId = -1;
        if (query != null && query.startsWith("cardId=")) {
            cardId = Integer.parseInt(query.substring(7));
        }

        ApiResult result = library.showBorrowHistory(cardId);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        OutputStream outputStream = exchange.getResponseBody();

        String response;
        int statusCode;

        if (result.ok) {
            ObjectMapper objectMapper = new ObjectMapper();
            BorrowHistories borrowHistories = (BorrowHistories) result.payload;
            response = objectMapper.writeValueAsString(borrowHistories.getItems());
            statusCode = 200;
        } else {
            response = "[]";
            statusCode = 200;
        }

        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        outputStream.write(responseBytes);
        outputStream.close();
    }

    // 显示所有借阅记录
    private void handleGetAllRequest(HttpExchange exchange) throws IOException {
        try {
            ApiResult cardsResult = library.showCards();
            
            List<Map<String, Object>> allBorrows = new ArrayList<>();
            
            if (cardsResult.ok && cardsResult.payload != null) {
                Object cardsObj = cardsResult.payload;
                List<Integer> cardIds = new ArrayList<>();
                
                try {
                    java.lang.reflect.Method getCardsMethod = cardsObj.getClass().getMethod("getCards");
                    List<?> cards = (List<?>) getCardsMethod.invoke(cardsObj);
                    
                    for (Object cardObj : cards) {
                        java.lang.reflect.Method getCardIdMethod = cardObj.getClass().getMethod("getCardId");
                        int cardId = (int) getCardIdMethod.invoke(cardObj);
                        cardIds.add(cardId);
                    }
                } catch (Exception e) {
                    System.err.println("提取卡片ID失败: " + e.getMessage());
                    e.printStackTrace();
                    
                    String cardsStr = cardsObj.toString();
                    if (cardsStr.contains("cardId")) {
                        String[] parts = cardsStr.split("cardId=");
                        for (int i = 1; i < parts.length; i++) {
                            try {
                                int endIndex = parts[i].indexOf(",");
                                if (endIndex == -1) endIndex = parts[i].indexOf("}");
                                if (endIndex != -1) {
                                    int cardId = Integer.parseInt(parts[i].substring(0, endIndex).trim());
                                    cardIds.add(cardId);
                                }
                            } catch (NumberFormatException nfe) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
                
                for (int cardId : cardIds) {
                    ApiResult borrowResult = library.showBorrowHistory(cardId);
                    
                    if (borrowResult.ok && borrowResult.payload != null) {
                        BorrowHistories borrowHistories = (BorrowHistories) borrowResult.payload;
                        List<BorrowHistories.Item> items = borrowHistories.getItems();
                        for (BorrowHistories.Item item : items) {
                            Map<String, Object> borrowMap = new ObjectMapper().convertValue(item, Map.class);
                            allBorrows.add(borrowMap);
                        }
                    }
                }
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            String response = objectMapper.writeValueAsString(allBorrows);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
            
        } catch (Exception e) {
            String errorMessage = "获取所有借阅记录失败: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, errorMessage.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(errorMessage.getBytes());
            outputStream.close();
        }
    }

    // 借书
    private void handlePostRequest(HttpExchange exchange) throws IOException {
        try {
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

            Borrow borrow = new Borrow();

            if (jsonMap.containsKey("cardId")) {
                borrow.setCardId(((Number) jsonMap.get("cardId")).intValue());
            }
            if (jsonMap.containsKey("bookId")) {
                borrow.setBookId(((Number) jsonMap.get("bookId")).intValue());
            }
            if (jsonMap.containsKey("borrowTime")) {
                borrow.setBorrowTime(((Number) jsonMap.get("borrowTime")).longValue());
            } else {
                borrow.setBorrowTime(System.currentTimeMillis());
            }

            ApiResult result = library.borrowBook(borrow);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "借书成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "借书失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理借书请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理借书请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }

    // 还书
    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
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

            Borrow borrow = new Borrow();

            if (jsonMap.containsKey("cardId")) {
                borrow.setCardId(((Number) jsonMap.get("cardId")).intValue());
            }
            if (jsonMap.containsKey("bookId")) {
                borrow.setBookId(((Number) jsonMap.get("bookId")).intValue());
            }
            if (jsonMap.containsKey("borrowTime")) {
                borrow.setBorrowTime(((Number) jsonMap.get("borrowTime")).longValue());
            }
            if (jsonMap.containsKey("returnTime")) {
                borrow.setReturnTime(((Number) jsonMap.get("returnTime")).longValue());
            } else {
                borrow.setReturnTime(System.currentTimeMillis());
            }

            ApiResult result = library.returnBook(borrow);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "还书成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "还书失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理还书请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理还书请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }
}