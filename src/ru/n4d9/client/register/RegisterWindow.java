package ru.n4d9.client.register;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.n4d9.Utils.Message;
import ru.n4d9.client.Client;
import ru.n4d9.client.Window;
import ru.n4d9.client.settings.SettingsDialog;
import ru.n4d9.transmitter.Receiver;
import ru.n4d9.transmitter.ReceiverListener;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Properties;
import java.util.ResourceBundle;

public class RegisterWindow implements Window {

    ResourceBundle bundle = Client.currentResourceBundle();
    private Stage stage;
    private RegisterListener registerListener;
    private Receiver receiver;
    private static final int SENDING_PORT = 6666;


    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private Button registerButton;
    @FXML private Hyperlink cancelLink;

    public RegisterWindow(Receiver receiver, RegisterListener l) {
        registerListener = l;
        this.receiver = receiver;
        stage = new Stage();
        loadView();
        stage.show();
        stage.setOnCloseRequest(e -> registerListener.onRegister());
        receiver.setListener(new ReceiverListener() {
            @Override
            public void received(int requestID, byte[] data, InetAddress address, int port) {
                Thread outer = Thread.currentThread();
                try {
                    Message message = Message.deserialize(data);
                    onMessageReceived(message);
                    Thread.sleep(30);
                    outer.interrupt();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace(); // todo handling
                } catch (InterruptedException ignored) {
                }
            }

            @Override
            public void exceptionThrown(Exception e) {
                e.printStackTrace(); // todo handling
            }
        });
    }

    private void onMessageReceived(Message m) {
        switch (m.getText()) {
            case "OK_REGISTER":
                Platform.runLater(() -> {
                    registerListener.onRegister();
                    stage.close();
                });
                break;

            case "ALREADY_REGISTERED": {
                setDisable(false);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText(bundle.getString("register.alert.already-registered"));
                    alert.show();
                });
                break;
            }
            case "WRONG_EMAIL": {
                setDisable(false);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("register.alert.incorrect-email"));
                    alert.show();
                });
               //showErrorMessage(bundle.getString("register.alert.incorrect-email"));
                break;
            }
            case "INTERNAL_ERROR": {
                setDisable(false);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("alert.internal-error"));
                    alert.show();
                });
               break;
            }
        }
    }

    @Override
    public void loadView() {
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(bundle);
        loader.setController(this);

        String emailTextBackup = "";
        String nameTextBackup = "";

        if (emailField != null)
            emailTextBackup = emailField.getText();
        if (nameField != null)
            nameTextBackup = nameField.getText();

        try {
            Parent root = loader.load(getClass().getResourceAsStream("/layout/register.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(root));

            emailField.setText(emailTextBackup);
            nameField.setText(nameTextBackup);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSendClick(){

        setDisable(true);

        String email = emailField.getText();
        String name = nameField.getText();

        if (email.isEmpty()) {
            showErrorMessage(bundle.getString("error-message.no-email"));
            nameField.clear();
            return;
        }

        if (name.isEmpty()) {
            showErrorMessage(bundle.getString("register.alert.no-name"));
            emailField.clear();
            return;
        }
        Properties registerInfo = new Properties();
        registerInfo.setProperty("email", email);
        registerInfo.setProperty("name", name);

        send("register", registerInfo);

    }

    @FXML
    public void onSettingsClicked() {
        new SettingsDialog((changed) -> {
            if (changed)
                loadView();
            stage.show();
        });
        stage.hide();
    }

    @FXML
    public void onCancelClicked() {
        registerListener.onRegister();
    }

    private void send(String s, Serializable serializable) {
        try {
            Message message = new Message(s, serializable);
            message.setSourcePort(receiver.getLocalPort());
            Sender.send(message.serialize(), InetAddress.getByName("localhost"), SENDING_PORT, true, new SenderAdapter(){
                @Override
                public void onSuccess() {}

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

    private void setDisable (boolean b) {
        nameField.setDisable(b);
        emailField.setDisable(b);
        registerButton.setDisable(b);
    }

}