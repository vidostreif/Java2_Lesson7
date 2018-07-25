package server;


public class InactivityHandler extends Thread {
    private ClientHandler clientHandler;
    public long finTimeLastMsg;
    private int secondsBeforeDisconnecting;
    private boolean destroy = false;

    public InactivityHandler(long TimeLastMsg, ClientHandler clientHandler, int secondsBeforeDisconnecting){
        this.finTimeLastMsg = TimeLastMsg;
        this.clientHandler = clientHandler;
        this.secondsBeforeDisconnecting = secondsBeforeDisconnecting;
    }

    public InactivityHandler(long TimeLastMsg, ClientHandler clientHandler){
        this.finTimeLastMsg = TimeLastMsg;
        this.clientHandler = clientHandler;
        this.secondsBeforeDisconnecting = 120;
    }

    public void run() {
        while (true) {
            if (!destroy) {
                if (finTimeLastMsg + 1000 * secondsBeforeDisconnecting < System.currentTimeMillis()) {
                    System.out.println("Client " + clientHandler.getNick() + " отключен за бездействие более " + this.secondsBeforeDisconnecting + "сек.");
                    clientHandler.sendMsg("Вы отключены за бездействие более " + this.secondsBeforeDisconnecting + "сек.");
                    clientHandler.sendMsg("/serverclosed");
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {break;}
        }
    }

    public void setDestroy(boolean destroy) {
        this.destroy = destroy;
    }
}