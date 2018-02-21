package impl;

import api.IFileSystem;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static <F> String format(IFileSystem<F> fileSystem, F file) throws IOException {
        String lastModified = validDateFormat(fileSystem.getLastModified(file));
        return String.format("%s %3d %5s %5s %8d %s %s\r\n",
                fileSystem.getPermission(file),
                fileSystem.getHardLinks(file),
                fileSystem.getOwner(file),
                fileSystem.getGroup(file),
                fileSystem.getSize(file),
                lastModified,
                fileSystem.getName(file));
    }

    private static String validDateFormat(long date) {
        String[] splittedDate = new Date(date).toString().split("\\s");
        StringBuilder validDate = new StringBuilder();
        String month = splittedDate[1].toLowerCase();
        String day = splittedDate[2];
        String time = splittedDate[3].substring(0, splittedDate[3].lastIndexOf(":"));
        validDate.append(month).append(" ").append(day).append(" ").append(time);
        return validDate.toString();
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
