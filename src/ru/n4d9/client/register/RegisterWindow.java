package ru.n4d9.client.register;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.n4d9.Message;
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

public class RegisterWindow implements Window {
    private Stage stage;
    private RegisterListener registerListener;
    private Receiver receiver;
    private ReceiverListener listener;
    private static final int SENDING_PORT = 6666;


    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private Button registerButton;

    public RegisterWindow(Receiver receiver, ReceiverListener listener, RegisterListener l) {
        registerListener = l;
        this.listener = listener;
        this.receiver = receiver;
        stage = new Stage();
        loadView();
        stage.show();
    }

    private void onMessageReceived(Message m) {
        switch (m.getText()) {
            case "OK":
                registerListener.onRegister();
                stage.close();
                break;

            case "ALREADY_REGISTERED": {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Аккаунт с таким e-mail уже зарегистрирован.");
                alert.show();
                break;
            }
            case "WRONG_EMAIL": {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Вы указали некорректный e-mail.");
                alert.show();
                break;
            }
            case "INTERNAL_ERROR": {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Произошла внутренняя ошибка сервера.");
                alert.show();
                break;
            }
        }
    }

    @Override
    public void loadView() {
        FXMLLoader loader = new FXMLLoader();
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
        stage.hide();

        nameField.setDisable(true);
        emailField.setDisable(true);
        registerButton.setDisable(true);

//        try {
        String email = emailField.getText();
        String name = nameField.getText();

        if (email.isEmpty()) {
            showErrorMessage("Не указан e-mail.");
            nameField.clear();
            return;
        }

        if (name.isEmpty()) {
            showErrorMessage("Не указано имя.");
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

    private void send(String s, Serializable serializable) {
        try {
            Sender.send(new Message(s, serializable).serialize(), InetAddress.getByName("localhost"), SENDING_PORT, true, new SenderAdapter(){
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
            e.printStackTrace(); // todo
        }
    }

}
