package lk.ijse.chat_application_shocket.Client;



import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ChatClient {
    @FXML
    private ListView<Object> messageView;

    @FXML
    private Button btnImage;

    @FXML
    private TextField txtMessage;

    @FXML
    private Button btnSend;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private boolean nameAccepted = false;

    public void initialize() {

        messageView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String) {
                    setText((String) item);
                    setGraphic(null);
                } else if (item instanceof Image) {
                    ImageView imageView = new ImageView((Image) item);
                    imageView.setFitHeight(100);
                    imageView.setFitWidth(100);
                    imageView.setPreserveRatio(true);
                    setGraphic(imageView);
                    setText(null);
                }
            }
        });


        try {
            Socket socket = new Socket("localhost", 5000); // must match server port
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(this::listenForMessages);
            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
            appendStatus("Unable to connect to server: " + e.getMessage());
        }
    }

    private void promptForName() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your name");
            dialog.setHeaderText("Please enter your name");

            dialog.showAndWait().ifPresent(name -> {
                clientName = name.trim();
                if (clientName.isEmpty()) {
                    appendStatus("Name cannot be empty, try again");
                    promptForName();
                } else {
                    try {
                        out.writeObject(clientName);
                        out.flush();
                    } catch (IOException e) {
                        appendStatus("Error sending name, try again");
                    }
                }
            });

            if (dialog.getResult() == null || dialog.getResult().isEmpty()) {
                Platform.exit();
            }
        });
    }

    private void listenForMessages() {
        try {
            while (true) {
                Object message = in.readObject();
                if (message == null) break;

                if (message instanceof String) {
                    String text = (String) message;

                    if (text.startsWith("SUBMITNAME") && !nameAccepted) {
                        promptForName();
                    } else if (text.startsWith("NAMEACCEPTED")) {
                        nameAccepted = true;
                        appendStatus("Connected as " + clientName);
                    } else if (text.startsWith("TEXT")) {
                        String content = text.substring(5);
                        if (content.startsWith(clientName + ":")) {
                            appendStatus("You: " + content.substring(clientName.length() + 1).trim());
                        } else {
                            appendStatus(content);
                        }
                    } else if (text.startsWith("TIME")) {
                        appendStatus("Time: " + text.substring(5));
                    } else if (text.startsWith("DATE")) {
                        appendStatus("Date: " + text.substring(5));
                    } else if (text.startsWith("IMAGE")) {
                        byte[] imageData = (byte[]) in.readObject();
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        appendStatus(text.substring(6) + " sent an image");
                        Platform.runLater(() -> messageView.getItems().add(image));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            appendStatus("Disconnected : "+ e.getMessage());
        } finally {
            closeConnection();
        }
    }
    //conaction close

    public void closeConnection() {
        try {
            if (out != null) out.close();
            if (in  != null)  in .close();
        } catch (IOException e) {
            appendStatus("Unable to close connection");
        }
    }

    private void appendStatus(String message) {
        Platform.runLater(() -> messageView.getItems().add(message));
    }

    // Massege send button

    @FXML
    void btnSendOnAction() {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;
        try {
            out.writeObject(message);
            out.flush();
            txtMessage.clear();
        } catch (IOException e) {
            appendStatus("Unable to send message!");
        }
    }

    //Add images and files

    @FXML
    void btnImageOnAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] imageData = Files.readAllBytes(file.toPath());
                out.writeObject(imageData);
                out.flush();
            } catch (IOException e) {
                appendStatus("Error sending image, try again");
            }
        }
    }

    //client server disconnected

    public void btnDisconnectedOnAction(ActionEvent actionEvent) {
        closeConnection();

    }
}
