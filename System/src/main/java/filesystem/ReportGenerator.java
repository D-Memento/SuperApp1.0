package filesystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportGenerator {
    private List<ProcessTracker.ProcessInfo> processes;
    private FileManager fileManager;

    
    public ReportGenerator(List<ProcessTracker.ProcessInfo> processes, FileManager fileManager) {
        this.processes = processes;
        this.fileManager = fileManager;
    }

    public void saveReport(String fileName) {
        
        String documentsPath = new File("Мои документы").getAbsolutePath();
        String fullPath = documentsPath + File.separator + fileName;

        try (FileWriter writer = new FileWriter(fullPath)) {
            
            for (ProcessTracker.ProcessInfo process : processes) {
                writer.write(process.toString() + System.lineSeparator());
            }
            System.out.println("Отчет успешно сохранен в файл: " + fullPath);

            
            if (fileManager != null) {
                fileManager.addFileToTree(new File(fullPath));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении отчета: " + e.getMessage());
        }
    }
}
