package ru.n4d9.client;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import ru.n4d9.client.login.LoginWindow;

import java.io.IOException;


public class MainWindow extends Application {
    private Stage stage;

    public MainWindow() {

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Button button = new Button("test");
        button.setVisible(true);
////        primaryStage.show();
//        Button login = new Button("Login");
//        Button browse = new Button("Browse");
//        browse.setVisible(false);
//
//        login.setOnAction(e -> browse.setVisible(true));
//
//        VBox root = new VBox(14, login, browse);
//        root.setAlignment(Pos.BOTTOM_LEFT);
//        Scene scene = new Scene(root, 300, 275);
//        primaryStage.setScene(scene);
//        primaryStage.show();


        stage = primaryStage;
        stage.hide();
        promptLogin(stage);
    }

    private static void promptLogin(Stage stage)throws IOException {
       new LoginWindow(stage::show);
    }
}
