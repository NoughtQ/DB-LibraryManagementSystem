package dbSystem;

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // Check if the book already exists in the library
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? and author = ?"
            );
            checkPStmt.setString(1, book.getCategory());
            checkPStmt.setString(2, book.getTitle());
            checkPStmt.setString(3, book.getPress());
            checkPStmt.setInt(4, book.getPublishYear());
            checkPStmt.setString(5, book.getAuthor());
            ResultSet rs = checkPStmt.executeQuery();
            // If so, terminate the operation
            if (rs.next()) {
                return new ApiResult(false, "Book already exists");
            }

            // Try to store the new book
            PreparedStatement storePStmt = conn.prepareStatement(
            "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            storePStmt.setString(1, book.getCategory());
            storePStmt.setString(2, book.getTitle());
            storePStmt.setString(3, book.getPress());
            storePStmt.setInt(4, book.getPublishYear());
            storePStmt.setString(5, book.getAuthor());
            storePStmt.setDouble(6, book.getPrice());
            storePStmt.setInt(7, book.getStock());
            storePStmt.executeUpdate();
            rs = storePStmt.getGeneratedKeys();
            if (rs.next()) {
                // If it succeeds, update the book id of the book object
                book.setBookId(rs.getInt(1));
            } else {
                // Else throw the exception
                throw new SQLException("Failed to insert book");
            }

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully store a book");
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // Check if stock + deltaStock is negative
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT stock FROM book WHERE book_id = ?"
            );
            checkPStmt.setInt(1, bookId);
            ResultSet rs = checkPStmt.executeQuery();
            // If there's no such book, terminate the operation
            if (!rs.next()) {
                return new ApiResult(false, "Book doesn't exist");
            }
            int stock = rs.getInt("stock");
            // If it's negative, terminate the operation too
            if (stock + deltaStock < 0) {
                return new ApiResult(false, "Stock can't be negative");
            }

            // Update the stock
            PreparedStatement updatePStmt = conn.prepareStatement(
            "UPDATE book SET stock = ? WHERE book_id = ?"
            );
            updatePStmt.setInt(1, stock + deltaStock);
            updatePStmt.setInt(2, bookId);
            updatePStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully increase stock of the book");
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            PreparedStatement storePStmt = conn.prepareStatement(
            "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ResultSet rs;

            // Store books in order
            for (Book book : books) {
                // Try to store the new book
                storePStmt.setString(1, book.getCategory());
                storePStmt.setString(2, book.getTitle());
                storePStmt.setString(3, book.getPress());
                storePStmt.setInt(4, book.getPublishYear());
                storePStmt.setString(5, book.getAuthor());
                storePStmt.setDouble(6, book.getPrice());
                storePStmt.setInt(7, book.getStock());
                storePStmt.executeUpdate();
                rs = storePStmt.getGeneratedKeys();
                if (rs.next()) {
                    // If it succeeds, update the book id of the book object
                    book.setBookId(rs.getInt(1));
                } else {
                    // Else throw the exception
                    throw new SQLException("Failed to insert book");
                }
            }
            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully store books");
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // Firstly check if the book exists
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM book WHERE book_id = ?"
            );
            checkPStmt.setInt(1, bookId);
            ResultSet rs = checkPStmt.executeQuery();
            // If there's no such book, terminate the operation
            if (!rs.next()) {
                // System.out.println("Book doesn't exist");
                return new ApiResult(false, "Book doesn't exist");
            }

            // Check if the book is returned
            PreparedStatement checkPStmt2 = conn.prepareStatement(
            "SELECT * FROM borrow WHERE book_id = ? AND return_time = 0"
            );
            checkPStmt2.setInt(1, bookId);
            rs = checkPStmt2.executeQuery();
            // If not, terminate this operation
            if (rs.next()) {
                // System.out.println("Someone hasn't returned the book yet");
                return new ApiResult(false, "Someone hasn't returned the book yet");
            }

            // Remove the book
            PreparedStatement removePStmt = conn.prepareStatement(
            "DELETE FROM book WHERE book_id = ?"
            );
            removePStmt.setInt(1, bookId);
            removePStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully remove a book");
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // Firstly check if the book exists
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM book WHERE book_id = ?"
            );
            checkPStmt.setInt(1, book.getBookId());
            ResultSet rs = checkPStmt.executeQuery();
            // If there's no such book, terminate the operation
            if (!rs.next()) {
                // System.out.println("Book doesn't exist");
                return new ApiResult(false, "Book doesn't exist");
            }

            // modify the info of book
            PreparedStatement modifyPStmt = conn.prepareStatement(
            "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? " +
                "WHERE book_id = ?"
            );
            modifyPStmt.setString(1, book.getCategory());
            modifyPStmt.setString(2, book.getTitle());
            modifyPStmt.setString(3, book.getPress());
            modifyPStmt.setInt(4, book.getPublishYear());
            modifyPStmt.setString(5, book.getAuthor());
            modifyPStmt.setDouble(6, book.getPrice());
            modifyPStmt.setInt(7, book.getBookId());
            modifyPStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println("Exception: " + e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully modify the book info");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        List<Book> results = new ArrayList<Book>();
        try {
            conn.setAutoCommit(false);

            // SQL string builder
            StringBuilder sql = new StringBuilder("SELECT * FROM book WHERE 0=0");
            // filter contains all parameters of SQL statement
            List<Object> filter = new ArrayList<Object>();

            // Parse all conditions
            if (conditions.getCategory() != null) {
                sql.append(" AND category = ?");
                filter.add(conditions.getCategory());
            }
            if (conditions.getTitle() != null) {
                sql.append(" AND title LIKE ?");
                filter.add("%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                sql.append(" AND press LIKE ?");
                filter.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                sql.append(" AND publish_year >= ?");
                filter.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                sql.append(" AND publish_year <= ?");
                filter.add(conditions.getMaxPublishYear());
            }
            if (conditions.getAuthor() != null) {
                sql.append(" AND author LIKE ?");
                filter.add("%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                sql.append(" AND price >= ?");
                filter.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                sql.append(" AND price <= ?");
                filter.add(conditions.getMaxPrice());
            }
            sql.append(" ORDER BY ");
            if (conditions.getSortBy() != null) {
                sql.append(conditions.getSortBy().getValue());
                sql.append(" ");
                if (conditions.getSortOrder() != null) {
                    sql.append(conditions.getSortOrder().getValue());
                }
                sql.append(", book_id");
            } else {
                sql.append("book_id ");
                if (conditions.getSortOrder() != null) {
                    sql.append(conditions.getSortOrder().getValue());
                }
            }

            // Insert all parameters
            PreparedStatement pStmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < filter.size(); i++) {
                pStmt.setObject(i + 1, filter.get(i));
            }

            // Collect the result of query
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                Book book = new Book();
                book.setBookId(rs.getInt("book_id"));
                book.setCategory(rs.getString("category"));
                book.setTitle(rs.getString("title"));
                book.setPress(rs.getString("press"));
                book.setPublishYear(rs.getInt("publish_year"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));
                book.setStock(rs.getInt("stock"));
                results.add(book);
            }

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully make a query", new BookQueryResults(results));
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            ResultSet rs;

            // Firstly check if the book exists
            PreparedStatement getStockPtStmt = conn.prepareStatement(
                "SELECT stock FROM book WHERE book_id = ? FOR UPDATE"    // Add lock
            );
            getStockPtStmt.setInt(1, borrow.getBookId());
            rs = getStockPtStmt.executeQuery();
            if (!rs.next()) {
                // System.out.println("Book doesn't exist");
                return new ApiResult(false, "Book doesn't exist");
            }
            int stock = rs.getInt("stock");
            if (stock <= 0) {
                // System.out.println("Stock is empty");
                return new ApiResult(false, "The stock is empty");
            }

            // Then check if the user has returned the book
            PreparedStatement checkPStmt = conn.prepareStatement(
                "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0 FOR UPDATE"    // Add lock
            );
            checkPStmt.setInt(1, borrow.getBookId());
            checkPStmt.setInt(2, borrow.getCardId());
            rs = checkPStmt.executeQuery();
            // If so, terminate this operation
            if (rs.next()) {
                return new ApiResult(false, "The user hasn't returned the book yet");
            }

            // Add the record of borrow
            PreparedStatement borrowPtStmt = conn.prepareStatement(
            "INSERT INTO borrow (card_id, book_id, borrow_time, return_time) VALUES (?, ?, ?, ?)"
            );
            borrowPtStmt.setInt(1, borrow.getCardId());
            borrowPtStmt.setInt(2, borrow.getBookId());
            borrowPtStmt.setLong(3, borrow.getBorrowTime());
            borrowPtStmt.setLong(4, 0);
            borrowPtStmt.executeUpdate();

            // Update the stock
            PreparedStatement stockPtStmt = conn.prepareStatement(
            "UPDATE book SET stock = stock - 1 WHERE book_id = ?"
            );
//            stockPtStmt.setInt(1, stock - 1);
            stockPtStmt.setInt(1, borrow.getBookId());
            stockPtStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println("Exception: " + e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully borrow a book");
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            ResultSet rs;

            // Check if the user has borrowed the book
            PreparedStatement checkPStmt = conn.prepareStatement(
                "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND borrow_time = ? FOR UPDATE"    // Add lock
            );
            checkPStmt.setInt(1, borrow.getBookId());
            checkPStmt.setInt(2, borrow.getCardId());
            checkPStmt.setLong(3, borrow.getBorrowTime());
            rs = checkPStmt.executeQuery();
            // If not, terminate this operation
            if (!rs.next()) {
                return new ApiResult(false, "The user hasn't borrowed the book");
            }
            // Or if the user has already returned the book, terminate the operation
            if (rs.getLong("return_time") != 0) {
                return new ApiResult(false, "The user has already returned the book");
            }

            // Update the return time of borrow
            PreparedStatement returnTimePtStmt = conn.prepareStatement(
            "UPDATE borrow SET return_time = ? WHERE book_id = ? AND card_id = ? AND borrow_time = ?"
            );
            returnTimePtStmt.setLong(1, borrow.getReturnTime());
            returnTimePtStmt.setInt(2, borrow.getBookId());
            returnTimePtStmt.setInt(3, borrow.getCardId());
            returnTimePtStmt.setLong(4, borrow.getBorrowTime());
            returnTimePtStmt.executeUpdate();

            // Update the stock
            PreparedStatement stockPtStmt = conn.prepareStatement(
            "UPDATE book SET stock = stock + 1 WHERE book_id = ?"
            );
//            stockPtStmt.setInt(1, stock + 1);
            stockPtStmt.setInt(1, borrow.getBookId());
            stockPtStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully return the book");
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        List<BorrowHistories.Item> items = new ArrayList<BorrowHistories.Item>();
        try {
            conn.setAutoCommit(false);
            ResultSet rs;
            BorrowHistories.Item item;
            Book book;
            Borrow borrow;

            // Firstly check if the card exists
            PreparedStatement cardPStmt = conn.prepareStatement(
            "SELECT * FROM card WHERE card_id = ?"
            );
            cardPStmt.setInt(1, cardId);
            rs = cardPStmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "The card doesn't exist");
            }

            PreparedStatement borrowHistoryPStmt = conn.prepareStatement(
            "SELECT book.*, borrow_time, return_time FROM borrow " +
                "JOIN book ON book.book_id = borrow.book_id " +
                "WHERE borrow.card_id = ? ORDER BY borrow_time DESC, borrow.book_id"
            );
            borrowHistoryPStmt.setInt(1, cardId);
            rs = borrowHistoryPStmt.executeQuery();

            // Collect the borrow history
            while (rs.next()) {
                book = new Book();
                book.setBookId(rs.getInt("book_id"));
                book.setCategory(rs.getString("category"));
                book.setTitle(rs.getString("title"));
                book.setPress(rs.getString("press"));
                book.setPublishYear(rs.getInt("publish_year"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));

                borrow = new Borrow();
                borrow.setBorrowTime(rs.getLong("borrow_time"));
                borrow.setReturnTime(rs.getLong("return_time"));

                item = new BorrowHistories.Item(cardId, book, borrow);
                items.add(item);
            }

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully show the borrow history", new BorrowHistories(items));
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            // conn.setAutoCommit(false);

            // Check if the card exists
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?"
            );
            checkPStmt.setString(1, card.getName());
            checkPStmt.setString(2, card.getDepartment());
            checkPStmt.setString(3, card.getType().getStr());
            ResultSet rs = checkPStmt.executeQuery();
            // If so, terminate this operation
            if (rs.next()) {
                // System.out.println("The card already exists");
                return new ApiResult(false, "The card already exists");
            }

            // Register the card
            PreparedStatement cardPStmt = conn.prepareStatement(
            "INSERT INTO card (name, department, type) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            cardPStmt.setString(1, card.getName());
            cardPStmt.setString(2, card.getDepartment());
            cardPStmt.setString(3, card.getType().getStr());
            cardPStmt.executeUpdate();

            // Obtain and update the generated card id
            rs = cardPStmt.getGeneratedKeys();
            if (rs.next()) {
                card.setCardId(rs.getInt(1));
            }

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully register a card");
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            // Check if the card exists
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM card WHERE card_id = ?"
            );
            checkPStmt.setInt(1, card.getCardId());
            ResultSet rs = checkPStmt.executeQuery();

            if (!rs.next()) {
                return new ApiResult(false, "Card doesn't exist");
            }

            // Update the card info
            PreparedStatement updatePStmt = conn.prepareStatement(
            "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?"
            );
            updatePStmt.setString(1, card.getName());
            updatePStmt.setString(2, card.getDepartment());
            updatePStmt.setString(3, card.getType().getStr());
            updatePStmt.setInt(4, card.getCardId());
            updatePStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Card info modified successfully");
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            // conn.setAutoCommit(false);

            // Firstly check if the card exists
            PreparedStatement checkPStmt = conn.prepareStatement(
            "SELECT * FROM card WHERE card_id = ?"
            );
            checkPStmt.setInt(1, cardId);
            ResultSet rs = checkPStmt.executeQuery();
            // If not, terminate the operation
            if (!rs.next()) {
                // System.out.println("The card doesn't exist");
                return new ApiResult(false, "The card doesn't exist");
            }

            // Then check if the card has unreturned book(s)
            PreparedStatement checkBorrowPStmt = conn.prepareStatement(
            "SELECT * FROM borrow WHERE card_id = ? AND return_time = 0"
            );
            checkBorrowPStmt.setInt(1, cardId);
            rs = checkBorrowPStmt.executeQuery();
            // If so, also terminate the operation
            if (rs.next()) {
                // System.out.println("The card has unreturned book(s)");
                return new ApiResult(false, "The card has unreturned book(s)");
            }

            // Remove the card
            PreparedStatement removePStmt = conn.prepareStatement(
            "DELETE FROM card WHERE card_id = ?"
            );
            removePStmt.setInt(1, cardId);
            removePStmt.executeUpdate();

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully remove the card");
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        List<Card> cards = new ArrayList<Card>();
        try {
            conn.setAutoCommit(false);

            PreparedStatement showPStmt = conn.prepareStatement(
            "SELECT * FROM card ORDER BY card_id"
            );
            ResultSet rs = showPStmt.executeQuery();

            while (rs.next()) {
                Card card = new Card();
                card.setCardId(rs.getInt("card_id"));
                card.setName(rs.getString("name"));
                card.setDepartment(rs.getString("department"));
                card.setType(Card.CardType.values(rs.getString("type")));
                cards.add(card);
            }

            // Commit the transaction
            commit(conn);
        } catch (Exception e) {
            // If any error happens, rollback the transaction
            rollback(conn);
            // System.out.println(e.getMessage());
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully show all cards", new CardList(cards));
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
