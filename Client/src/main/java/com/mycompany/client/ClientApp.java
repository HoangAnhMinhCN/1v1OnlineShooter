package com.mycompany.client;

import com.mycompany.client.controller.SceneController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class ClientApp extends Application {
    private Client client;

    @Override
    public void start(Stage stage) {
        client = new Client();
        client.setConnectionCallback(new ConnectionCallback() {
            @Override
            public void onConnectSuccess() {
                SceneController.getInstance().init(stage, client);
                SceneController.getInstance().showLoginUI();
            }

            @Override
            public void onConnectFailure(String errorMessage) {
                // Hiển thị thông báo lỗi lên màn hình cho người dùng
                showErrorAlert(errorMessage);
            }
        });

        client.connect(); // lưu ý xử lý trường hợp mất kết nối mạng (hiện tại chưa xử lý)
    }

    public void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch();
    }

}