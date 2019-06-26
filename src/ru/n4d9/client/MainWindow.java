package ru.n4d9.client;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.n4d9.client.login.LoginListener;
import ru.n4d9.client.login.LoginWindow;
import ru.n4d9.client.settings.SettingsDialog;
import ru.n4d9.transmitter.Receiver;

import java.io.IOException;

public class MainWindow extends Application {
    private Stage stage;
    private static final int SENDING_PORT = 6666;
    private static Receiver receiver;
    private int id;
    private String login, password;

    @FXML private Label userNameLabel;
    @FXML private TabPane tabPane;
    @FXML private GridPane gridPane;
    @FXML private Button  importButton;
    @FXML private Button addButton, removeButton;
    @FXML private TableView roomsTable;
    @FXML private VBox addBox, importBox, removeBox;
    @FXML private VBox[] boxes = {addBox, importBox, removeBox};

    private static Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> {
        System.out.println(e.toString());
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("The program has crashed in thread " + t.getName());
        alert.setContentText(e.toString());
        alert.show();
    };

    public static Thread.UncaughtExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public MainWindow() {}

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        stage = primaryStage;
        loadView();

        try {
            receiver = new Receiver(true);
            receiver.startListening();
        } catch (IOException e) {
            System.exit(0);
        }
        stage.show();
//        stage.hide();
//        promptLogin();
    }

    public void loadView(){
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);

        try {
            Parent root = loader.load(getClass().getResourceAsStream("/layout/main.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(root));
            roomsTable.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setParameters(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    private void promptLogin() {
       new LoginWindow(new LoginListener() {
           @Override
           public void onLogin(int id, String login, String password) {
              stage.show();
              setParameters(id, login, password);
              userNameLabel.setText(login);
           }
       }, receiver);
    }

    @FXML
    public void onAddClicked(){
        try {
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] != null)
                    if (boxes[i].isVisible()) {
                        boxes[i].setVisible(false);
                    }
            }
            tabPane.setVisible(false);
            addBox.setVisible(true);
        } catch (NullPointerException ignored) {}
    }

    @FXML
    public void onImportClicked(){
        try {
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] != null)
                    if (boxes[i].isVisible()) {
                        boxes[i].setVisible(false);
                    }
            }
            tabPane.setVisible(false);
            importBox.setVisible(true);
        } catch (NullPointerException ignored) {}
    }

    @FXML
    public void onRemoveClicked(){
        try {
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] != null)
                    if (boxes[i].isVisible()) {
                        boxes[i].setVisible(false);
                    }
            }
            tabPane.setVisible(false);
            removeBox.setVisible(true);
        } catch (NullPointerException ignored) {}
    }

    @FXML
    public void onCancelClicked() {
        try {
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] != null)
                    if (boxes[i].isVisible()) {
                         boxes[i].setVisible(false);
                    }
            }
            tabPane.setVisible(true);
        } catch (NullPointerException ignored) {}
    }

    @FXML
    public void onSettingsClicked() {
        new SettingsDialog((changed) -> {
            if (changed) {
                loadView();
            }
            stage.show();
        });
        stage.hide();
    }

    @FXML
    public void onExitClicked() {
        stage.close();
        promptLogin();
    }
}
