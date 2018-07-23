package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    String nick = "";

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
                        if (str.startsWith("/auth")) { // /auth login1 pass1 и так далее
                            String[] tokens = str.split(" ");
                            String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                            if (newNick != null) {
                                if(!server.isNickBusy(newNick)) {
                                    sendMsg("/authok");
                                    nick = newNick;
                                    server.subscribe(this);
                                    server.broadcastMsg(nick + " подключился к чату!");
                                    break;
                                } else {
                                    sendMsg("Учетная запись уже используется");
                                }
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String str = in.readUTF();
                        System.out.println("Client: " + str);

                        //Отключение
                        if (str.equals("/end")) {
                            out.writeUTF("/serverclosed");
                            System.out.println("Client " + nick + " закрыл подключение!");
                            server.broadcastMsg(nick + " вышел из чата!");
                        }
                        //запрос списка комманд
                        else if (str.equals("/help")) {
                            String commands = "Доступные команды:" + "\n" +
                                    "/end - закрыть подключение" + "\n" +
                                    "/list - список подключенных клиентов" + "\n" +
                                    "/w [имя] [сообщение] - сообщение конкретному клиенту " + "\n";
                            sendMsg(commands);
                        }
                        //запрос списка пользователей
                        else if (str.equals("/list")) {
                            sendMsg("Список всех авторизованных пользователей:" + "\n" + server.getAllClientsNick());
                        }
                        //Отправка сообщения конкретному пользователю
                        else if (str.startsWith("/w")) {
                            String[] tokens = str.split(" ", 3);
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
                        } else {
                            server.broadcastMsg(nick + ": " + str);
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
