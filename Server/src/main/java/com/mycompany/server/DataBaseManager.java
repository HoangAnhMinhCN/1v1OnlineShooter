package com.mycompany.server;

import java.sql.Connection;
import java.sql.DriverManager;

public class DataBaseManager {
    private static final String URL =
            "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/server_db";
    private static final String USER = "zcwFYPLk7RCg8CJ.root";
    private static final String PASSWORD = "kIYKKp2bCqIxdYOI";

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
