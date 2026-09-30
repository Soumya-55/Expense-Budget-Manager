package com.expense;

import java.sql.*;

/** JDBC connection helper + automatic database/table creation. */
public class Db {
    private static final String HOST = Config.get("db.host", "localhost");
    private static final String PORT = Config.get("db.port", "3306");
    private static final String NAME = Config.get("db.name", "expense_manager");
    private static final String USER = Config.get("db.user", "root");
    private static final String PASS = Config.get("db.password", "");
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + NAME + OPTS, USER, PASS);
    }

    public static void init() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        // 1) create database if missing
        try (Connection c = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTS, USER, PASS);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + NAME + "`");
        }
        // 2) create tables if missing
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "type ENUM('INCOME','EXPENSE') NOT NULL,"
                + "amount DECIMAL(12,2) NOT NULL,"
                + "category VARCHAR(50) NOT NULL,"
                + "description VARCHAR(255),"
                + "tx_date DATE NOT NULL,"
                + "INDEX idx_date (tx_date))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS budgets ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "category VARCHAR(50) NOT NULL,"
                + "budget_month CHAR(7) NOT NULL,"
                + "limit_amount DECIMAL(12,2) NOT NULL,"
                + "UNIQUE KEY uq_cat_month (category, budget_month))");
        }
        System.out.println("Database ready: " + NAME);
    }
}
