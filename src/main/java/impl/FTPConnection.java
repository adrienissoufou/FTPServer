package impl;

import api.Logger;
import api.ResponseException;
import api.iComand.ArgsArrayCommand;
import api.iComand.Command;
import api.iComand.NoArgsCommand;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

    FTPConnection(FTPServer server, Socket clientConnection) throws IOException {
        this.server = server;
        this.serverSocket = server.getSocket();
        this.clientConnection = clientConnection;
        this.clientIP = clientConnection.getInetAddress().toString().replace("/", "");
        Logger.setLogData("Client: " + clientIP + " connected");
        this.bufferedReader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));
        registerAllCommands();
    }

    private void registerAllCommands() {
        registerCommand("FEAT", this::feat);
    }

    private void feat() {
        StringBuilder list = new StringBuilder();
        list.append("- Supported Features:\r\n");

        list.append(' ').append("UTF8").append("\r\n");

        sendResponse(211, list.toString());
        sendResponse(211, "End");
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

//    private void handleConnection() throws IOException{
//        bufferedWriter.println("220 - connected to server\r\n");
//        String command;
//        while (server.isRunning()) {
//            command = bufferedReader.readLine();
//            Logger.setLogData(command);
//            switch (commandParser(command)) {
//                case "USER":
//                    bufferedWriter.println("230 - user logged in\r\n");
//                    break;
//                case "PASV":
//                    bufferedWriter.println("230 - user logged in\r\n");
//                    break;
//                case "PWD":
//                    bufferedWriter.println("257 - \"/server\" current\r\n");
//                    break;
//                case "FEAT":
//                    bufferedWriter.println("211 - Features\r\n");
//                    bufferedWriter.println("UTF8\r\n");
//                    bufferedWriter.println("211 - End\r\n");
//                    break;
//                case "TYPE":
//                    bufferedWriter.println("200 - A\r\n");
//                    break;
//                case "PORT":
//                    FTPServer server = this.server;
//                    passiveServer = new ServerSocket(0, 5, server.getAddress());
//
//                    String host = passiveServer.getInetAddress().getHostAddress();
//                    int port = passiveServer.getLocalPort();
//                    String[] addr = host.split("\\.");
//
//                    String address = addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3];
//                    String addressPort = port / 256 + "," + port % 256;
//                    bufferedWriter.println("227 - Enabled Passive Mode (" + address + "," + addressPort + ")\r\n");
//                    break;
//                case "LIST":
//                    bufferedWriter.println("150 - Sending list of files\r\n");
//                    bufferedWriter.println("\"/server/README.rtf\"\r\n");
//                    bufferedWriter.println("226 - List of files was sent\r\n");
//                    break;
//            }
//        }
//    }

    void update() {
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

    void process(String cmd) {
        int firstSpace = cmd.indexOf(' ');
        if(firstSpace < 0) firstSpace = cmd.length();

        Command command = commands.get(cmd.substring(0, firstSpace).toUpperCase());

        if(command == null) {
            sendResponse(502, "Unknown command");
            return;
        }

        processCommand(command, cmd, firstSpace != cmd.length() ? cmd.substring(firstSpace + 1) : "");
    }

    private String commandParser(String command) {
        return command.split("\\s+")[0];
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

    @Override
    public void close() throws IOException {
        close();
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
