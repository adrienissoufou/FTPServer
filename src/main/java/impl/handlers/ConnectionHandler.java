package impl.handlers;

import impl.FTPConnection;
import impl.FTPServer;
import impl.Utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler {

    private final FTPConnection connection;

    private boolean passive = false;
    private ServerSocket passiveServer = null;
    private String activeHost = null;
    private int activePort = 0;

    private boolean ascii = true;
    private boolean stop = false;


    public ConnectionHandler(FTPConnection connection) {
        this.connection = connection;
    }

    public boolean shouldStop() {
        return stop;
    }

    public boolean isAsciiMode() {
        return ascii;
    }

    public Socket createDataSocket() throws IOException {
        if(passive && passiveServer != null) {
            return passiveServer.accept();
        } else {
            return new Socket(activeHost, activePort);
        }
    }

    public void onDisconnected() {
        if(passiveServer != null) {
            Utils.closeQuietly(passiveServer);
            passiveServer = null;
        }
    }

    public void registerCommands() {
        connection.registerCommand("USER", this::user);
        connection.registerCommand("PASV", this::pasv);
        connection.registerCommand("PORT", this::port);
        connection.registerCommand("PASS", this::pass);
        connection.registerCommand("QUIT", this::quit);
    }

    private void user(String username) {
        //check users
        //sendResponse(230, "User logged in, proceed");
        connection.sendResponse(331, "User name okay, need password.");
    }

    private void pass(String pass){
        connection.sendResponse(230, "User logged in, proceed");
    }

    private void pasv() throws IOException {
        FTPServer server = connection.getServer();
        passiveServer = new ServerSocket(0, 5, server.getAddress());
        passive = true;

        String host = passiveServer.getInetAddress().getHostAddress();
        int port = passiveServer.getLocalPort();


        String[] addr = host.split("\\.");

        String address = addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3];
        String addressPort = port / 256 + "," + port % 256;

        connection.sendResponse(227, "Enabled Passive Mode (" + address + "," + addressPort + ")");
    }

    private void quit() {
        connection.sendResponse(221, "Closing connection...");
        stop = true;
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
        connection.sendResponse(200, "Enabled Active Mode");
    }

}
