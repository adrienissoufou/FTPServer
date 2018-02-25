package impl.server.handlers;

import api.Event;
import api.ResponseException;
import impl.ObserverNotificator;
import impl.server.FTPConnection;
import impl.server.ServerFileSystem;
import impl.Utils;

import javax.management.Notification;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHandler {

    private final FTPConnection connection;

    private ServerFileSystem fileSystem;
    private File cwd;

    private File rnFile = null;

    private ObserverNotificator notificator;

    public FileHandler(FTPConnection connection) {
        this.connection = connection;
        this.fileSystem = connection.getServer().getFileSystem();
        this.cwd = fileSystem.getRoot();
        notificator = ObserverNotificator.getInstance();
    }

    public void registerCommands() {
        connection.registerCommand("PWD", this::pwd);
        connection.registerCommand("CWD", this::cwd);
        connection.registerCommand("CDUP", this::cdup);
        connection.registerCommand("LIST", this::list);
        connection.registerCommand("MKD", this::mkd);
        connection.registerCommand("RMD", this::rmd);
        connection.registerCommand("DELE", this::dele);
        connection.registerCommand("RNFR", this::rnfr);
        connection.registerCommand("RNTO", this::rnto);
        connection.registerCommand("ABOR", this::abor);
        connection.registerCommand("APPE", this::appe);
        connection.registerCommand("STOR", this::stor);
        connection.registerCommand("RETR", this::retr);
    }


    private File getFile(String path) throws IOException {
        if(path.equals("...") || path.equals("..")) {
            return fileSystem.getParent(cwd);
        } else if(path.equals("/")) {
            return fileSystem.getRoot();
        } else if(path.startsWith("/")) {
            return fileSystem.findFile(fileSystem.getRoot(), path.substring(1));
        } else {
            return fileSystem.findFile(cwd, path);
        }
    }

    private void pwd() {
//        String path = cwd == fileSystem.getRoot() ? "/" + fileSystem.getRoot().getName() :"/" + fileSystem.getPath(cwd);
        String path = "/" + fileSystem.getPath(cwd);
        connection.sendResponse(257, path + " CWD Name");
    }

    private void cwd(String path) throws IOException {
        File dir = getFile(path);

        if(fileSystem.isDirectory(dir)) {
            cwd = dir;
            connection.sendResponse(250, "The working directory was changed");
        } else {
            connection.sendResponse(550, "Not a valid directory");
        }
    }

    private void cdup() throws IOException {
        cwd = fileSystem.getParent(cwd);
        connection.sendResponse(200, "The working directory was changed");
    }

    private void mkd(String path) throws IOException {
        File file = getFile(path);

        fileSystem.mkdirs(file);
        notificator.fire(Event.FS);
        connection.sendResponse(257, '"' + path + '"' + " Directory Created");
    }

    private void rmd(String path) throws IOException {
        File file = getFile(path);

        if(!fileSystem.isDirectory(file)) {
            connection.sendResponse(550, "Not a directory");
            return;
        }

        fileSystem.delete(file);
        notificator.fire(Event.FS);
        connection.sendResponse(250, '"' + path + '"' + " Directory Deleted");
    }

    private void dele(String path) throws IOException {
        File file = getFile(path);

        if(fileSystem.isDirectory(file)) {
            connection.sendResponse(550, "Not a file");
            return;
        }

        fileSystem.delete(file);
        notificator.fire(Event.FS);
        connection.sendResponse(250, '"' + path + '"' + " File Deleted");
    }

    private void rnfr(String path) throws IOException {
        rnFile = getFile(path);
        connection.sendResponse(350, "Rename request received");
    }

    private void rnto(String path) throws IOException {
        if(rnFile == null) {
            connection.sendResponse(503, "No rename request was received");
            return;
        }

        fileSystem.rename(rnFile, getFile(path));
        rnFile = null;
        ObserverNotificator.getInstance().fire(Event.FS);
        connection.sendResponse(250, "File successfully renamed");
    }

    private void abor() throws IOException {
        connection.abortDataTransfers();
        connection.sendResponse(226, "All transfers were aborted successfully");
    }

    private void appe(String path) throws IOException {
        File file = getFile(path);

        connection.sendResponse(150, "Receiving a file stream for " + path);
        receiveStream(fileSystem.writeFile(file, fileSystem.exists(file) ? fileSystem.getSize(file) : 0));
        notificator.fire(Event.FS);
    }

    private void stor(String path) throws IOException {
        File file = getFile(path);

        connection.sendResponse(150, "Receiving a file stream for " + path);

        receiveStream(fileSystem.writeFile(file, 0));
        notificator.fire(Event.FS);
    }

    private void retr(String path) throws IOException {
        File file = getFile(path);

        connection.sendResponse(150, "Sending the file stream for " + path + " (" + fileSystem.getSize(file) + " bytes)");
        sendStream(Utils.readFileSystem(fileSystem, file, 0, connection.isAscii()));
    }

    private void list(String args[]) throws IOException {
        connection.sendResponse(150, "File status okay; about to open data connection.");

        File dir = args.length > 0 && !args[0].equals("-a") ? getFile(args[0]) : cwd;

        if(!fileSystem.isDirectory(dir)) {
            connection.sendResponse(550, "Not a directory");
            return;
        }

        StringBuilder data = new StringBuilder();


        for (File file: fileSystem.listFiles(dir)) {
            data.append(Utils.format(fileSystem, file));
        }

        connection.sendData(String.valueOf(data).getBytes("UTF-8"));
        connection.sendResponse(226, "Requested file action successful");
    }

    private void receiveStream(OutputStream out) {
        new Thread(() -> {
            try {
                connection.receiveData(out);
                connection.sendResponse(226, "File received!");
            } catch(ResponseException ex) {
                connection.sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                connection.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    private void sendStream(InputStream in) {
        new Thread(() -> {
            try {
                connection.sendData(in);
                connection.sendResponse(226, "File sent!");
            } catch(ResponseException ex) {
                connection.sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                connection.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

}
