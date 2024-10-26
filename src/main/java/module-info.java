module org.example.posystem {
    requires java.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.naming;
    requires org.xerial.sqlitejdbc;  // Add this for SQLite
    requires java.desktop; // Required for printing capabilities
    requires escpos.coffee;

    opens javafx to javafx.fxml;
    opens javafx.controller to javafx.fxml;
    exports javafx;
    exports javafx.controller;
    exports javafx.model;
    exports javafx.utils;
    exports database;  // Add this to allow access to Database class
}
