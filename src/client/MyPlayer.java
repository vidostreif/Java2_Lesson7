package client;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URISyntaxException;

public class MyPlayer{
    private MediaPlayer mediaPlayer;

    public MyPlayer() throws URISyntaxException {
        Media sound = new Media(getClass().getResource("/Resurses/OneKiss.mp3").toURI().toString());
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.pause();
    }

    public String Format(Duration d) {
        final int seconds = (int) (d.toMillis() / 1000) % 60;
        final int minutes = (int) (d.toMillis() / (1000 * 60));
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void stop() {
        mediaPlayer.stop();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void play() {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
            mediaPlayer.play();
        }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
            mediaPlayer.stop();
            mediaPlayer.setStartTime(Duration.ZERO);
            mediaPlayer.play();
        }
    }

    public String getTime() {
        return Format(mediaPlayer.getCurrentTime());
    }

    public void setVolume(double volume) {
        mediaPlayer.setVolume(volume);
    }
}
