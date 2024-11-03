module org.example.posystem {
    requires java.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.naming;
    requires org.xerial.sqlitejdbc;
    requires java.desktop;
    requires escpos.coffee;

    // Open more packages to allow reflection access
    opens javafx to javafx.fxml, javafx.graphics;
    opens javafx.controller to javafx.fxml, javafx.graphics;
    opens javafx.model to javafx.base;
    opens database to java.base;

    // Explicit exports
    exports javafx;
    exports javafx.controller;
    exports javafx.model;
    exports javafx.utils;
    exports database;
}