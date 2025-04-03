package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dbSystem.LibraryManagementSystem;
import dbSystem.LibraryManagementSystemImpl;
import entities.Book;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import utils.DatabaseConnector;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.List;
import java.util.Map;

public class BookHandler implements HttpHandler {
    private final LibraryManagementSystem library;

    public BookHandler(DatabaseConnector connector) {
        this.library = new LibraryManagementSystemImpl(connector);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 设置CORS头
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");

        // 处理预检请求
        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // 处理批量导入图书的请求
        if (method.equals("POST") && path.endsWith("/batch")) {
            handleBatchImportRequest(exchange);
            return;
        }

        // 处理其他请求
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
        } else if (requestMethod.equals("PATCH") && path.endsWith("/stock")) {
            handlePatchStockRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    // 图书查询相关（高级查询）
    private void handleGetRequest(HttpExchange exchange) throws IOException {
        try {
            String queryString = exchange.getRequestURI().getQuery();
            BookQueryConditions conditions = new BookQueryConditions();
            
            if (queryString != null && !queryString.isEmpty()) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                        
                        switch (key) {
                            case "category":
                                conditions.setCategory(value);
                                break;
                            case "title":
                                conditions.setTitle(value);
                                break;
                            case "press":
                                conditions.setPress(value);
                                break;
                            case "minPublishYear":
                                conditions.setMinPublishYear(Integer.parseInt(value));
                                break;
                            case "maxPublishYear":
                                conditions.setMaxPublishYear(Integer.parseInt(value));
                                break;
                            case "author":
                                conditions.setAuthor(value);
                                break;
                            case "minPrice":
                                conditions.setMinPrice(Double.parseDouble(value));
                                break;
                            case "maxPrice":
                                conditions.setMaxPrice(Double.parseDouble(value));
                                break;
                            case "sortBy":
                                switch (value) {
                                    case "book_id":
                                        conditions.setSortBy(Book.SortColumn.BOOK_ID);
                                        break;
                                    case "title":
                                        conditions.setSortBy(Book.SortColumn.TITLE);
                                        break;
                                    case "author":
                                        conditions.setSortBy(Book.SortColumn.AUTHOR);
                                        break;
                                    case "category":
                                        conditions.setSortBy(Book.SortColumn.CATEGORY);
                                        break;
                                    case "press":
                                        conditions.setSortBy(Book.SortColumn.PRESS);
                                        break;
                                    case "publish_year":
                                        conditions.setSortBy(Book.SortColumn.PUBLISH_YEAR);
                                        break;
                                    case "price":
                                        conditions.setSortBy(Book.SortColumn.PRICE);
                                        break;
                                    case "stock":
                                        conditions.setSortBy(Book.SortColumn.STOCK);
                                        break;
                                }
                                break;
                            case "sortOrder":
                                if (value.equals("ASC")) {
                                    conditions.setSortOrder(queries.SortOrder.ASC);
                                } else if (value.equals("DESC")) {
                                    conditions.setSortOrder(queries.SortOrder.DESC);
                                }
                                break;
                        }
                    }
                }
            }
            
            ApiResult result = library.queryBook(conditions);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            OutputStream outputStream = exchange.getResponseBody();
            
            if (result.ok) {
                BookQueryResults queryResults = (BookQueryResults) result.payload;
                List<Book> books = queryResults.getResults();
                
                ObjectMapper objectMapper = new ObjectMapper();
                String response = objectMapper.writeValueAsString(books);
                exchange.sendResponseHeaders(200, response.getBytes().length);
                outputStream.write(response.getBytes());
            } else {
                String response = "[]";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                outputStream.write(response.getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            String errorMessage = "查询图书失败: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, errorMessage.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(errorMessage.getBytes());
            outputStream.close();
        }
    }

    // 单本图书入库
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

