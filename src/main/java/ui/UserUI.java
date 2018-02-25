package ui;

import api.Event;
import api.observer.IFileSystemObserver;
import extensions.FxDialogs;
import impl.ObserverNotificator;
import impl.server.FTPServer;
import impl.server.ServerFileSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import api.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Pattern;


public class UserUI implements Initializable, IFileSystemObserver {

    private FTPServer ftpServerInstance;
    private ServerFileSystem fileSystem;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TextArea log;

    @FXML
    private TextField serverPort;


    static private  String homeDir = "/Users/alexeisevko/Desktop/server";


    @FXML
    private void displayTreeView() throws IOException {
        File root = fileSystem.getRoot();

        TreeItem<String> rootItem = new TreeItem<>(root.getName());
        treeView.setShowRoot(true);

        treeView.setCellFactory(TextFieldTreeCell.forTreeView());

        ArrayList<File> fileList = fileSystem.listFiles(root);


        for (File file : fileList) {
            createTree(file, rootItem);
        }

        treeView.setRoot(rootItem);
    }

    @FXML
    private void createTree(File file, TreeItem<String> parent) throws IOException {
        if (fileSystem.isDirectory(file)) {
            TreeItem<String> treeItem = new TreeItem<>(fileSystem.getName(file));
            parent.getChildren().add(treeItem);
            for (File f : fileSystem.listFiles(file)) {
                if (!f.toString().contains("/.DS_Store")) {
                    createTree(f, treeItem);
                }
            }
        } else {
            parent.getChildren().add(new TreeItem<>(fileSystem.getName(file)));
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileSystem = new ServerFileSystem(new File(homeDir));
        try {
            displayTreeView();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.registerLog(this.log);
        ObserverNotificator.getInstance().addObserver(Event.FS, this);
    }

    @FXML
    private void runServer() {
        if (portIsValid()) {
            if (ftpServerInstance == null) {
                ftpServerInstance = new FTPServer(fileSystem);
                new Thread(() -> ftpServerInstance.createServerState(Integer.parseInt(serverPort.getText()))).start();
            }else {
                FxDialogs.showError("Error", "Server is already running");
            }
        } else {
            FxDialogs.showError("Error", "Port format is not valid");
        }
    }

    private Boolean portIsValid() {
        Pattern portPattern = Pattern.compile("[0-9]{1,5}");
        return portPattern.matcher(serverPort.getText()).matches();
    }

    @FXML
    private void stopServer() {
        if (FTPServer.getIsRunning()) {
            ftpServerInstance.dispose();
            ftpServerInstance = null;
        } else {
            FxDialogs.showError("Error", "Server is not running");
        }
    }

    @Override
    public void updateState() {
        Platform.runLater(() -> {
            try {
                displayTreeView();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
