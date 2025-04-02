import com.sun.net.httpserver.HttpServer;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import handlers.CardHandler;
import handlers.BookHandler;
import handlers.BorrowHandler;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }

            // backend part
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/book", new BookHandler(connector));
            server.createContext("/card", new CardHandler(connector));
            server.createContext("/borrow", new BorrowHandler(connector));
            server.start();
            System.out.println("Server is listening on port 8080");

            // release database connection handler(bonus part)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (connector.release()) {
                    log.info("Success to release connection.");
                } else {
                    log.warning("Failed to release connection.");
                }
            }));

            // release database connection handler(basic part)
//            if (connector.release()) {
//                log.info("Success to release connection.");
//            } else {
//                log.warning("Failed to release connection.");
//            }
         
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
