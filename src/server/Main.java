package server;

import java.sql.SQLException;

public class Main {
    public static Server server = null;

    public static void main(String[] args) throws SQLException {
        server = new Server();
    }
}
