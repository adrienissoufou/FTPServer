package api;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.time.LocalTime;

public final class Logger {

    private static TextArea log;
    private static StringBuffer wholeLog = new StringBuffer();

    public static void setLogData(String data) {
        wholeLog.append(String.format("%s %3s %3s\n",
                LocalTime.now(),
                "-",
                data));
        Platform.runLater(() -> log.setText(wholeLog.toString()));
    }

    public static void registerLog(TextArea textArea) {
        log = textArea;
    }

}
