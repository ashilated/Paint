module com.ash.paint {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;


    opens dev.ashilated.paint to javafx.fxml;
    exports dev.ashilated.paint;
}