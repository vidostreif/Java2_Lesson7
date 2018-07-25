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
                if (myHash == dbHash) {
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
        //запрашиваем последние 50 записей из истории сообщений
        int lasRecord = 50;

        try {
            //создаем временную таблицу если она отсутствует
            String sql = "create TEMP table if not exists tempT (\n" +
                    "id int,\n" +
                    "nick TEXT,\n" +
                    "post TEXT,\n" +
                    "PRIMARY KEY(id));";
            stmt.execute(sql);

            //очищаем временную таблицу
            sql = "DELETE FROM tempT;";
            stmt.execute(sql);

            //копируем во временную таблиуц последние записи
            sql = String.format("INSERT INTO tempT (id, nick, post)\n" +
                    "SELECT id, nick, post\n" +
                    "FROM history ORDER BY ID DESC LIMIT 0,'%s';", lasRecord);
            stmt.execute(sql);

            //запрашиваем все данные из временной таблицы с правильной сортировкой
            sql = "SELECT id, nick, post FROM tempT ORDER BY ID;";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                stringBuilder.append(rs.getString("nick") + " " + rs.getString("post") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stringBuilder;
    }

    public static String addBlacklist(String nickWho, String nickWhom) {

        if(nickWho.equals(nickWhom)){return "Вы не можете добавить себя в черный список";}

        if (!checkThatAdmin(nickWhom)) {
            String sql = String.format("INSERT INTO blacklist (loginWho, loginWhom) " +
                    "VALUES ('%s', '%s')", nickWho, nickWhom);
            try {
                stmt.execute(sql);
                return "Пользователь добавлен в черный список.";
            } catch (SQLException e) {
                e.printStackTrace();
                return "Кикие-то проблемы с базой данных";
            }
        }else {return "Вы не можете добавить администратора в черный список";}
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

    public static String delFromBlacklist(String nickWho, String nickWhom) {

        //проверяем добавили ли мы его в черный список
        String sql = String.format("SELECT loginWhom FROM blacklist\n" +
                "WHERE loginWho = '%s' AND loginWhom = '%s'\n", nickWho, nickWhom);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String query = String.format("DELETE FROM blacklist\n" +
                        "WHERE loginWho = '%s' AND loginWhom = '%s'\n", nickWho, nickWhom);
                Statement st = connection.createStatement();
                st.executeUpdate(query);

                return (nickWhom + " удален из черного списка.");
            } else {return (nickWhom + " отсутствует в вашем черном списке.");}
        } catch (SQLException e) {
            e.printStackTrace();
            return "Кикие-то проблемы с базой данных";
        }
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
                } else {
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
            date.setTime(date.getTime() + time * 60000);

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
        } else {
            return "У вас нет прав администратора";
        }
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
        } else {
            return "У вас нет прав администратора";
        }
    }

    public static boolean checkThatAdmin(String nick) {
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
