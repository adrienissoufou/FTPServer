package impl;

import api.Logger;
import api.ResponseException;
import api.iComand.ArgsArrayCommand;
import api.iComand.Command;
import api.iComand.NoArgsCommand;
import impl.handlers.ConnectionHandler;
import impl.handlers.FileHandler;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class FTPConnection implements Closeable, Runnable {

    final Map<String, Command> commands = new HashMap<>();
    private final ArrayDeque<Socket> dataConnections = new ArrayDeque<>();

    private ConnectionHandler connectionHandler;
    private FileHandler fileHandler;


    private final FTPServer server;
    private Socket clientConnection;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientIP;
    private boolean responseSent = true;
    private boolean ascii = false;

    FTPConnection(FTPServer server, Socket clientConnection) throws IOException {
        this.server = server;
        this.clientConnection = clientConnection;
        this.clientIP = clientConnection.getInetAddress().toString().replace("/", "");
        Logger.setLogData("Client: " + clientIP + " connected");
        this.bufferedReader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));

        this.connectionHandler = new ConnectionHandler(this);
        this.fileHandler = new FileHandler(this);


        registerAllCommands();
        sendResponse(220, "Service ready for new user");
    }

    private void registerAllCommands() {
        registerCommand("FEAT", this::feat);
        registerCommand("TYPE", this::type);
        this.fileHandler.registerCommands();
        this.connectionHandler.registerCommands();
    }

    private void feat() {
        StringBuilder list = new StringBuilder();
        list.append("- Supported Features:\r\n");

        list.append(' ').append("UTF8").append("\r\n");

        sendResponse(211, list.toString());
        sendResponse(211, "End");
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


    public void abortDataTransfers() {
        while(!dataConnections.isEmpty()) {
            Socket socket = dataConnections.poll();
            if(socket != null) Utils.closeQuietly(socket);
        }
    }


    public void sendData(InputStream in) throws ResponseException {
        if(clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = connectionHandler.createDataSocket();
            dataConnections.add(socket);
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


    public void receiveData(OutputStream out) throws ResponseException {
        if(clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = connectionHandler.createDataSocket();
            dataConnections.add(socket);
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


    public void sendData(byte[] data) throws ResponseException {
        if (clientConnection.isClosed()) return;

        Socket socket;
        try {
            socket = connectionHandler.createDataSocket();
            dataConnections.add(socket);
            OutputStream out = socket.getOutputStream();

            Utils.write(out, data, data.length, ascii);

            out.flush();

            Utils.closeQuietly(out);
            Utils.closeQuietly(socket);
        } catch (IOException e) {
            throw new ResponseException(425, "An error occurred while transferring the data");
        }


    }

    public void registerCommand(String label, Command cmd) { addCommand(label, cmd) ; }

    public void registerCommand(String label, NoArgsCommand cmd) {
        addCommand(label, cmd);
    }

    public void registerCommand(String label, ArgsArrayCommand cmd) {
        addCommand(label, cmd);
    }

    private void addCommand(String label, Command cmd) {
        commands.put(label.toUpperCase(), cmd);
    }

    private void update() {
        if(connectionHandler.shouldStop()) {
            Utils.closeQuietly(this);
            return;
        }


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

    private void processCommand(Command cmd, String name, String args) {

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


    public FTPServer getServer() {
        return server;
    }

    public boolean isAscii() {
        return ascii;
    }

    @Override
    public void close() throws IOException {
        if (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        connectionHandler.onDisconnected();

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
