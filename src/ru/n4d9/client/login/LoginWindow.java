package ru.n4d9.client.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.n4d9.Message;
import ru.n4d9.client.Client;
import ru.n4d9.client.Window;
import ru.n4d9.client.register.RegisterListener;
import ru.n4d9.client.register.RegisterWindow;
import ru.n4d9.client.settings.SettingsDialog;
import ru.n4d9.transmitter.Receiver;
import ru.n4d9.transmitter.ReceiverListener;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Properties;

public class LoginWindow implements Window {

    private static final int SENDING_PORT = 6666;

    private Stage stage;
    private LoginListener loginListener;

    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton, registerButton;
    private Label label;

    private Receiver receiver;

    private ReceiverListener receiverListener = new ReceiverListener() {
        @Override
        public void received(int requestID, byte[] data, InetAddress address, int port) {
            try {
                Message message = Message.deserialize(data);
                onMessageReceived(message);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace(); // todo
            }
        }

        @Override
        public void exceptionThrown(Exception e) {
            e.printStackTrace(); // todo
        }
    };

    public LoginWindow(LoginListener listener, Receiver r) {
        receiver = r;
        receiver.setListener(receiverListener);
      //  send("unsubscribe", null);
        this.loginListener = listener;
        stage = new Stage();
        stage.setResizable(false);
        loadView();
        stage.show();
    }

    private void onMessageReceived(Message m) {
        switch (m.getText()) {
            case "OK":
                loginListener.onLogin(m.getUserid(), m.getLogin(), m.getPassword());
                stage.close();
                break;
            case "WRONG": {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Неправильная пара логин-пароль.");
                alert.show();
                break;
            }
            case "INTERNAL_ERROR":
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Произошла внутренняя ошибка сервера.");
                alert.show();
                break;
        }
    }

    @Override
    public void loadView() {

//        ResourceBundle bundle = Client.currentResourceBundle();
        String emailTextBackup = "";
        String passwordTextBackup = "";

        if (emailField != null)
            emailTextBackup = emailField.getText();
        if (passwordField != null)
            passwordTextBackup = passwordField.getText();

        try {
            FXMLLoader loader = new FXMLLoader();
//            loader.setResources(bundle);
            loader.setController(this);
            Pane root = loader.load(getClass().getResourceAsStream("/layout/login.fxml"));
//            stage.setTitle(bundle.getString("login-dialog.window-title"));
            stage.setScene(new Scene(root));

            emailField = (TextField) stage.getScene().getRoot().lookup("#email-input");
            passwordField = (PasswordField) stage.getScene().getRoot().lookup("#password-input");
            loginButton = (Button) stage.getScene().getRoot().lookup("#login-button");
            registerButton = (Button) stage.getScene().getRoot().lookup("#register-button");


            emailField.setText(emailTextBackup);
            passwordField.setText(passwordTextBackup);

            emailField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
//        alert.setTitle(bundle.getString("login-dialog.error-alert-title"));
//        alert.setHeaderText(bundle.getString("login-dialog.error-alert-header"));
            alert.setContentText(e.toString());
            alert.show();
        }
    }


    @FXML
    public void onRegisterClick() {
        stage.hide();
        new RegisterWindow(receiver, receiverListener, new RegisterListener() {
            @Override
            public void onRegister() {
                 stage.show();
            }
        });
    }

    @FXML
    public void onLoginClick() {
        loginButton.setDisable(true);
        emailField.setDisable(true);
        passwordField.setDisable(true);

//        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty()) {
                showErrorMessage("Не указан e-mail.");
                passwordField.clear();
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);
                return;
            }

            if (password.isEmpty()) {
                showErrorMessage("Не указан пароль.");
                emailField.clear();
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);
                return;
            }

            Properties loginInfo = new Properties();
            loginInfo.setProperty("email", email);
            loginInfo.setProperty("password", password);

            send("login", loginInfo);

//            Message response = (Message)(ois.readObject());
//
//            Properties properties = (Properties)response.getAttachment();
//            String status = response.getText();
//
//            switch (status) {
//                case "OK":
//                    listener.onLogin(
//                            Integer.parseInt(properties.getProperty("userid")),
//                            Integer.parseInt(properties.getProperty("user_token")),
//                            properties.getProperty("user_name"),
//                            Color.valueOf(properties.getProperty("user_color")),
//                            socket,
//                            ois, oos
//                    );
//                    stage.close();
//                    break;
//
//                case "WRONG":
//                    passwordField.clear();
//                    showErrorMessage(Client.currentResourceBundle().getString("login-dialog.wrong-email-or-password"));
//                    break;
//
//                case "INTERNAL_ERROR":
//                    passwordField.clear();
//                    showErrorMessage(Client.currentResourceBundle().getString("login-dialog.internal-server-error"));
//                    break;
//            }
//
//        } catch (UnknownHostException e) {
////            showErrorMessage(Client.currentResourceBundle().getString("login-dialog.unknown-host"));
//        } catch (IOException e) {
//            if (e.getMessage().equals("Connection refused: connect"))
////                showErrorMessage(Client.currentResourceBundle().getString("login-dialog.no-connection"));
//            else
//                showErrorMessage(Client.currentResourceBundle().getString("login-dialog.cant-connect-to-server") + '\n' + e.toString());
//        } catch (ClassNotFoundException e) {
//            showErrorMessage(Client.currentResourceBundle().getString("login-dialog.class-not-found") + '\n' + e.toString());
//        } finally {
//            loginButton.setDisable(false);
//            emailField.setDisable(false);
//            passwordField.setDisable(false);
//        }
    }

    private void send(String s, Serializable serializable) {
        try {
            Sender.send(new Message(s, serializable).serialize(), InetAddress.getByName("localhost"), SENDING_PORT, true, new SenderAdapter(){
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
            e.printStackTrace(); // todo
        }
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
}
