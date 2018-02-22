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
        String month = splittedDate[1];
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

    public static void write(OutputStream out, byte[] bytes, int len, boolean ascii) throws IOException {
        if(ascii) {
            byte lastByte = 0;
            for(int i = 0; i < len; i++) {
                byte b = bytes[i];

                if(b == '\n' && lastByte != '\r') {
                    out.write('\r');
                }

                out.write(b);
                lastByte = b;
            }
        } else {
            out.write(bytes, 0, len);
        }
    }

    public static <F> InputStream readFileSystem(IFileSystem<F> fs, F file, long start, boolean ascii) throws IOException {
        if(ascii && start > 0) {
            InputStream in = new BufferedInputStream(fs.readFile(file, 0));
            long offset = 0;

            // Count \n as two bytes for skipping
            while(start >= offset++) {
                int c = in.read();
                if(c == -1) {
                    throw new IOException("Couldn't skip this file. End of the file was reached");
                } else if(c == '\n') {
                    offset++;
                }
            }

            return in;
        } else {
            return fs.readFile(file, start);
        }
    }

}
