package api;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalTime;

public final class Logger {
    private static StringProperty logData = new SimpleStringProperty();
    private static StringBuffer wholeLog = new StringBuffer();

    public static StringProperty logDataProperty() { return logData; }
    public static void setLogData(String data) {
        wholeLog.append(LocalTime.now()).append(" - ").append(data).append("\n");
        logData.set(wholeLog.toString());
    }
    public static String getLogData() { return logData.get(); }
}
