module com.example.birdgame3 {
    requires javafx.controls;
    requires java.prefs;
    requires javafx.media;

    opens com.example.birdgame3 to javafx.graphics;
}
