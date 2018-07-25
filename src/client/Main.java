package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//Для авторизации
// admin admin - администратор
// login1 pass1
// login2 pass2
// login3 pass3

public class Main extends Application {
    public static MyPlayer myMediaPlayer;
    public static Controller control;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Chat and music!");

        Scene scene = new Scene(root, 350, 300);

        myMediaPlayer = new MyPlayer();

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    @Override
    public void stop() throws Exception {
        if (control.isAuthorized()) {
            control.closeConnect();
        }
        super.stop();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}



