package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;

    public Server() throws SQLException {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        try {
            AuthService.connect();
            server = new ServerSocket(16586);
            System.out.println("Сервер запущен. Ожидаем клиентов...");
            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void broadcastMsg(ClientHandler from, String msg) {
        for (ClientHandler o : clients) {
            if(AuthService.checkBlacklist(from.getNick(), o.getNick())) {
                o.sendMsg(msg);
            }
        }
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastMsg(client, client.getNick() + " подключился к чату!");
        broadcastClientList();
        System.out.println("Client " + client.getNick() + " подключился к чату!");
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastMsg(client, client.getNick() + " вышел из чата!");
        broadcastClientList();
        System.out.println("Client " + client.getNick() + " закрыл подключение!");
    }

    private void broadcastClientList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientlist ");
        for (ClientHandler o: clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
        for(ClientHandler o: clients) {
            o.sendMsg(out);
        }
    }

    public ClientHandler getClient(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick)) {
                return client;
            }
        }
        return null;
    }

    public String getAllClientsNick() {
        StringBuffer clientsNick = new StringBuffer();

        clientsNick.append("/clientlist");
        for (ClientHandler client : clients) {
            clientsNick.append(" " + client.getNick());
        }
        return clientsNick.toString();
    }
}
