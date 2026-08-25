module com.mycompany.client {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml; // Add line này để sử dụng FXMLLoader

    // Mở package chứa Controller và GameScene cho JavaFX FXML truy cập
    opens com.mycompany.client.controller to javafx.fxml;
    opens com.mycompany.client.game to javafx.fxml;
    
    exports com.mycompany.client;
}
