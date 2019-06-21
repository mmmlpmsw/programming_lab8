package ru.n4d9.client.register;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterWindow {
    private Stage stage;
    private RegisterListener listener;
    private TextField nameField, emailField;

    public RegisterWindow(RegisterListener l) {
        listener = l;
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);

        try {
            Parent root = loader.load(getClass().getResourceAsStream("/layout/register.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSendClick(){
        stage.hide();

    }

}
