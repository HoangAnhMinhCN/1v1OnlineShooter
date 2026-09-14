package com.mycompany.server;

import java.sql.Connection;
import java.sql.DriverManager;

public class DataBaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/server_db";
    private static final String USER = "root";
    private static final String PASSWORD = "123456789";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

}
