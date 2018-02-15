package ui;

import com.sun.deploy.uitoolkit.impl.fx.ui.FXMessageDialog;
import extensions.FxDialogs;
import impl.FTPServer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import api.Logger;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class UserUI implements Initializable {

    private FTPServer ftpServerInstance;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane tabbedLog;

    @FXML
    private TextArea log;

    @FXML
    private Button startButton;

    @FXML
    private TextField serverPort;


    static private  String homeDir = "/Users/alexeisevko/Desktop/server";


    @FXML
    private void displayTreeView(String inputDirectoryLocation) {
        TreeItem<String> rootItem = new TreeItem<>(inputDirectoryLocation.substring(inputDirectoryLocation.lastIndexOf("/") + 1));

        treeView.setShowRoot(true);

        treeView.setCellFactory(TextFieldTreeCell.forTreeView());

        File fileInputDirectoryLocation = new File(inputDirectoryLocation);
        File fileList[] = fileInputDirectoryLocation.listFiles();


        for (File file : fileList) {
            createTree(file, rootItem);
        }

        treeView.setRoot(rootItem);
    }

    @FXML
    private void createTree(File file, TreeItem<String> parent) {
        if (file.isDirectory()) {
            TreeItem<String> treeItem = new TreeItem<>(file.getName());
            parent.getChildren().add(treeItem);
            for (File f : file.listFiles()) {
                if (!f.toString().contains("/.DS_Store")) {
                    createTree(f, treeItem);
                }
            }
        } else {
            parent.getChildren().add(new TreeItem<>(file.getName()));
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displayTreeView(homeDir);
        log.textProperty().bind(Logger.logDataProperty());
    }

    @FXML
    private void runServer() {
        if (portIsValid()) {
            if (ftpServerInstance == null) {
                ftpServerInstance = new FTPServer();
                new Thread(() -> ftpServerInstance.createServerState(Integer.parseInt(serverPort.getText()))).start();
            }else {
                FxDialogs.showError("Error", "Server is already running");
            }
        } else {
            FxDialogs.showError("Error", "Port format is not valid");
        }
    }

    @FXML
    private Boolean portIsValid() {
        Pattern portPattern = Pattern.compile("[0-9]{3,5}");
        return portPattern.matcher(serverPort.getText()).matches();
    }

}
