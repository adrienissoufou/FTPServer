package impl;

import api.Logger;

import javax.activation.CommandInfo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FTPConnection implements Closeable, Runnable {

    private final Map<String, CommandInfo> commands = new HashMap<>();


    private final FTPServer server;
    private ServerSocket serverSocket;
    private Socket clientConnection;
    private BufferedReader bufferedReader;
    private PrintWriter bufferedWriter;
    private String clientIP;

    FTPConnection(FTPServer server, Socket clientConnection) throws IOException {
        this.server = server;
        this.serverSocket = server.getSocket();
        this.clientConnection = clientConnection;
        this.clientIP = clientConnection.getInetAddress().toString().replace("/", "");
        Logger.setLogData("Client: " + clientIP + " connected");
        this.bufferedReader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
        this.bufferedWriter = new PrintWriter(new OutputStreamWriter(clientConnection.getOutputStream()),true);
        bufferedWriter.println("220 - connected to server\r\n");

    }

    private void handleConnection() throws IOException{
        String command;
        while (server.isRunning()) {
            command = bufferedReader.readLine();
            //Logger.setLogData(command);
        }
    }


    @Override
    public void close() throws IOException {

    }

    @Override
    public void run() {
        try {
            handleConnection();
        } catch (IOException e) {
            Logger.setLogData("Error in handling client: " + clientIP + " connection");
        }
    }
}
