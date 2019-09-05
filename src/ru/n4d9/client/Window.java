package ru.n4d9.client;

import javafx.scene.control.Alert;

public interface Window {

    default void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    void loadView();


}
