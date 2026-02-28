package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private final Path path;
    public LogManager(String filePath) {
        this.path = Paths.get(filePath);
        try {
            // Create the log file if it does not exist
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = timestamp + " - " + message;

        try {
            Files.write(path, (logEntry + System.lineSeparator()).getBytes(),
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
