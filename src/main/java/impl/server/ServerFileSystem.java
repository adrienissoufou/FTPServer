package impl.server;

import api.IFileSystem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class ServerFileSystem implements IFileSystem<File> {

    private final File rootDir;

    public ServerFileSystem(File rootDir) {
        this.rootDir = rootDir;

        if (!rootDir.exists())
            rootDir.mkdirs();
    }


    @Override
    public File getRoot() {
        return rootDir;
    }

    @Override
    public String getPath(File file) {
        return rootDir.toURI().relativize(file.toURI()).getPath();
    }

    @Override
    public Boolean exists(File file) {
        return file.exists();
    }

    @Override
    public Boolean isDirectory(File file) {
        return file.isDirectory();
    }

    @Override
    public String getName(File file) {
        return file.getName();
    }

    @Override
    public File getParent(File file) throws IOException {
        if(file.equals(rootDir)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file.getParentFile();
    }

    @Override
    public ArrayList<File> listFiles(File dir) throws IOException {
        if(!dir.isDirectory()) throw new IOException("Not a directory");

        return new ArrayList<>(Arrays.asList(dir.listFiles()));
    }

    @Override
    public File findFile(File cwd, String path) throws IOException {
        File file = new File(cwd, path);

        if(!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file;
    }

    @Override
    public String getPermission(File file) throws IOException {
        Path path = Paths.get(String.valueOf(file));
        Set<PosixFilePermission> set = Files.getPosixFilePermissions(path);
        return PosixFilePermissions.toString(set);
    }

    @Override
    public String getOwner(File file) {
        return "alexeisevko";
    }

    @Override
    public String getGroup(File file) {
        return "-";
    }

    @Override
    public long getSize(File file) {
        return file.length();
    }

    @Override
    public long getLastModified(File file) {
        return file.lastModified();
    }

    @Override
    public int getHardLinks(File file) {
        return file.isDirectory() ? 3 : 1;
    }

    @Override
    public InputStream readFile(File file, long start) throws IOException {
        if(start <= 0) {
            return new FileInputStream(file);
        }

        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);

        return new FileInputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public OutputStream writeFile(File file, long start) throws IOException {
        if(start <= 0) {
            return new FileOutputStream(file, false);
        } else if(start == file.length()) {
            return new FileOutputStream(file, true);
        }

        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(start);

        return new FileOutputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public void mkdirs(File file) throws IOException {
        if(!file.mkdirs()) throw new IOException("Couldn't create the directory");
    }

    @Override
    public void delete(File file) throws IOException {
        if(!file.delete()) throw new IOException("Couldn't delete the file");
    }

    @Override
    public void rename(File from, File to) throws IOException {
        if(!from.renameTo(to)) throw new IOException("Couldn't rename the file");
    }

    private boolean isInside(File dir, File file) {
        if(file.equals(dir)) return true;

        try {
            return file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
        } catch(IOException ex) {
            return false;
        }
    }
}
