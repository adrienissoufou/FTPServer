package api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public interface IFileSystem<F extends Object> {

    F getRoot();

    String getPath(F file);

    Boolean exists(F file);

    Boolean isDirectory(F file);

    String getName(F file);

    F getParent(F file) throws IOException;

    ArrayList<F> listFiles(F dir) throws IOException;

    F findFile(F cwd, String path) throws IOException;

    String getPermission(F file) throws IOException;

    String getOwner(F file);

    String getGroup(F file);

    long getSize(F file);

    long getLastModified(F file);

    int getHardLinks(F file);

    InputStream readFile(F file, long start) throws IOException;

    OutputStream writeFile(F file, long start) throws IOException;

    void mkdirs(F file) throws IOException;

    void delete(F file) throws IOException;

    void rename(F from, F to) throws IOException;
}
