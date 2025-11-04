package lk.ijse.chat_application_shocket.server;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import lk.ijse.chat_application_shocket.Client.ChatClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;

public class ChatServer {

    private static final int PORT = 5000;
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private boolean isRunning = false;
    private ServerSocket serverSocket;

    @FXML
    private TextArea txtServerStatus;
    @FXML
    private Button addClientBtn;

    @FXML
    public void initialize() {
        appendStatus("Server Started on port " + PORT);
    }

    private void appendStatus(String message) {
        Platform.runLater(() -> txtServerStatus.appendText(message + "\n"));
    }

    @FXML
    public void btnAddClientOnAction(ActionEvent actionEvent) {
        if (!isRunning) {
            startServer();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        openClientWindow();
    }

    //Server Start

    private void startServer() {
        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendStatus("Server started on port " + PORT);

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    appendStatus("New client connected");

                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }

            } catch (IOException e) {
                appendStatus("Server Error: " + e.getMessage());
            }
        }).start();
    }

    //open client windows

    private void openClientWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_client.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);

                Stage stage = new Stage();
                stage.setTitle("Chat Client");
                stage.setScene(scene);
                stage.setMinHeight(610);
                stage.setMinWidth(700);

                stage.centerOnScreen();
                stage.toFront();
                stage.requestFocus();

                ChatClient controller = loader.getController();
                stage.setOnCloseRequest(event -> controller.closeConnection());

                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    //client Handalng

    private class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        //run section
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());


                while (true) {
                    out.writeObject("SUBMITNAME");
                    clientName = (String) in.readObject();
                    if (clientName != null && !clientName.trim().isEmpty()) {
                        break;
                    }
                    appendStatus("Invalid name from client, requesting again");
                }

                out.writeObject("NAMEACCEPTED");
                appendStatus("Client " + clientName + " connected");

                broadcast("TEXT " + clientName + " joined the chat");

                synchronized (writers) {
                    writers.add(out);
                }


                while (true) {
                    Object message = in.readObject();
                    if (message == null) break;

                    if (message instanceof String) {
                        String text = (String) message;


                        if (text.equalsIgnoreCase("TIME")) {
                            String time = LocalTime.now().toString();
                            broadcast("TIME " + time);
                        }

                        else if (text.equalsIgnoreCase("DATE")) {
                            String date = LocalDate.now().toString();
                            broadcast("DATE " + date);
                        }
                        // Regular chat text
                        else {
                            broadcast("TEXT " + clientName + ": " + text);
                        }
                    }

                    else if (message instanceof byte[]) {
                        broadcastImage("IMAGE " + clientName, (byte[]) message);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                appendStatus("Client " + clientName + " disconnected unexpectedly");
            } finally {
                if (clientName != null) {
                    appendStatus("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat");
                }

                synchronized (writers) {
                    writers.remove(out);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    appendStatus("Error closing client socket: " + e.getMessage());
                }
            }
        }

        //broadcast

        private void broadcast(String message) {
            synchronized (writers) {
                for (ObjectOutputStream writer : writers) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (IOException e) {
                        appendStatus("Error broadcasting message...");
                    }
                }
            }
        }

        private void broadcastImage(String header, byte[] imageData) {
            synchronized (writers) {
                for (ObjectOutputStream writer : writers) {
                    try {
                        writer.writeObject(header);
                        writer.writeObject(imageData);
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
