package dev.drperky.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public String serverAddress = "127.0.0.1";
    public int serverPort = 25564;
    public int proxyPort = 25565;
    //public boolean debug = false;

    public static Config load(File jarDir) {
        Config config = new Config();
        Properties props = new Properties();

        File file = new File(jarDir, "proxy.properties");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception e) {
                System.out.println("Failed to read proxy.properties, using defaults.");
            }
        }

        config.serverAddress = props.getProperty("serverAddress", config.serverAddress);
        config.serverPort = parseInt(props.getProperty("serverPort"), config.serverPort);
        config.proxyPort = parseInt(props.getProperty("proxyPort"), config.proxyPort);
        //config.debug = Boolean.parseBoolean(props.getProperty("debug", String.valueOf(config.debug)));

        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("serverAddress=" + config.serverAddress + "\n");
                writer.write("serverPort=" + config.serverPort + "\n");
                writer.write("proxyPort=" + config.proxyPort + "\n");
                //writer.write("debug=" + config.debug + "\n");
            } catch (IOException ignored) {}
        }

        return config;
    }

    private static int parseInt(String val, int def) {
        try {
            return val != null ? Integer.parseInt(val) : def;
        } catch (Exception e) {
            return def;
        }
    }
}
