module com.example.birdgame3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires javafx.media;


    opens com.example.birdgame3 to javafx.fxml;
    exports com.example.birdgame3;
}