            Book book = new Book();
            if (jsonMap.containsKey("title")) {
                book.setTitle((String) jsonMap.get("title"));
            }
            if (jsonMap.containsKey("author")) {
                book.setAuthor((String) jsonMap.get("author"));
            }
            if (jsonMap.containsKey("category")) {
                book.setCategory((String) jsonMap.get("category"));
            }
            if (jsonMap.containsKey("press")) {
                book.setPress((String) jsonMap.get("press"));
            }
            if (jsonMap.containsKey("publishYear")) {
                book.setPublishYear(((Number) jsonMap.get("publishYear")).intValue());
            }
            if (jsonMap.containsKey("price")) {
                book.setPrice(((Number) jsonMap.get("price")).doubleValue());
            }
            if (jsonMap.containsKey("stock")) {
                book.setStock(((Number) jsonMap.get("stock")).intValue());
            }

            ApiResult result = library.storeBook(book);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "图书入库成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "图书入库失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            // 发送响应
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            // 处理异常
            System.err.println("处理 POST 请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }

    // 修改图书信息
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

            Book book = new Book();

            if (jsonMap.containsKey("bookId")) {
                book.setBookId(((Number) jsonMap.get("bookId")).intValue());
            } else {
                String errorMessage = "缺少图书 ID";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                byte[] responseBytes = errorMessage.getBytes();
                exchange.sendResponseHeaders(400, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
                return;
            }

            if (jsonMap.containsKey("title")) {
                book.setTitle((String) jsonMap.get("title"));
            }
            if (jsonMap.containsKey("author")) {
                book.setAuthor((String) jsonMap.get("author"));
            }
            if (jsonMap.containsKey("category")) {
                book.setCategory((String) jsonMap.get("category"));
            }
            if (jsonMap.containsKey("press")) {
                book.setPress((String) jsonMap.get("press"));
            }
            if (jsonMap.containsKey("publishYear")) {
                book.setPublishYear(((Number) jsonMap.get("publishYear")).intValue());
            }
            if (jsonMap.containsKey("price")) {
                book.setPrice(((Number) jsonMap.get("price")).doubleValue());
            }

            ApiResult result = library.modifyBookInfo(book);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "图书信息修改成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "图书信息修改失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理 PUT 请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }

    // 删除图书
    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            int bookId = -1;

            if (query != null && query.startsWith("bookId=")) {
                try {
                    bookId = Integer.parseInt(query.substring(7));
                } catch (NumberFormatException e) {
                    String errorMessage = "图书 ID 格式不正确";
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    byte[] responseBytes = errorMessage.getBytes();
                    exchange.sendResponseHeaders(400, responseBytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(responseBytes);
                    outputStream.close();
                    return;
                }
            } else {
                String errorMessage = "缺少图书 ID 参数";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                byte[] responseBytes = errorMessage.getBytes();
                exchange.sendResponseHeaders(400, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
                return;
            }

            ApiResult result = library.removeBook(bookId);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "图书删除成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "图书删除失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理 DELETE 请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }

    // 更新图书库存
    private void handlePatchStockRequest(HttpExchange exchange) throws IOException {
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

            int bookId = -1;
            int deltaStock = 0;

            if (jsonMap.containsKey("bookId")) {
                bookId = ((Number) jsonMap.get("bookId")).intValue();
            } else {
                String errorMessage = "缺少图书 ID";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                byte[] responseBytes = errorMessage.getBytes();
                exchange.sendResponseHeaders(400, responseBytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseBytes);
                outputStream.close();
                return;
            }

            if (jsonMap.containsKey("deltaStock")) {
                deltaStock = ((Number) jsonMap.get("deltaStock")).intValue();
            }

            ApiResult result = library.incBookStock(bookId, deltaStock);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "图书库存更新成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "图书库存更新失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理 PATCH 请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }

    // 批量导入图书
    private void handleBatchImportRequest(HttpExchange exchange) throws IOException {
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
            List<Book> books = objectMapper.readValue(jsonStr,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Book.class));

            ApiResult result = library.storeBook(books);

            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            if (result.ok) {
                message = "批量导入图书成功";
                exchange.sendResponseHeaders(200, message.getBytes().length);
            } else {
                message = result.message != null ? result.message : "批量导入图书失败";
                exchange.sendResponseHeaders(400, message.getBytes().length);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();

        } catch (Exception e) {
            System.err.println("处理批量导入请求时发生异常: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "服务器处理批量导入请求时发生错误: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = errorMessage.getBytes();
            exchange.sendResponseHeaders(500, responseBytes.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.close();
        }
    }
}


