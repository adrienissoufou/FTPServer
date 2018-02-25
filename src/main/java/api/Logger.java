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


/*    private static StringProperty logData = new SimpleStringProperty();
    private static StringBuffer wholeLog = new StringBuffer();

    public static StringProperty logDataProperty() { return logData; }
    public static void setLogData(String data) {
        wholeLog.append(LocalTime.now()).append(" - ").append(data).append("\n");
        logData.set(wholeLog.toString());
    }*/
}
