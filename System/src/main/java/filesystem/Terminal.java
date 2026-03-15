package filesystem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Terminal extends JFrame {
    private JTextArea consoleOutput;
    private JTextField commandInput;
    private File currentDirectory;
    private static final int MAX_LINES = 500;
    private FileManager fileManager;

    public Terminal(FileManager fileManager) {
        this.fileManager = fileManager;

        // Получаем корневую папку приложения (например, "Файловый менеджер")
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) fileManager.getFileTree().getModel().getRoot();
        String rootNodeName = (String) root.getUserObject();

        // Устанавливаем текущую директорию как корень приложения
        currentDirectory = new File(System.getProperty("user.dir"));
        if (!currentDirectory.exists()) {
            currentDirectory.mkdirs();
        }
        filesystem.Logger.logTerminalStart();
        setTitle("Терминал Linux");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        commandInput = new JTextField();
        commandInput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JButton executeButton = new JButton("Выполнить");
        executeButton.addActionListener(new ExecuteCommandListener());

        inputPanel.add(commandInput, BorderLayout.CENTER);
        inputPanel.add(executeButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        commandInput.addActionListener(new ExecuteCommandListener());

        printHelp();
    }

    private void printHelp() {
        consoleOutput.append("Доступные команды:\n");
        consoleOutput.append("1. ls - список файлов и папок\n");
        consoleOutput.append("2. pwd - текущая директория\n");
        consoleOutput.append("3. ping <host> - отправить ICMP-запрос\n");
        consoleOutput.append("4. ip addr - информация о сетевых интерфейсах\n");
        consoleOutput.append("5. ss -tuln - информация о сетевых соединениях\n");
        consoleOutput.append("6. cd <path> - изменить текущую директорию\n");
        consoleOutput.append("7. help - показать справку\n");
        consoleOutput.append("8. create <filename> - создать новый файл\n");
        consoleOutput.append("9. delete <filename> - удалить файл в корзину\n");
        consoleOutput.append("10. clear-trash - очистить корзину\n");
        consoleOutput.append("11. exit - выйти из терминала\n");
        consoleOutput.append("\nВведите команду и нажмите Enter:\n");
    }

    private void executeCommand(String command) {
        try {
            consoleOutput.append("$ " + command + "\n");
            filesystem.Logger.logCommandExecuted(command);
            // Разделяем команду на части
            String[] parts = command.split("\\s+");
            if (parts.length == 0) return;
            String cmd = parts[0];

            // Обработка встроенных команд
            switch (cmd) {
                case "help":
                    printHelp();
                    break;

                case "pwd":
                    consoleOutput.append("Текущая директория: " + currentDirectory.getAbsolutePath() + "\n");
                    break;

                case "cd":
                    handleCd(parts);
                    break;

                case "create":
                    handleCreate(parts);
                    break;

                case "delete":
                    handleDelete(parts);
                    break;

                case "clear-trash":
                    handleClearTrash();
                    break;

                case "exit":
                    dispose(); // Закрыть окно терминала
                    break;

                default:
                    runSystemCommand(command);
                    break;
            }
        } catch (Exception e) {
            consoleOutput.append("Ошибка: " + e.getMessage() + "\n");
        }
    }

    private void handleCd(String[] parts) {
        if (parts.length > 1) {
            String path = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
            File newDirectory = new File(path);

            if (!newDirectory.isAbsolute()) {
                newDirectory = new File(currentDirectory, path);
            }

            if (newDirectory.exists() && newDirectory.isDirectory()) {


                if (newDirectory.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                    consoleOutput.append("Ошибка: Доступ к папке 'System' запрещён.\n");
                    return;
                }

                currentDirectory = newDirectory;
                consoleOutput.append("Текущая директория: " + currentDirectory.getAbsolutePath() + "\n");
                fileManager.refreshDirectoryNode(currentDirectory);
            } else {
                consoleOutput.append("Ошибка: Директория не найдена: " + path + "\n");
            }
        } else {
            consoleOutput.append("Использование: cd <путь>\n");
        }
    }

    private void handleCreate(String[] parts) {
        if (parts.length > 1) {
            String filename = parts[1];
            File newFile = new File(currentDirectory, filename);

            if (currentDirectory.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                consoleOutput.append("Ошибка: Невозможно создать файл в защищённой папке 'System'.\n");
                return;
            }

            if (!newFile.exists()) {
                try {
                    boolean created = newFile.createNewFile();
                    if (created) {
                        consoleOutput.append("Файл успешно создан: " + newFile.getAbsolutePath() + "\n");
                        fileManager.refreshDirectoryNode(currentDirectory);
                    } else {
                        consoleOutput.append("Ошибка: Не удалось создать файл.\n");
                    }
                } catch (IOException e) {
                    consoleOutput.append("Ошибка: Не удалось создать файл — " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            } else {
                consoleOutput.append("Ошибка: Файл уже существует.\n");
            }
        } else {
            consoleOutput.append("Использование: create <filename>\n");
        }
    }

    private void handleDelete(String[] parts) {
        if (parts.length > 1) {
            String filename = parts[1];
            File fileToDelete = new File(currentDirectory, filename);

            // 🔒 Проверка: нельзя удалять файлы из System
            if (fileToDelete.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                consoleOutput.append("Ошибка: Невозможно удалить файл из защищённой папки 'System'.\n");
                return;
            }

            if (fileToDelete.exists()) {
                fileManager.moveFileToTrash(fileToDelete.getName());
                consoleOutput.append("Файл перемещён в корзину: " + fileToDelete.getAbsolutePath() + "\n");
                fileManager.refreshDirectoryNode(currentDirectory);
            } else {
                consoleOutput.append("Ошибка: Файл не найден.\n");
            }
        } else {
            consoleOutput.append("Использование: delete <filename>\n");
        }
    }

    private void handleClearTrash() {
        fileManager.clearTrash();
        consoleOutput.append("Корзина успешно очищена.\n");
        fileManager.refreshDirectoryNode(new File(fileManager.getTrashPath()));
    }

    private void runSystemCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.directory(currentDirectory);
            Process process = processBuilder.start();

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < MAX_LINES) {
                        consoleOutput.append(line + "\n");
                        lineCount++;
                    }
                    if (lineCount >= MAX_LINES) {
                        consoleOutput.append("Вывод слишком большой. Остальная часть скрыта.\n");
                    }
                } catch (IOException e) {
                    consoleOutput.append("Ошибка при чтении вывода: " + e.getMessage() + "\n");
                }
            });

            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        consoleOutput.append("Ошибка: " + errorLine + "\n");
                    }
                } catch (IOException e) {
                    consoleOutput.append("Ошибка при чтении ошибок: " + e.getMessage() + "\n");
                }
            });

            outputThread.start();
            errorThread.start();

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                consoleOutput.append("Команда превысила лимит времени и была остановлена.\n");
            }

            outputThread.join();
            errorThread.join();
        } catch (Exception e) {
            consoleOutput.append("Ошибка: " + e.getMessage() + "\n");
        }
    }

    private class ExecuteCommandListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String command = commandInput.getText().trim();
            if (!command.isEmpty()) {
                executeCommand(command);
                commandInput.setText("");
            }
        }
    }
}