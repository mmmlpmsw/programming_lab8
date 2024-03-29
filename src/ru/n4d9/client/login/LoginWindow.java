package ru.n4d9.client.login;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.n4d9.Utils.Message;
import ru.n4d9.client.Client;
import ru.n4d9.client.MainWindow;
import ru.n4d9.client.Room;
import ru.n4d9.client.Window;
import ru.n4d9.client.register.RegisterWindow;
import ru.n4d9.client.settings.SettingsDialog;
import ru.n4d9.transmitter.ReceiverListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

public class LoginWindow implements Window {

    private Stage stage;
    private LoginListener loginListener;
    ResourceBundle bundle;

    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton, registerButton;

    private RegisterWindow registerWindow;
//
    /**
     * конструктор, использующийся при инициализации
     * @param listener
     * @param autofillLogin
     * @param autofillPassword
     */
    public LoginWindow(boolean isBegin, LoginListener listener, String autofillLogin, String autofillPassword) {
      //  send("unsubscribe", null);
        this.loginListener = listener;
        stage = new Stage();
        stage.setResizable(false);
        loadView();
        emailField.setText(autofillLogin);
        passwordField.setText(autofillPassword);
        if (isBegin) {
            stage.hide();
            promptRegister();
        } else stage.show();
    }

    private void onMessageReceived(Message m) {
        System.out.println("LoginWindow: "+ m.getText());
        switch (m.getText()) {
            case "OK":
                MainWindow.send("subscribe");
                loginListener.onLogin(m.getUserid(), m.getUsername(), m.getLogin(), m.getPassword(), (ArrayList<Room>)m.getAttachment(), m.getUserColor());
                Platform.runLater(() -> stage.close());
                break;
            case "WRONG": {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("login.alert.incorrect-login-pswd"));
                    alert.show();
                });
                break;
            }
            case "INTERNAL_ERROR":
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("alert.internal-error"));
                    alert.show();
                });
                break;


            case "ALREADY_REGISTERED": {
                Platform.runLater(() -> {
//                    registerWindow.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText(bundle.getString("register.alert.already-registered"));
                    alert.show();

                    registerWindow.setDisable(false);

                });

                break;
            }

            case "WRONG_EMAIL": {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(bundle.getString("register.alert.incorrect-email"));
                    alert.show();
                });
                registerWindow.setDisable(false);
                break;

            }

            case "OK_REGISTER":
                Platform.runLater(() -> {
                    registerWindow.getStage().close();
                    stage.show();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText(bundle.getString("register.alert.ok"));
                    alert.show();
                });
//                registerWindow.getRegisterListener().onRegister();

                break;
        }

    }

    private void promptRegister() {
        registerWindow = new RegisterWindow(() ->
            Platform.runLater(stage::show)
        );
            MainWindow.getReceiver().setListener(new ReceiverListener() {
                public void received(int requestID, byte[] data, InetAddress address, int port) {
                    try {
                        onMessageReceived(Message.deserialize(data));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }
                public void exceptionThrown(Exception e) {
                    e.printStackTrace(); // handle
                }
        });

    }

    @Override
    public void loadView() {
        bundle = Client.currentResourceBundle();
        String emailTextBackup = "";
        String passwordTextBackup = "";

        if (emailField != null)
            emailTextBackup = emailField.getText();
        if (passwordField != null)
            passwordTextBackup = passwordField.getText();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(bundle);
            loader.setController(this);
            Pane root = loader.load(getClass().getResourceAsStream("/layout/login.fxml"));
//            stage.setTitle(bundle.getString("login-dialog.window-title"));
            stage.setScene(new Scene(root));

            emailField = (TextField) stage.getScene().getRoot().lookup("#email-input");
            passwordField = (PasswordField) stage.getScene().getRoot().lookup("#password-input");
            loginButton = (Button) stage.getScene().getRoot().lookup("#login-button");
            registerButton = (Button) stage.getScene().getRoot().lookup("#register-button");

            stage.setOnCloseRequest(e -> System.exit(0));
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
    public void onLoginClick() {
        loginButton.setDisable(true);
        emailField.setDisable(true);
        passwordField.setDisable(true);

            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty()) {
                showErrorMessage(bundle.getString("error-message.no-email"));
                passwordField.clear();
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);
                return;
            }

            if (password.isEmpty()) {
                showErrorMessage(bundle.getString("register.alert.no-password"));
                emailField.clear();
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);
                return;
            }

            Properties loginInfo = new Properties();
            loginInfo.setProperty("email", email);
            loginInfo.setProperty("password", password);

            MainWindow.send("login", loginInfo);
            emailField.setDisable(false);
            passwordField.setDisable(false);
            loginButton.setDisable(false);
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

    public Stage getStage() {
        return stage;
    }

    public LoginListener getLoginListener() {
        return loginListener;
    }
}
