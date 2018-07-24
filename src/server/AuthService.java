package server;

import java.util.Date;
import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:userDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname, password FROM main\n" +
                "WHERE login = '%s'\n", login);
        int myHash = pass.hashCode();
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if(myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveHistory(String login, String msg) {
        String sql = String.format("INSERT INTO history (post, nick) " +
                "VALUES ('%s', '%s')", msg, login);
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static StringBuilder getHistoryChat() {
        StringBuilder stringBuilder = new StringBuilder();
        String sql = String.format("SELECT nick, post from history ORDER BY ID");
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stringBuilder.append(rs.getString("nick") + " " + rs.getString("post") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stringBuilder;
    }

    public static void addBlacklist(String nickWho, String nickWhom) {
        String sql = String.format("INSERT INTO blacklist (loginWho, loginWhom) " +
                "VALUES ('%s', '%s')", nickWho, nickWhom);
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkBlacklist(String nickWho, String nickWhom) {

        //проверяем добавили ли мы его в черный список
        String sql = String.format("SELECT loginWhom FROM blacklist\n" +
                "WHERE loginWho = '%s' AND loginWhom = '%s'\n", nickWho, nickWhom);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //проверяем добавили ли он нас в черный список
        String sql2 = String.format("SELECT loginWhom FROM blacklist\n" +
                "WHERE loginWho = '%s' AND loginWhom = '%s'\n", nickWhom, nickWho);
        try {
            ResultSet rs2 = stmt.executeQuery(sql2);
            while (rs2.next()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean checkBan(String nick) {

        //проверяем бан лист
        String sql = String.format("SELECT endTime FROM banlist\n" +
                "WHERE nickname = '%s'\n", nick);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Date date = new Date();
                if (!date.after(rs.getTimestamp(1))) {
                    return true;
                }else {
                    delBan(nick);
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    //админские дела
    private static void delBan(String nick) {

            try {
                String query = String.format("DELETE FROM banlist\n" +
                        "      WHERE nickname = '%s'", nick);
                Statement st = connection.createStatement();
                st.executeUpdate(query);

                System.out.println(nick + " разблокирован.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    public static String addBan(String nickAdmin, String nickUser, long time) {
        if (checkThatAdmin(nickAdmin)) {

            Date date = new Date();
            date.setTime(date.getTime() + time*60000);

            try {
                String query = "INSERT INTO banlist (nickname, endTime) VALUES (?, ?)";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, nickUser);
                ps.setTimestamp(2, new java.sql.Timestamp(date.getTime()));
                ps.executeUpdate();
                System.out.println("Время бана" + date.toString());
                return "Пользователь добавлен в бан до" + date.toString();
            } catch (SQLException e) {
                e.printStackTrace();
                return "Какие-то проблемы с базой данных";
            }
        } else {return "У вас нет прав администратора";}
    }

    public static String addUser(String nickAdmin, String login, String pass, String nick, Boolean admin) {
        if (checkThatAdmin(nickAdmin)) {
            try {
                String query = "INSERT INTO main (login, password, nickname, admin) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, login);
                ps.setInt(2, pass.hashCode());
                ps.setString(3, nick);
                ps.setBoolean(4, admin);
                ps.executeUpdate();
                return "Пользователь добавлен в базу данных";
            } catch (SQLException e) {
                e.printStackTrace();
                return "Какие-то проблемы с базой данных";
            }
        } else {return "У вас нет прав администратора";}
    }

    private static boolean checkThatAdmin(String nick) {
        String sql = String.format("SELECT admin FROM main\n" +
                "WHERE nickname = '%s'\n", nick);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
