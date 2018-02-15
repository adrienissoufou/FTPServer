package api;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class Logger {
    private static StringProperty logData = new SimpleStringProperty();
    private static String wholeLog = "";

    public static StringProperty logDataProperty() { return logData; }
    public static void setLogData(String data) {
        wholeLog += data +"\n";
        logData.set(wholeLog);
    }
    public static String getLogData() { return logData.get(); }
}
