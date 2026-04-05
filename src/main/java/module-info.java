module com.example.birdgame3 {
    requires javafx.controls;
    requires java.prefs;
    requires javafx.media;
    requires hid4java;
    requires com.sun.jna;

    opens com.example.birdgame3 to javafx.graphics, com.sun.jna;
}
