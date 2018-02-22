package impl;

import api.Logger;
import api.ResponseException;
import api.iComand.ArgsArrayCommand;
import api.iComand.Command;
import api.iComand.NoArgsCommand;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class FTPConnection implements Closeable, Runnable {

    final Map<String, Command> commands = new HashMap<>();


    private final FTPServer server;
    private ServerSocket serverSocket;
    private Socket clientConnection;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientIP;
    private ServerSocket passiveServer = null;
    private boolean responseSent = true;
    private ServerFileSystem fileSystem;
    private File cwd = null;
    private String activeHost = null;
    private int activePort = 0;
    private boolean passive = false;
    private boolean ascii = false;
    private File rnFile;

    FTPConnection(FTPServer server, Socket clientConnection) throws IOException {
        this.server = server;
        this.fileSystem = server.getFileSystem();
        this.cwd = fileSystem.getRoot();
        this.serverSocket = server.getSocket();
        this.clientConnection = clientConnection;
        this.clientIP = clientConnection.getInetAddress().toString().replace("/", "");
        Logger.setLogData("Client: " + clientIP + " connected");
        this.bufferedReader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));
        registerAllCommands();
        sendResponse(220, "Service ready for new user");
    }

    private void registerAllCommands() {
        registerCommand("FEAT", this::feat);
        registerCommand("USER", this::user);
        registerCommand("PASV", this::pasv);
        registerCommand("PWD", this::pwd);
        registerCommand("CWD", this::cwd);
        registerCommand("CDUP", this::cdup);
        registerCommand("TYPE", this::type);
        registerCommand("PORT", this::port);
        registerCommand("LIST", this::list);
        registerCommand("PASS", this::pass);
        registerCommand("MKD", this::mkd);
        registerCommand("RMD", this::rmd);
        registerCommand("DELE", this::dele);
        registerCommand("RNFR", this::rnfr);
        registerCommand("RNTO", this::rnto);
        registerCommand("ABOR", this::abor);
        registerCommand("APPE", this::appe);
        registerCommand("STOR", this::stor);
        registerCommand("RETR", this::retr);
        registerCommand("QUIT", this::quit);
    }

    private void feat() {
        StringBuilder list = new StringBuilder();
        list.append("- Supported Features:\r\n");

        list.append(' ').append("UTF8").append("\r\n");

        sendResponse(211, list.toString());
        sendResponse(211, "End");
    }

    private void user(String username) {
        //check users
        //sendResponse(230, "User logged in, proceed");
        sendResponse(331, "User name okay, need password.");
    }

    private void pass(String pass){
        sendResponse(230, "User logged in, proceed");
    }

    private void pasv() throws IOException {
        FTPServer server = this.server;
        passiveServer = new ServerSocket(0, 5, server.getAddress());
        passive = true;

        String host = passiveServer.getInetAddress().getHostAddress();
        int port = passiveServer.getLocalPort();


        String[] addr = host.split("\\.");

        String address = addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3];
        String addressPort = port / 256 + "," + port % 256;

        sendResponse(227, "Enabled Passive Mode (" + address + "," + addressPort + ")");
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
        String path = cwd == fileSystem.getRoot() ? "/" + fileSystem.getRoot().getName() :"/" + fileSystem.getPath(cwd);
        sendResponse(257, path + " CWD Name");
    }

    private void cwd(String path) throws IOException {
        File dir = getFile(path);

        if(fileSystem.isDirectory(dir)) {
            cwd = dir;
            sendResponse(250, "The working directory was changed");
        } else {
            sendResponse(550, "Not a valid directory");
        }
    }

    private void cdup() throws IOException {
        cwd = fileSystem.getParent(cwd);
        sendResponse(200, "The working directory was changed");
    }

    private void mkd(String path) throws IOException {
        File file = getFile(path);

        fileSystem.mkdirs(file);
        sendResponse(257, '"' + path + '"' + " Directory Created");
    }

    private void rmd(String path) throws IOException {
        File file = getFile(path);

        if(!fileSystem.isDirectory(file)) {
            sendResponse(550, "Not a directory");
            return;
        }

        fileSystem.delete(file);
        sendResponse(250, '"' + path + '"' + " Directory Deleted");
    }

    private void dele(String path) throws IOException {
        File file = getFile(path);

        if(fileSystem.isDirectory(file)) {
            sendResponse(550, "Not a file");
            return;
        }

        fileSystem.delete(file);
        sendResponse(250, '"' + path + '"' + " File Deleted");
    }

    private void rnfr(String path) throws IOException {
        rnFile = getFile(path);
        sendResponse(350, "Rename request received");
    }

    private void rnto(String path) throws IOException {
        if(rnFile == null) {
            sendResponse(503, "No rename request was received");
            return;
        }

        fileSystem.rename(rnFile, getFile(path));
        rnFile = null;

        sendResponse(250, "File successfully renamed");
    }

    private void abor() throws IOException {
        abortDataTransfers();
        sendResponse(226, "All transfers were aborted successfully");
    }

    public void abortDataTransfers() {
        //protected final
        ArrayDeque<Socket> dataConnections = new ArrayDeque<>();
        while(!dataConnections.isEmpty()) {
            Socket socket = dataConnections.poll();
            if(socket != null) Utils.closeQuietly(socket);
        }
    }

    private void appe(String path) throws IOException {
        File file = getFile(path);

        sendResponse(150, "Receiving a file stream for " + path);
        receiveStream(fileSystem.writeFile(file, fileSystem.exists(file) ? fileSystem.getSize(file) : 0));
    }

    private void stor(String path) throws IOException {
        File file = getFile(path);

        sendResponse(150, "Receiving a file stream for " + path);

        receiveStream(fileSystem.writeFile(file, 0));
    }

    private void retr(String path) throws IOException {
        File file = getFile(path);

        sendResponse(150, "Sending the file stream for " + path + " (" + fileSystem.getSize(file) + " bytes)");
        sendStream(Utils.readFileSystem(fileSystem, file, 0, ascii));
    }

    private void sendStream(InputStream in) {
        new Thread(() -> {
            try {
                sendData(in);
                sendResponse(226, "File sent!");
            } catch(ResponseException ex) {
                sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    private void sendData(InputStream in) throws ResponseException {
        if(clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = createDataSocket();
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                Utils.write(out, buffer, len, ascii);
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(SocketException ex) {
            throw new ResponseException(426, "Transfer aborted");
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        }
    }

    private void receiveStream(OutputStream out) {
        new Thread(() -> {
            try {
                receiveData(out);
                sendResponse(226, "File received!");
            } catch(ResponseException ex) {
                sendResponse(ex.getCode(), ex.getMessage());
            } catch(Exception ex) {
                sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    private void receiveData(OutputStream out) throws ResponseException {
        if(clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = createDataSocket();
            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.flush();
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(socket);
        } catch(SocketException ex) {
            throw new ResponseException(426, "Transfer aborted");
        } catch(IOException ex) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        }
    }

    private void type(String type) {
        type = type.toUpperCase();
        if (type.startsWith("A")) {
            ascii = true;
            sendResponse(200, "Type set to " + type);
        } else if (type.startsWith("I") ) {
            ascii = false;
            sendResponse(200, "Type set to " + type);
        } else {
            sendResponse(500, "Unknown type " + type);
        }
    }

    private void quit() {
        sendResponse(221, "Closing connection...");
        //stop = true;
    }

    private void port(String data) {
        String[] args = data.split(",");

        activeHost = args[0] + "." + args[1] + "." + args[2] + "." + args[3];
        activePort = Integer.parseInt(args[4]) * 256 + Integer.parseInt(args[5]);
        passive = false;

        if(passiveServer != null) {
            Utils.closeQuietly(passiveServer);
            passiveServer = null;
        }
        sendResponse(200, "Enabled Active Mode");
    }

    private void list(String args[]) throws IOException {
        sendResponse(150, "File status okay; about to open data connection.");

        File dir = args.length > 0 && !args[0].equals("-a") ? getFile(args[0]) : cwd;

        if(!fileSystem.isDirectory(dir)) {
            sendResponse(550, "Not a directory");
            return;
        }

        StringBuilder data = new StringBuilder();


        for (File file: fileSystem.listFiles(dir)) {
            data.append(Utils.format(fileSystem, file));
        }

        sendData(String.valueOf(data).getBytes("UTF-8"));
        sendResponse(226, "Requested file action successful");
    }

    private void sendData(byte[] data) throws ResponseException {
        if (clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = createDataSocket();
            OutputStream out = socket.getOutputStream();

            Utils.write(out, data, data.length, ascii);

            out.flush();

            Utils.closeQuietly(out);
            Utils.closeQuietly(socket);
        } catch (IOException e) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        }


    }

    private Socket createDataSocket() throws IOException {
        if(passive && passiveServer != null) {
            return passiveServer.accept();
        } else {
            return new Socket(activeHost, activePort);
        }
    }

    private void registerCommand(String label, Command cmd) { addCommand(label, cmd) ; }

    private void registerCommand(String label, NoArgsCommand cmd) {
        addCommand(label, cmd);
    }

    private void registerCommand(String label, ArgsArrayCommand cmd) {
        addCommand(label, cmd);
    }

    private void addCommand(String label, Command cmd) {
        commands.put(label.toUpperCase(), cmd);
    }

    private void update() {
        String line;

        try {
            line = bufferedReader.readLine();
        } catch (IOException e) {
           return;
        }

        if(line == null) {
            Utils.closeQuietly(this);
            return;
        }

        if(line.isEmpty())
            return;

        process(line);
    }

    private void process(String cmd) {
        Logger.setLogData(cmd);
        int firstSpace = cmd.indexOf(' ');
        if(firstSpace < 0) firstSpace = cmd.length();

        Command command = commands.get(cmd.substring(0, firstSpace).toUpperCase());

        if(command == null) {
            sendResponse(502, "Unknown command");
            return;
        }

        processCommand(command, cmd, firstSpace != cmd.length() ? cmd.substring(firstSpace + 1) : "");
    }

    void processCommand(Command cmd, String name, String args) {

        responseSent = false;

        try {
           cmd.run(name, args);
        } catch(ResponseException ex) {
            sendResponse(ex.getCode(), ex.getMessage());
        } catch(FileNotFoundException ex) {
            sendResponse(550, ex.getMessage());
        } catch(IOException ex) {
            sendResponse(450, ex.getMessage());
        } catch(Exception ex) {
            sendResponse(451, ex.getMessage());
            ex.printStackTrace();
        }

        if(!responseSent) sendResponse(200, "Done");
    }


    public void sendResponse(int code, String response) {
        if(clientConnection.isClosed()) return;

        if(response == null || response.isEmpty()) {
            response = "Unknown";
        }

        try {
            if(response.charAt(0) == '-') {
                bufferedWriter.write(code + response + "\r\n");
            } else {
                bufferedWriter.write(code + " " + response + "\r\n");
            }
            bufferedWriter.flush();
        } catch(IOException ex) {
            Utils.closeQuietly(this);
        }
        responseSent = true;
    }

    private void onDisconnected() {
        if(passiveServer != null) {
            Utils.closeQuietly(passiveServer);
            passiveServer = null;
        }
    }

    @Override
    public void close() throws IOException {
        if (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        onDisconnected();

        clientConnection.close();
        server.removeConnection(this);
    }

    @Override
    public void run() {
        while(!clientConnection.isClosed()) {
            update();
        }

        try {
            close();
        } catch(IOException ex) {
            Logger.setLogData("Error in handling client: " + clientIP + " connection");
        }
    }
}
