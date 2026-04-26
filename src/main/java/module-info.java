module com.example.birdgame {
    requires javafx.controls;
    requires java.prefs;
    requires java.net.http;
    requires java.logging;
    requires javafx.media;
    requires hid4java;
    requires com.sun.jna;

    opens com.example.birdgame3 to javafx.graphics, com.sun.jna;
}
