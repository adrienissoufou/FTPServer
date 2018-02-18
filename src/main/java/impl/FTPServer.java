package impl;

import api.Logger;
import api.iComand.ArgsArrayCommand;
import api.iComand.Command;
import api.iComand.NoArgsCommand;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FTPServer {

    private int port;
    private ServerSocket socket = null;
    private final String ftpHome = "/Users/alexeisevko/Desktop/server";
    private ArrayList<FTPConnection> connections;
    private static volatile Boolean isRunning = false;

    public FTPServer() {
        this.connections = new ArrayList<>();
    }

    public void createServerState(int port) {
        this.port = port;
        try {
            socket = new ServerSocket(port, 50, InetAddress.getLocalHost());
            isRunning = true;
            Logger.setLogData("Server address is: " +  InetAddress.getLocalHost().toString() + ":" + port);
        } catch (IOException e) {
            Logger.setLogData("Socket error: " + e.getMessage());
            System.exit(-1);
        }
        acceptConnections();
    }


    private void acceptConnections() {
        while (true) {
            try {
                Socket clientConnection = socket.accept();
                FTPConnection ftpConnection = new FTPConnection(this, clientConnection);
                connections.add(ftpConnection);
                new Thread(ftpConnection).start();
            } catch (IOException e) {
                System.err.print("FTP error: " + e.getMessage());
            }
        }
    }


    ServerSocket getSocket() {
        return socket;
    }

    Boolean isRunning() {
        return isRunning;
    }

    public InetAddress getAddress() {
        return socket != null ? socket.getInetAddress() : null;
    }



}
