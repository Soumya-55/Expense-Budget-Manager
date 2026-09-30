package com.expense;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Reads config.properties from the project root. */
public class Config {
    private static final Properties P = new Properties();
    static {
        try (InputStream in = new FileInputStream("config.properties")) {
            P.load(in);
        } catch (IOException e) {
            System.out.println("WARNING: config.properties not found - using defaults. Run from the project root folder!");
        }
    }
    public static String get(String key, String def) {
        return P.getProperty(key, def).trim();
    }
}
