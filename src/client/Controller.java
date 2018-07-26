package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;

public class Controller {

    @FXML Slider volumeSlider;

    @FXML Label currentTime;

    @FXML TextField msgField;

    @FXML ListView chatArea;

    @FXML HBox bottomPanel;

    @FXML HBox upperPanel;

    @FXML TextField loginField;

    @FXML PasswordField passwordField;

    @FXML ListView clientList;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADDRESS = "localhost";
    final int PORT = 16586;
    String nickName = "NoName";

    private boolean isAuthorized;

    public boolean isAuthorized() {
        return isAuthorized;
    }

    @FXML
    public void initialize() {
        Updater updater = new Updater(this);
        Timer timer = new Timer();
        timer.schedule(updater,0,1000);

        Main.control = this;

        chatArea.setCellFactory(lv -> new ListCell<ColoredText>() {
            @Override
            protected void updateItem(ColoredText item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText(null);
                    setTextFill(null);
                } else {
                    setText(item.getText());
                    setTextFill(item.getColor());
                }
            }
        });
    }

    public void stopMusic() {
        Main.myMediaPlayer.stop();
    }

    public void pauseMusic() {
        Main.myMediaPlayer.pause();
    }

    public void playMusic() {
        Main.myMediaPlayer.play();
    }

    public void sendVolume() {
        Main.myMediaPlayer.setVolume(volumeSlider.getValue());
    }

    public void updateTime() {
        currentTime.setText(Main.myMediaPlayer.getTime());
    }

    public void selecFromList(){
        if (clientList.getSelectionModel().getSelectedItem()!= null) {
            String selItem = clientList.getSelectionModel().getSelectedItem().toString();
            msgField.clear();
            msgField.requestFocus();
            msgField.appendText("/w " + selItem + " ");
        }
    }

    private void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if(!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientList.setManaged(false);
            clientList.setVisible(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientList.setManaged(true);
            clientList.setVisible(true);
        }
    }

    private void connect() {
        try {
            setAuthorized(false);
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/authok")) {
                            setAuthorized(true);
                            //Получаем наше имя
                            String[] tokens = str.split(" ", 2);
                            if (tokens.length == 2) {
                                nickName = tokens[1];
                            } else  {
                                nickName = "NoName";
                            }
                            chatArea.getItems().clear();
                            break;
                        } else {
                            addMsgOnChatArea(str, Color.RED);

                        }
                    }
                    getHistory();

                    while (true) {
                        String str = in.readUTF();
                        if (str.equals("/serverclosed")) break;
                        else if (str.startsWith("/history")) {bringOutHistory(str);}
                        else if (str.startsWith("/clientlist")) {
                            String[] tokens = str.split(" ");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientList.getItems().add(tokens[i]);
                                    }
                                }
                            });
                        } else {
                            addMsgOnChatArea(str, getMsgColor(str));
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    addMsgOnChatArea("Сервер разорвал соединение!", Color.RED);
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthorized(false);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            socket = null;
            in = null;
            out = null;
            addMsgOnChatArea("Не удалось установить соединение с сервером.", Color.RED);
        }
    }

    private boolean sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            addMsgOnChatArea("Исходящий поток не доступен!", Color.RED);
            return false;
        }
    }

    public void enterMsg() {
        if (sendMsg(msgField.getText())) {
            msgField.clear();
            msgField.requestFocus();
        }
    }

    public void closeConnect() {
        sendMsg("/end");
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        if (sendMsg("/auth " + loginField.getText() + " " + passwordField.getText())) {
            loginField.clear();
            passwordField.clear();
        }
    }

    private void getHistory() {
        try {
            out.writeUTF("/history ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void bringOutHistory(String msg) {
        String[] tokens = msg.split("\n");
        if (tokens.length > 1) {
            for (String token: tokens) {
                if(token.equals("/history")){addMsgOnChatArea("История сообщений:", Color.OLIVE);}
                else {addMsgOnChatArea(token, getMsgColor(token));}
            }
        } else if (tokens.length == 1) {
            addMsgOnChatArea("История сообщений пуста.", Color.OLIVE);
        }
    }

    private Color getMsgColor(String msg){
        Color color;
        if (msg.startsWith(nickName)) {
            color = Color.DARKBLUE;
        }else {color = Color.CHOCOLATE;}
        return color;
    }

    private void addMsgOnChatArea(String msg, Color color) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                chatArea.getItems().add(new ColoredText(msg, color));
                chatArea.scrollTo(chatArea.getItems().size());
            }
        });
    }
}