package lk.ijse.chat_application_shocket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatLauncher extends Application {

    //Initializer
    @Override
    public void start(Stage stage) throws Exception {
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/chat_server.fxml"))));
        stage.setTitle("Chat Room");
        stage.centerOnScreen();
        stage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }

}
