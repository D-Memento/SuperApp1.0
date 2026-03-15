package filesystem;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProcessTracker {
    private List<ProcessInfo> processes = new ArrayList<>();
    private LocalDateTime appStartTime;

    public ProcessTracker() {
        this.appStartTime = LocalDateTime.now(); // Запоминаем время старта приложения
    }

    // Класс для хранения информации о процессе
    public static class ProcessInfo {
        private String processName;
        private String startTime;
        private long pid;

        public ProcessInfo(String processName, LocalDateTime startTime) {
            this.processName = processName;
            this.startTime = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.pid = -1;
        }

        public ProcessInfo(String processName, LocalDateTime startTime, long pid) {
            this.processName = processName;
            this.startTime = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.pid = pid;
        }

        @Override
        public String toString() {
            return "Имя процесса: " + processName +
                    ", PID: " + pid +
                    ", Время старта: " + startTime;
        }
    }


    // Получение списка всех активных процессов
    public void trackProcesses() {
        System.out.println("Отслеживание процессов...");
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        List<OSProcess> allProcesses = os.getProcesses();
        for (OSProcess process : allProcesses) {
            LocalDateTime processStartTime = Instant.ofEpochMilli(process.getStartTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            // Проверяем, был ли процесс запущен после старта приложения
            if (processStartTime.isAfter(appStartTime)) {
                processes.add(new ProcessInfo(process.getName(), processStartTime));
                System.out.println("Добавлен процесс: " + process.getName() + ", Время старта: " + processStartTime);
            }
        }
        System.out.println("Общее количество процессов: " + processes.size());
    }

    // Метод для получения списка процессов
    public List<ProcessInfo> getProcesses() {
        return processes;
    }
}