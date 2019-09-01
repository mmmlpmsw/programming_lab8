package ru.n4d9.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.StringEntity;
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
import java.util.*;

public class MainWindow extends Application implements Window {

    private static Locale currentLocale = Locale.getDefault();
    private static HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<>();

    private Stage stage;
    private static final int SENDING_PORT = 6666;
    private static Receiver receiver;
    private static int id;
    private static String login, password, username;
    private static ArrayList<Room> rooms;
    private ResourceBundle bundle = Client.currentResourceBundle();

    static String autofillLogin = "", autofillPassword = "";

    @FXML private Label userNameLabel;
    @FXML private TabPane tabPane;
    @FXML private GridPane gridPane;
    @FXML private RoomsTable roomsTable;
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

        stage.hide();
        promptLogin();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadView(){
        ObservableList<Room> roomObservableList = null;

        if (roomsTable != null) {
            roomObservableList = roomsTable.getItems();
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(Client.currentResourceBundle());
            loader.setController(this);
            Parent root = loader.load(getClass().getResourceAsStream("/layout/main.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(root));
            userNameLabel.setText(username);

            stage.setMinWidth(600);
            stage.setMinHeight(400);

            stage.setOnCloseRequest((e) -> {
                send("disconnect");
                System.exit(0);
            });

            if (roomObservableList != null) {
                roomsTable.setItems(roomObservableList);
            }

            roomPropertiesPane.setApplyingListener(model -> send("modify", model));

            roomsTable.setVisible(true);
            tableTab.getTabPane().heightProperty().addListener((observable, oldValue, newValue) -> roomsTable.setPrefHeight(tableTab.getTabPane().getHeight()));

            addButton.setRoomAddListener(MainWindow::addRoom);
            addButton.setParent(stage);

            importButton.setRoomImportListener(MainWindow::importRoom);
            loadButton.setRoomImportListener(MainWindow::loadRoom);

            roomsCanvas.clearProxy();
            roomsCanvas.setTarget(roomsTable.getItems());

            roomsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> roomsCanvas.selectRoom((Room)newValue));
            roomsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    roomPropertiesPane.selectCreature((Room)newValue, newValue != null && ((Room) newValue).getOwnerId() == id));
            roomsCanvas.widthProperty().bind(canvasTab.getTabPane().widthProperty());
            roomsCanvas.heightProperty().bind(canvasTab.getTabPane().heightProperty());

            roomsCanvas.setSelectingListener((m) -> {
                if (m != null)
                    roomsTable.getSelectionModel().select(m);
                else
                    roomsTable.getSelectionModel().clearSelection();
            });

            roomPropertiesPane.setDeletingListener(roomId -> send("remove", roomId));
            roomPropertiesPane.setRemovingGreaterListener(model -> send("remove_greater", model));
            roomPropertiesPane.setRemovingLowerListener(model -> send("remove_lower", model));
            roomPropertiesPane.selectCreature(null, false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setParameters(int id, String username, String login, String password) {
        this.id = id;
        this.login = login;
        this.username = username;
        this.password = password;
    }

    private void promptLogin() {
        new LoginWindow((id, username, login, password, rooms, color) -> {
            Platform.runLater(() -> {
                stage.show();
                setParameters(id, username, login, password);
                userNameLabel.setText(username);
                //TODO color
                roomsCanvas.getUserColors().put(id, Color.valueOf(color));
                roomsTable.getItems().clear();
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
    private void proccessMessage(Message message) {
//        System.out.println(message.getText());
        switch (message.getText()){

            case "room_added": {
                roomsTable.getItems().add((Room)message.getAttachment());
                break;
            }

            case "room_removed": {
                Room room = (Room) message.getAttachment();
                roomsTable.getItems().remove(room);
                break;
            }

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

            case "room_modified": {
//                Platform.runLater(() -> {
                Room model = (Room) message.getAttachment();
                ObservableList<Room> items = roomsTable.getItems();
                for (int i = 0; i < items.size(); i ++) {
                    if (items.get(i).getId() == model.getId()) {
                        roomsTable.getItems().get(i).setFromRoomModel((Room)message.getAttachment());
                        Platform.runLater(() -> {
                            roomsTable.getColumns().get(0).setVisible(false);
                            roomsTable.getColumns().get(0).setVisible(true);
                        });
//                            roomsTable.getItems().set(i, (Room)message.getAttachment()); //не меняет канвас
//

                        roomsTable.getSelectionModel().select(i);
                        break;
                    }
                }
//                });

//                Room model = (Room) message.getAttachment();
//                ObservableList<Room> items = roomsTable.getItems();
//                for (int i = 0; i < items.size(); i ++) {
//                    if (items.get(i).getId() == model.getId()) {
//                        roomsTable.getItems().get(i).setFromRoomModel(model);
//                        Platform.runLater(() -> {
//                            roomsTable.getColumns().get(0).setVisible(false);
//                            roomsTable.getColumns().get(0).setVisible(true);
//                        });
//                        roomsTable.getSelectionModel().select(i);
//                        break;
//                    }
//                }
                break;

            }

            case "collection_state": {
                ArrayList<Room> rooms = (ArrayList<Room>) message.getAttachment();
                ObservableList<Room> items = roomsTable.getItems();
                for (Room r : rooms) {
                    for (int i = 0; i < items.size(); i ++) {
                        if (items.get(i).getId() == r.getId()) {
                            Room target = roomsTable.getItems().get(i);
                            target.setFromRoomModel(r);
                            target.setX(r.getX());
                            target.setY(r.getY());
                            target.setWidth(r.getWidth());
                            target.setHeight(r.getHeight());
                            target.setName(r.getName());
                            target.setRotation(r.getRotation());
                            break;
                        }
                    }

                }
//                Platform.runLater(() -> {
//                    roomsTable.getColumns().get(0).setVisible(false);
//                    roomsTable.getColumns().get(0).setVisible(true);
//                });
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

    public static void send(String s) {
        send(s, null);
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
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText(message);
                        alert.show();});

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
        send("disconnect");
        stage.close();
        promptLogin();
    }

}
