package ru.n4d9.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.buttons.add.AddRoomButton;
import ru.n4d9.client.buttons.imp.ImportRoomButton;
import ru.n4d9.client.buttons.load.LoadRoomButton;
import ru.n4d9.client.canvas.RoomsCanvas;
import ru.n4d9.client.login.LoginWindow;
import ru.n4d9.client.settings.SettingsDialog;
import ru.n4d9.server.FileLoader;
import ru.n4d9.transmitter.Receiver;
import ru.n4d9.transmitter.ReceiverListener;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainWindow extends Application {

    private static Locale currentLocale = Locale.getDefault();
    private static HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<>();

    private Stage stage;
    private static final int SENDING_PORT = 6666;
    private static Receiver receiver;
    private static int id;
    private static String login, password;
    private static ArrayList<Room> rooms;
    private ResourceBundle bundle;

    private static String autofillLogin = "", autofillPassword = "";

    @FXML private Label userNameLabel;
    @FXML private TabPane tabPane;
    @FXML private GridPane gridPane;
    @FXML private TableView roomsTable;
    @FXML private Tab tableTab;
    @FXML private Tab canvasTab;
    @FXML private AddRoomButton addButton;
    @FXML private ImportRoomButton importButton;
    @FXML private LoadRoomButton loadButton;
    @FXML private RoomsCanvas roomsCanvas;
    @FXML private RoomPropertiesPane roomPropertiesPane;

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

    public static void main(String[] args) {
        if (args.length == 2) {
            autofillLogin = args[0];
            autofillPassword = args[1];
        }
        Application.launch(MainWindow.class);
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        stage = primaryStage;
        initResourceBundles();
        loadView();

        try {
            receiver = new Receiver(true);
            receiver.startListening();
        } catch (IOException e) {
            System.exit(0);
        }

        stage.hide();
        promptLogin();
    }

    @SuppressWarnings("unchecked")
    public void loadView(){
        bundle = MainWindow.currentResourceBundle();
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(bundle);
        loader.setController(this);

        try {
            Parent root = loader.load(getClass().getResourceAsStream("/layout/main.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(root));
            roomsTable.setVisible(true);
            tableTab.getTabPane().heightProperty().addListener((observable, oldValue, newValue) -> roomsTable.setPrefHeight(tableTab.getTabPane().getHeight()));
            addButton.setRoomAddListener(MainWindow::addRoom);
            addButton.setParent(stage);

            roomsCanvas.clearProxy();

            importButton.setRoomImportListener(MainWindow::importRoom);
            loadButton.setRoomImportListener(MainWindow::loadRoom);

            roomsCanvas.setTarget(roomsTable.getItems());
            roomsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> roomsCanvas.selectRoom((Room)newValue));
            roomsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> roomPropertiesPane.selectCreature((Room)newValue, newValue != null && ((Room) newValue).getOwnerId() == id));
            roomsCanvas.widthProperty().bind(canvasTab.getTabPane().widthProperty());
            roomsCanvas.heightProperty().bind(canvasTab.getTabPane().heightProperty());
            roomsCanvas.setSelectingListener((m) -> {
                if (m != null)
                    roomsTable.getSelectionModel().select(m);
                else
                    roomsTable.getSelectionModel().clearSelection();
            });
            roomPropertiesPane.selectCreature(null, false);
            roomPropertiesPane.setApplyingListener(model -> send("modify", model)); //todo
            roomPropertiesPane.setDeletingListener(roomId -> send("remove", roomId));
            roomPropertiesPane.setRemovingGreaterListener(model -> send("remove_greater", model));
            roomPropertiesPane.setRemovingLowerListener(model -> send("remove_lower", model));


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
        new LoginWindow((id, login, password, rooms) -> {
            Platform.runLater(() -> {
                stage.show();
                setParameters(id, login, password);
//                userNameLabel.setText(login);
                roomsTable.getItems().addAll(rooms);

            });
            receiver.setListener(new ReceiverListener() {
                public void received(int requestID, byte[] data, InetAddress address, int port) {
                    try {
                        proccessMessage(Message.deserialize(data));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace(); // todo handle
                    }
                }
                public void exceptionThrown(Exception e) {
                    e.printStackTrace(); // todo handle
                }
            });
            receiver.startListening();
        },
                autofillLogin,
                autofillPassword
        );
        autofillLogin = "";
        autofillPassword = "";

    }

    private static void addRoom(String name, double x, double y, double height, double width) {
        Room room = new Room(width, height, x, y, name);
        send("add", room);
    }

    private static void importRoom(String filename) {
        try {
            String content = FileLoader.getFileContent(filename);
            send("import", new StringEntity().set(content));
        } catch (IOException e) {
            System.out.println("Ошибка чтения с файла " + e.getMessage());
        }
    }

    private static void loadRoom(String filename) {
        send("load", new StringEntity().set(filename));
    }

    @SuppressWarnings("unchecked")
    private void proccessMessage(Message message) { //todo перерисовка
        System.out.println(message.getText());
        switch (message.getText()){
            case "room_added":
                roomsTable.getItems().add(message.getAttachment());
                break;
            case "room_removed":
                Room model = (Room) message.getAttachment();
                roomsTable.getItems().remove(model);
        //        updateCreaturesCountText();
                break;

            case "rooms_removed": {
                ArrayList<Room> rooms = (ArrayList<Room>) message.getAttachment();
                roomsTable.getItems().removeAll(rooms);
                break;
            }
            case "rooms_import": {
                ArrayList<Room> rooms = (ArrayList<Room>) message.getAttachment();
                roomsTable.getItems().addAll(rooms);
                break;
            }
            case "INTERNAL_ERROR":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("alert.internal-error"));
                    alert.show();
                });
                break;
            case "WRONG":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("alert.incorrect-data-format"));
                    alert.show();
                });
                break;

        }

    }

    public static void send(String s, Serializable serializable) {
        try {
            Message message = new Message(s, serializable);
            message.setSourcePort(receiver.getLocalPort());
            message.setUserid(id);
            message.setLogin(login);
            message.setPassword(password);
            Sender.send(message.serialize(), InetAddress.getByName("localhost"), SENDING_PORT, true, new SenderAdapter(){
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError(String message) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(message);
                    alert.show();

                }
            });
        } catch (IOException e) {
            e.printStackTrace(); // todo handling
        }
    }

    public static Receiver getReceiver() {
        return receiver;
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
        send("disconnect", null);
        stage.close();
        promptLogin();
    }


    private static void initResourceBundles() {
        resourceBundles.clear();
        resourceBundles.put(
                currentLocale,
                ResourceBundle.getBundle("i18n/text", currentLocale)
        );
        resourceBundles.put(
                new Locale("en", "US"),
                ResourceBundle.getBundle("i18n/text", new Locale("en", "US"))
        );
        resourceBundles.put(
                new Locale("es", "NI"),
                ResourceBundle.getBundle("i18n/text", new Locale("es", "NI"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("et", "EE"),
                ResourceBundle.getBundle("i18n/text", new Locale("et", "EE"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("fr", "FR"),
                ResourceBundle.getBundle("i18n/text", new Locale("fr", "FR"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("ru", "RU"),
                ResourceBundle.getBundle("i18n/text", new Locale("ru", "RU"), new UTF8BundleControl())
        );
    }

    public static HashMap<Locale, ResourceBundle> getResourceBundles() {
        return resourceBundles;
    }

    public static ResourceBundle currentResourceBundle() {
        return resourceBundles.get(currentLocale);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setCurrentLocale(Locale currentLocale) {
        MainWindow.currentLocale = currentLocale;
    }
}
