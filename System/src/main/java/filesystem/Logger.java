package filesystem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE_NAME = "filemanager_log.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static String getLogFileName() {
        return LOG_FILE_NAME;
    }
    public static void log(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME, true))) {
            writer.write("[" + LocalDateTime.now().format(formatter) + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Не удалось записать в лог: " + e.getMessage());
        }
    }
    public static void logStartup() {
        log("Программа запущена");
    }
    public static void logDirectoryOpen(String path) {
        log("Открыта директория: " + path);
    }

    public static void logFileAction(String action, String fileName) {
        log("Файл \"" + fileName + "\" - действие: " + action);
    }
    public static void logSearchPerformed(String query) {
        log("Поиск: " + query);
    }
    public static void logTerminalStart() {
        log("Терминал запущен");
    }

    public static void logCommandExecuted(String command) {
        log("Выполнена команда в терминале: " + command);
    }
}