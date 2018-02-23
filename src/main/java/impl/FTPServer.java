package impl;

import api.Logger;

import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.*;

public class FTPServer {


    private final List<FTPConnection> connections = Collections.synchronizedList(new ArrayList<FTPConnection>());

    private ServerSocket socket = null;
    private static volatile Boolean isRunning = false;
    private ServerFileSystem fileSystem;

    public FTPServer(ServerFileSystem fileSystem) {
        this.fileSystem = fileSystem;
        try {
            DataBase.init();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { DataBase.getStmt().close(); } catch(SQLException se) { /*can't do anything */ }
            try { DataBase.getCon().close(); } catch(SQLException se) { /*can't do anything */ }
            try { DataBase.getRs().close(); } catch(SQLException se) { /*can't do anything */ }
        }));
    }



    public void createServerState(int port) {
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
                synchronized(connections) {
                    connections.add(ftpConnection);
                }
                new Thread(ftpConnection).start();
            } catch (IOException e) {
                System.err.print("FTP error: " + e.getMessage());
            }
        }
    }

    public void dispose() {

        if(!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }

        synchronized(connections) {
            for(FTPConnection con : connections) {
                try {
                    con.close();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            connections.clear();
        }

        Logger.setLogData("Server stopped.");
    }

    protected void removeConnection(FTPConnection connection) {
        connections.remove(connection);
    }

    public ServerFileSystem getFileSystem() {
        return fileSystem;
    }


    public InetAddress getAddress() {
        return socket != null ? socket.getInetAddress() : null;
    }


    public static Boolean getIsRunning() {
        return isRunning;
    }
}
