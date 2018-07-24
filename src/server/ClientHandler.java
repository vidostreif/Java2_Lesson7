package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static jdk.nashorn.internal.runtime.JSType.isNumber;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String nick = "";

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth")) {
                            if (slashAuth(str)) {
                                break;
                            }
                        }
                    }

                    while (true) {
                        String str = in.readUTF();
                        System.out.println("Client: " + str);
                        //Отключение
                        if (str.equals("/end")) {
                            out.writeUTF("/serverclosed");
                            break;
                        }

                        if (!AuthService.checkBan(nick)) {
                            if (str.equals("/help")) {
                                slashHelp();
                            } else if (str.startsWith("/blacklist")) {
                                slashBlacklist(str);
                            } else if (str.startsWith("/history ")) {
                                slashHistory();
                            } else if (str.equals("/list")) {
                                slashList();
                            } else if (str.startsWith("/w")) {
                                slashW(str);
                            } else if (str.startsWith("/ban")) {
                                slashBan(str);
                            } else {
                                AuthService.saveHistory(nick, str);
                                server.broadcastMsg(this, nick + ": " + str);
                            }
                        } else {
                            sendMsg("Вы забанены!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean slashAuth(String msg) {
        //Авторизация на сервере
        String[] tokens = msg.split(" ");
        if (tokens.length == 3) {
            String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
            if (newNick != null) {
                if (!server.isNickBusy(newNick)) {
                    sendMsg("/authok");
                    nick = newNick;
                    server.subscribe(this);
                    sendMsg("Вы можете ввести /help что бы узнать все доступные команды.");
                    return true;
                } else {
                    sendMsg("Учетная запись уже используется");
                }
            } else {
                sendMsg("Неверный логин/пароль");
            }
        } else {
            sendMsg("Вы не ввели логин/пароль");
        }

        return false;

    }

    private void slashHelp() {
        //запрос списка комманд
        String commands = "Доступные команды:" + "\n" +
                "/end - закрыть подключение" + "\n" +
                "/list - список подключенных клиентов" + "\n" +
                "/w [имя] [сообщение] - сообщение конкретному клиенту " + "\n";
        sendMsg(commands);
    }

    private void slashBlacklist(String msg) {
        //Добавление пользователя в черный список
        String[] tokens = msg.split(" ", 2);
        if (tokens.length == 2) {
            AuthService.addBlacklist(nick, tokens[1]);
            sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
        } else if (tokens.length == 1) {
            sendMsg("Укажите, кого вы хотите добавить в черный список");
        }
    }

    private void slashList() {
        //запрос списка пользователей
        sendMsg("Список всех авторизованных пользователей:" + "\n" + server.getAllClientsNick());
    }

    private void slashHistory() {
        //История сообщений
        StringBuilder stringBuilder = AuthService.getHistoryChat();
        sendMsg(stringBuilder.toString());
    }

    private void slashW(String msg) {
        //Отправка сообщения конкретному пользователю
        String[] tokens = msg.split(" ", 3);
        if (tokens.length == 3) {
            ClientHandler targetClient = server.getClient(tokens[1]);
            if (targetClient != null) {
                targetClient.sendMsg("Приватное сообщение от " + nick + ": " + tokens[2]);
                if (targetClient != this) {
                    sendMsg(nick + " для " + targetClient.getNick() + ": " + tokens[2]);
                }
            } else {
                sendMsg("Пользователя с таким ником нет в чате.");
            }
        } else if (tokens.length == 2) {
            sendMsg("Укажите сообщение для пользователя");
        } else if (tokens.length == 1) {
            sendMsg("Укажите пользователя");
        }
    }

    private void slashBan(String msg) {
        //Добавление пользователя в бан
        String[] tokens = msg.split(" ");
        if (tokens.length == 3) {
            sendMsg(AuthService.addBan(nick, tokens[1], Long.parseLong(tokens[2])));
        } else if (tokens.length == 2) {
            sendMsg(AuthService.addBan(nick, tokens[1], 5));
        } else if (tokens.length == 1) {
            sendMsg("Укажите, кого вы хотите добавить в бан");
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

}
