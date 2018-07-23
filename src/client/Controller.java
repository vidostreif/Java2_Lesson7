package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;



import java.util.Timer;

public class Controller {

    @FXML
    Slider volumeSlider;

    @FXML
    Label currentTime;

    @FXML
    TextField msgField;

    @FXML
    TextArea chatArea;

    @FXML
    HBox bottomPanel;

    @FXML
    HBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADDRESS = "localhost";
    final int PORT = 16586;

    private boolean isAuthorized;

    @FXML
    public void initialize() {
        Updater updater = new Updater(this);
        Timer timer = new Timer();
        timer.schedule(updater,0,1000);

        Main.control = this;
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


    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
        }
    }

    public void connect() {
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
                            break;
                        } else {
                            chatArea.appendText(str + "\n");
                        }
                    }
                    while (true) {
                        String str = in.readUTF();
                        chatArea.appendText(str + "\n");
                        if (str.equals("/serverclosed")) break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    chatArea.appendText("Сервер разорвал соединение!" + "\n");
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
            chatArea.appendText("Не удалось установить соединение с сервером." + "\n");
        }
    }

    public boolean sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Исходящий поток не доступен!" + "\n");
        } finally {
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
}