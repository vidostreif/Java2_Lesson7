package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;

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
                String str;
                boolean authorized = false;
                long timeLastMsg = System.currentTimeMillis();
                InactivityHandler inactivityHandler = new InactivityHandler(timeLastMsg, this);
                inactivityHandler.setDaemon(true);

                while (true) {
                    try {
                        str = in.readUTF();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    if (str.startsWith("/auth")) {
                        if (slashAuth(str)) {
                            authorized = true;
                            inactivityHandler.finTimeLastMsg = System.currentTimeMillis();
                            inactivityHandler.start();
                            break;
                        }
                    }
                }

                //если авторизовался, то слушаем дальше
                if (authorized) {
                    while (true) {
                        try {
                            str = in.readUTF();
                        } catch (IOException e) {
                            break;
                        }

                        inactivityHandler.finTimeLastMsg = System.currentTimeMillis();
                        System.out.println("Client " + nick + " пишет: " + str);

                        //Отключение
                        if (str.equals("/end")) {
                            sendMsg("/serverclosed");
                            break;
                        }

                        if (!AuthService.checkBan(nick)) {
                            if (str.equals("/help")) {
                                slashHelp();
                            } else if (str.startsWith("/blacklist")) {
                                slashBlacklist(str);
                            } else if (str.startsWith("/history")) {
                                slashHistory();
                            } else if (str.equals("/list")) {
                                slashList();
                            } else if (str.startsWith("/w")) {
                                slashW(str);
                            } else if (str.startsWith("/ban")) {
                                slashBan(str);
                            } else if (str.startsWith("/adduser")) {
                                slashAddUser(str);
                            } else {
                                AuthService.saveHistory(nick, str);
                                server.broadcastMsg(this, nick + ": " + str);
                            }
                        } else {
                            sendMsg("Вы забанены!");
                        }
                    }
                }
                inactivityHandler.setDestroy(true);
                exitFromServer();

            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exitFromServer() {
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
                "/w [имя] [сообщение] - сообщение конкретному клиенту" + "\n" +
                "/blacklist [имя] - добавление клиента в ваш черный список" + "\n" +
                "/history - запросить историю сообщений" + "\n";
        sendMsg(commands);
    }

    private void slashBlacklist(String msg) {
        //Добавление пользователя в черный список
        String[] tokens = msg.split(" ", 2);
        if (tokens.length == 2) {
            sendMsg(AuthService.addBlacklist(nick, tokens[1]));
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
                if(AuthService.checkBlacklist(this.nick, targetClient.nick)) {
                    targetClient.sendMsg("Приватное сообщение от " + nick + ": " + tokens[2]);
                    if (targetClient != this) {
                        sendMsg(nick + " для " + targetClient.getNick() + ": " + tokens[2]);
                    }
                }else {sendMsg("Вы не можете отправлять сообщения этому пользователю!");}
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

    private void slashAddUser(String msg) {
        //Добавление пользователя в базу
        String[] tokens = msg.split(" ", 5);
        if (tokens.length == 5) {
            sendMsg(AuthService.addUser(nick, tokens[1], tokens[2], tokens[3], Boolean.parseBoolean(String.valueOf(tokens[4]))));
        } else {
            sendMsg("Пользователь добавляется в таком формате: /adduser [login] [password] [nick] [true - если администратор, false - если нет]");
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
