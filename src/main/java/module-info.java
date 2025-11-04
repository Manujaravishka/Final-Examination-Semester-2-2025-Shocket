module lk.ijse.chat_application_shocket {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;


    opens lk.ijse.chat_application_shocket.Client to javafx.fxml;
    opens lk.ijse.chat_application_shocket.server to javafx.fxml;
    exports lk.ijse.chat_application_shocket;
}



