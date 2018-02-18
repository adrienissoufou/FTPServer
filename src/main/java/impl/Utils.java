package impl;

import java.io.Closeable;
import java.io.IOException;

public class Utils {
    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch(IOException e) {}
    }
}
