package ru.n4d9.client.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.n4d9.client.register.RegisterWindow;

import java.io.IOException;

public class LoginWindow {
    private Stage stage;
    private LoginListener loginListener;

    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton, registerButton;
    private Label label;

    public LoginWindow(LoginListener listener) throws IOException {
        this.loginListener = listener;
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);
        Parent root = loader.load(getClass().getResourceAsStream("/layout/login.fxml"));
        stage = new Stage();
        stage.setScene(new Scene( root));
        stage.show();
    }

    @FXML
    public void onRegisterClick() {
        stage.hide();
        new RegisterWindow(() -> stage.show());
    }


}
