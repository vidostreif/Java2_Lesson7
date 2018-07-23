package client;

import javafx.application.Platform;

import java.util.TimerTask;

public class Updater extends TimerTask {

    public Controller contr;

    Updater(Controller contr){
        this.contr = contr;
    }

    @Override
    public void run(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                contr.updateTime();
            }
        });
    }

}
