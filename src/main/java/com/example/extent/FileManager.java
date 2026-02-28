package com.example.extent;

import com.example.LogManager;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    String path;
    LogManager log;

    public FileManager(String path, LogManager log) {
        this.path = path;
        this.log = log;
    }

    public byte[] get(String fileName) throws IOException {
        Path filePath = Paths.get(path, fileName);
        log.log("FILEMANAGER: GET file: " + fileName);

        if (fileName.endsWith("/")) {
            if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                List<String> fileNames = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(filePath)) {
                    for (Path entry : stream) {
                        if (Files.isDirectory(entry)) {
                            fileNames.add(entry.getFileName().toString() + "/");
                        } else {
                            fileNames.add(entry.getFileName().toString());
                        }
                    }
                }

                String joinedFileNames = String.join("\n", fileNames);
                return joinedFileNames.getBytes("UTF-8");
            } else {
                log.log("FILEMANAGER: Dir not found: " + fileName);
                return null;
            }
        } else {
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                log.log("FILEMANAGER: File not found: " + fileName);
                return null;
            }
        }
    }

    public boolean put(String fileName, byte[] content) throws IOException {
        Path filePath = Paths.get(path, fileName);
        log.log("FILEMANAGER: PUT file: " + fileName);

        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.log("FILEMANAGER: Created directory " + parentDir);
        }

        if (content == null) {
            if (fileName.endsWith("/")) {
                if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                    Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    log.log("FILEMANAGER: Dir deleted " + filePath);
                    return true;
                } else {
                    log.log("FILEMANAGER: Dir not found " + fileName);
                    return false;
                }
            } else {
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    Files.delete(filePath);
                    log.log("FILEMANAGER: File deleted " + filePath);
                    return true;
                } else {
                    log.log("FILEMANAGER: File not found " + fileName);
                    return false;
                }
            }
        } else {
            log.log("        - before write");
            if (fileName.endsWith("/")) {
                if (!Files.exists(filePath)) {
                    Files.createDirectories(filePath);
                    log.log("FILEMANAGER: Dir created " + filePath);
                }
                return true;
            } else {
                log.log("        - in write file");
                try {
                    Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    log.log("FILEMANAGER: Error during PUT: " + e.getMessage());
                }

                log.log("FILEMANAGER: File written " + filePath);
                return true;
            }
        }
    }
}
