package filesystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportGenerator {
    private List<ProcessTracker.ProcessInfo> processes;
    private FileManager fileManager;

    // Конструктор принимает список процессов и экземпляр FileManager
    public ReportGenerator(List<ProcessTracker.ProcessInfo> processes, FileManager fileManager) {
        this.processes = processes;
        this.fileManager = fileManager;
    }

    public void saveReport(String fileName) {
        // Получаем путь к папке "Мои документы"
        String documentsPath = new File("Мои документы").getAbsolutePath();
        String fullPath = documentsPath + File.separator + fileName;

        try (FileWriter writer = new FileWriter(fullPath)) {
            // Записываем информацию о процессах в файл
            for (ProcessTracker.ProcessInfo process : processes) {
                writer.write(process.toString() + System.lineSeparator());
            }
            System.out.println("Отчет успешно сохранен в файл: " + fullPath);

            // Добавляем файл в дерево файлового менеджера
            if (fileManager != null) {
                fileManager.addFileToTree(new File(fullPath));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении отчета: " + e.getMessage());
        }
    }
}