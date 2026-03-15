package filesystem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Stack;

public class MainWindow extends JFrame {
    private filesystem.FileManager fileManager;
    private filesystem.ProcessTracker processTracker = new filesystem.ProcessTracker();
    private filesystem.FileDragAndDropHandler dragAndDropHandler; // Объявление dragAndDropHandler
    private JTextField searchField;
    private File currentDirectory = new File(System.getProperty("user.home")); // Текущая директория
    private JLabel systemInfoLabel; // Метка для отображения системной информации
    private JPanel cardPanel; // Панель для карточного интерфейса
    private JButton backButton; // Кнопка "Назад"

    public MainWindow() {
        setTitle("Суперапп - Файловый менеджер");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Инициализация FileManager
        fileManager = new filesystem.FileManager(this);

        // Панель для поиска
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.setFont(new Font("Arial", Font.BOLD, 20));
        Font searchFont = new Font("Arial", Font.PLAIN, 21); // Устанавливаем размер шрифта
        searchField.setFont(searchFont);

        // Действие при нажатии на кнопку "Поиск"
        searchButton.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                List<File> searchResults = fileManager.searchFilesAndFolders(query);
                displaySearchResults(searchResults);
                filesystem.Logger.logSearchPerformed(query); // Логируем запрос
            } else {
                JOptionPane.showMessageDialog(this, "Введите запрос для поиска!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchPanel.add(new JLabel("Поиск:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        add(searchPanel, BorderLayout.NORTH);

        // Левая панель: древовидное представление
        JScrollPane treeScrollPane = new JScrollPane(fileManager.getFileTree());
        treeScrollPane.setPreferredSize(new Dimension(300, getHeight()));

        // Правая панель: карточный интерфейс
        cardPanel = new JPanel();
        cardPanel.setLayout(new GridLayout(4, 4, 10, 10)); // 4 колонки, адаптивные строки
        cardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showCardPanelContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showCardPanelContextMenu(e);
                }
            }
        });
        // Панель управления навигацией
        JPanel navigationPanel = new JPanel(new BorderLayout(5, 5));
        backButton = new JButton("← Назад"); // Создание кнопки "Назад"
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.setEnabled(false); // Кнопка недоступна, пока нет истории

        // Действие при нажатии на кнопку "Назад"
        backButton.addActionListener(e -> {
            if (!navigationHistory.isEmpty()) {
                currentDirectory = navigationHistory.pop(); // Возвращаемся к предыдущей папке
                updateCardPanel(currentDirectory);
                backButton.setEnabled(!navigationHistory.isEmpty()); // Отключаем кнопку, если история пуста
            }
        });

        navigationPanel.add(backButton, BorderLayout.WEST);

        // Разделение экрана на две части
        JScrollPane cardScrollPane = new JScrollPane(cardPanel);
        cardScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Только вертикальная прокрутка
        cardScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Для более плавной прокрутки

// Разделение экрана на две части
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, cardScrollPane);
        splitPane.setDividerLocation(300); // Ширина левой панели

        // Объединяем панели
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(navigationPanel, BorderLayout.NORTH);
        rightPanel.add(splitPane, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.CENTER);

        // Добавление обработчика выбора узла в дереве файлов
        fileManager.getFileTree().addTreeSelectionListener(e -> {
            TreePath selectedPath = fileManager.getFileTree().getSelectionPath();
            if (selectedPath != null) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                String nodeName = (String) selectedNode.getUserObject();

                // Получаем выбранный файл или системную папку
                File selectedFile = fileManager.getSelectedFile();

                if (selectedFile != null) {
                    if (selectedFile.isDirectory()) {
                        navigateToDirectory(selectedFile); // Переход к выбранной папке
                    }
                } else {
                    // Обработка специальных системных папок
                    switch(nodeName) {
                        case "Мои документы":
                            navigateToDirectory(new File(fileManager.getDocumentsPath()));
                            break;
                        case "Корзина":
                            navigateToDirectory(new File(fileManager.getTrashPath()));
                            break;
                        case "System":
                            navigateToDirectory(new File(fileManager.getSystemPath()));
                            break;
                        case "Съемные устройства":
                            String username = System.getProperty("user.name");
                            File mediaUserDir = new File("/media/" + username);
                            File mediaDir = new File("/media");
                            File mntDir = new File("/mnt");

                            if (mediaUserDir.exists() && mediaUserDir.isDirectory()) {
                                navigateToDirectory(mediaUserDir);
                            } else if (mediaDir.exists() && mediaDir.isDirectory()) {
                                navigateToDirectory(mediaDir);
                            } else if (mntDir.exists() && mntDir.isDirectory()) {
                                navigateToDirectory(mntDir);
                            }
                            break;
                    }
                }
            }
        });

        // Создание Drag-and-Drop обработчика
        dragAndDropHandler = new filesystem.FileDragAndDropHandler(fileManager,
                fileManager.getDocumentsPath(),
                fileManager.getTrashPath());

        // Настройка DnD для всего окна
        dragAndDropHandler.addDropTarget((JComponent) getContentPane()); // Применяем DnD к корневому контейнеру
        dragAndDropHandler.addDropTarget(treeScrollPane); // Для дерева файлов
        dragAndDropHandler.addDropTarget(searchPanel);   // Для панели поиска

        // Создание метки для отображения системной информации
        systemInfoLabel = new JLabel("", SwingConstants.CENTER);
        systemInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(systemInfoLabel, BorderLayout.SOUTH);

        startSystemInfoUpdater();

        createMenuBar();

        new filesystem.HotkeyManager(this, fileManager); // Присоединяем HotkeyManager

        currentDirectory = new File(fileManager.getDocumentsPath());
        updateCardPanel(currentDirectory);
    }

    private void savePopupLogToFile(String data, String fileName) {
        try {
            String documentsPath = fileManager.getDocumentsPath();

            File logFile = new File(documentsPath, fileName);

            Files.write(logFile.toPath(), data.getBytes());

            fileManager.refreshDirectoryNode(new File(documentsPath));

            JOptionPane.showMessageDialog(this,
                    "Лог успешно сохранен в файл: " + logFile.getAbsolutePath(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при сохранении лога: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void showCardPanelContextMenu(MouseEvent e) {
        TreePath selectedPath = fileManager.getFileTree().getSelectionPath();
        File currentDirectory = null;

        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            String nodeName = (String) selectedNode.getUserObject();
            currentDirectory = fileManager.findFileInAllDirectories(nodeName);
        }

        if (currentDirectory == null || !currentDirectory.isDirectory()) {
            currentDirectory = new File(fileManager.getDocumentsPath());
        }

        JPopupMenu contextMenu = fileManager.createContextMenu(currentDirectory.getName());
        contextMenu.show(cardPanel, e.getX(), e.getY());
    }
    private void navigateToDirectory(File directory) {

        if (directory.isDirectory()) {
            if (currentDirectory != null) {
                navigationHistory.push(currentDirectory); // Сохраняем текущую папку в истории
            }
            currentDirectory = directory;
            updateCardPanel(directory);
            updateCardPanel(directory);
            filesystem.Logger.logDirectoryOpen(directory.getAbsolutePath());
            backButton.setEnabled(true); // Включаем кнопку "Назад"
        }
    }

    private Stack<File> navigationHistory = new Stack<>(); // Стек для истории переходов

    private JPanel createFileCard(File file) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        card.setPreferredSize(new Dimension(10, 10));

        JLabel iconLabel = new JLabel();
        Icon icon = file.isDirectory() ? UIManager.getIcon("FileView.directoryIcon") : UIManager.getIcon("FileView.fileIcon");
        iconLabel.setIcon(icon);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel nameLabel = new JLabel(file.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        nameLabel.setToolTipText(file.getAbsolutePath());

        card.add(iconLabel, BorderLayout.CENTER);
        card.add(nameLabel, BorderLayout.SOUTH);

        // обработчик кликов для выбора карточки
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Двойной клик
                    handleCardSelection(file);
                } else if (e.getClickCount() == 1) { // Одиночный клик
                    selectCard(card, file);
                }
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showCardContextMenu(e, file);
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showCardContextMenu(e, file);
            }
        });

        return card;
    }

    private void selectCard(JPanel card, File file) {
        for (Component component : cardPanel.getComponents()) {
            component.setBackground(null);
        }
        card.setBackground(Color.LIGHT_GRAY);
        fileManager.highlightFileInTree(file);
    }

    private void showCardContextMenu(MouseEvent e, File file) {
        JPopupMenu contextMenu = fileManager.createContextMenu(file.getName());
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleCardSelection(File file) {
        if (file.isDirectory()) {
            updateCardPanel(file);
        } else {
            fileManager.openFile(file.getName());
        }
        fileManager.highlightFileInTree(file);
    }
    private void updateCardPanel(File directory) {
        cardPanel.removeAll(); // Очищаем панель

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Создаем карточку для каждого файла/папки
                JPanel card = createFileCard(file);
                cardPanel.add(card);
            }
        }

        cardPanel.revalidate();
        cardPanel.repaint();
        startFileWatcher(directory);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setFont(new Font("Arial", Font.BOLD, 20));
        JMenuItem aboutItem = createMenuItem("О программе", e -> showAbout());
        JMenuItem saveReportItem = createMenuItem("Сохранить отчет", e -> {
            processTracker.trackProcesses(); // Отслеживаем процессы перед сохранением
            saveReport();
        });
        JMenuItem saveLogItem = createMenuItem("Сохранить лог", e -> {
            String logData = "Содержимое лога:\n" + readLogFile(filesystem.Logger.getLogFileName());
            saveLogToFile(logData, "filemanager_log_export.txt");
        });
        fileMenu.add(saveLogItem);

        fileMenu.add(saveReportItem);
        fileMenu.add(saveLogItem);
        fileMenu.add(aboutItem);

        // Меню "Устройства"
        JMenu deviceMenu = new JMenu("Устройства");
        deviceMenu.setFont(new Font("Arial", Font.BOLD, 20));
        JMenuItem refreshDevicesItem = createMenuItem("Обновить устройства", e -> {
            fileManager.refreshRemovableDevicesNode(); // Ручное обновление
            Logger.log("Съемные устройства обновлены вручную");
        });
        deviceMenu.add(refreshDevicesItem);

        // Меню "Справка"
        JMenu helpMenu = new JMenu("Справка");
        helpMenu.setFont(new Font("Arial", Font.BOLD, 20));
        JMenuItem hotkeysItem = createMenuItem("Горячие клавиши", e -> showHotkeysInfo());
        helpMenu.add(hotkeysItem);

        JMenu utilitiesMenu = new JMenu("Утилиты");
        utilitiesMenu.setFont(new Font("Arial", Font.BOLD, 20));
        JMenuItem systemMonitorItem = createMenuItem("Монитор ресурсов", e -> launchUtility("gnome-system-monitor"));
        JMenuItem controlCenterItem = createMenuItem("Центр управления", e -> launchUtility("gnome-control-center"));
        JMenuItem terminalSystemItem = createMenuItem("Системный терминал", e -> launchUtility("gnome-terminal"));

        utilitiesMenu.add(terminalSystemItem);
        utilitiesMenu.add(systemMonitorItem);
        utilitiesMenu.add(controlCenterItem);

        // Новое меню "Инструменты"
        JMenu toolsMenu = new JMenu("Инструменты");
        toolsMenu.setFont(new Font("Arial", Font.BOLD, 20));
        JMenuItem terminalItem = createMenuItem("Открыть терминал", e -> {
            SwingUtilities.invokeLater(() -> new filesystem.Terminal(fileManager).setVisible(true));
        });
        JMenuItem popupItem = createMenuItem("Открыть всплывающее окно", e -> createPopupWindow());
        toolsMenu.add(terminalItem);
        toolsMenu.add(popupItem);

        menuBar.add(fileMenu);
        menuBar.add(deviceMenu);
        menuBar.add(helpMenu);
        menuBar.add(utilitiesMenu);
        menuBar.add(toolsMenu);
        setJMenuBar(menuBar);
    }
    private String readLogFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            return "Не удалось прочитать лог.";
        }
    }
    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setFont(new Font("Arial", Font.BOLD, 16));
        menuItem.addActionListener(e -> {
            if (text.contains("Монитор ресурсов")) {
                Logger.log("Запущена системная утилита: Монитор ресурсов");
            } else if (text.contains("Центр управления")) {
                Logger.log("Запущена системная утилита: Центр управления");
            } else if (text.contains("Системный терминал")) {
                Logger.log("Запущена системная утилита: Системный терминал");
            }
            action.actionPerformed(e);
        });
        return menuItem;
    }

    private void showAbout() {
        JDialog aboutDialog = new JDialog(this, "О программе", true);
        aboutDialog.setSize(1000, 700);
        aboutDialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel aboutLabel = new JLabel(
                "<html><center><b>Суперапп v1.0</b><br>ОС: Linux<br>Язык: Java<br>Разработчик: Егоров Данила<br>Группа: ПрИ-32</center></html>");
        aboutLabel.setFont(new Font("Arial", Font.BOLD, 16));
        aboutLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(aboutLabel, BorderLayout.CENTER);
        aboutDialog.add(panel);
        aboutDialog.setVisible(true);
    }
    private void startFileWatcher(File directory) {
        new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path path = directory.toPath();
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.take(); // Ожидаем событие
                    } catch (InterruptedException e) {
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        SwingUtilities.invokeLater(() -> updateCardPanel(directory));
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void showHotkeysInfo() {
        JDialog hotkeysDialog = new JDialog(this, "Горячие клавиши", true);
        hotkeysDialog.setSize(600, 400);
        hotkeysDialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextArea hotkeysInfo = new JTextArea(
                "Горячие клавиши:\n" +
                        "Ctrl + N: Создать новый файл\n" +
                        "Ctrl + D: Удалить выбранный файл\n" +
                        "Ctrl + R: Восстановить файл из корзины\n" +
                        "Ctrl + T: Очистить корзину\n" +
                        "F1: Показать информацию о программе"
        );
        hotkeysInfo.setFont(new Font("Arial", Font.PLAIN, 16));
        hotkeysInfo.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(hotkeysInfo);
        panel.add(scrollPane, BorderLayout.CENTER);
        hotkeysDialog.add(panel);
        hotkeysDialog.setVisible(true);
    }

    private void refreshDevices() {
        fileManager.refreshDirectoryNode(new File(fileManager.getDocumentsPath()));
        fileManager.refreshDirectoryNode(new File(fileManager.getTrashPath()));
    }

    private void saveReport() {
        JDialog dialog = new JDialog(this, "Сохранить отчет", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel("Введите имя файла:");
        JTextField fileNameField = new JTextField("report.txt");
        JButton saveButton = new JButton("Сохранить");
        panel.add(label, BorderLayout.NORTH);
        panel.add(fileNameField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(panel);
        saveButton.addActionListener(e -> {
            String fileName = fileNameField.getText().trim();
            if (!fileName.isEmpty()) {
                filesystem.ReportGenerator reportGenerator = new filesystem.ReportGenerator(processTracker.getProcesses(), fileManager);
                reportGenerator.saveReport(fileName);
                refreshDevices();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Имя файла не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialog.setVisible(true);
    }

    private void displaySearchResults(List<File> results) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (File file : results) {
            listModel.addElement(file.getAbsolutePath());
        }
        JList<String> resultList = new JList<>(listModel);
        resultList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultList);

        // При выборе файла из списка выделяем его в дереве
        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = resultList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    File selectedFile = results.get(selectedIndex);
                    fileManager.highlightFileInTree(selectedFile);
                }
            }
        });
    }

    private void launchUtility(String command) {
        try {
            // Всегда запускать как внешнюю команду
            Runtime.getRuntime().exec(command);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось запустить утилиту: " + command,
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startSystemInfoUpdater() {
        new Thread(() -> {
            while (true) {
                try {
                    String systemInfo = collectSystemInfo();
                    SwingUtilities.invokeLater(() -> systemInfoLabel.setText(systemInfo));
                    Thread.sleep(5000); // Обновление каждые 5 секунд
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String collectSystemInfo() {
        StringBuilder info = new StringBuilder();
        String wifiStatus = executeCommand("iwgetid -r");
        info.append("Статус беспроводной сети: ").append(wifiStatus.isEmpty() ? "Не подключено" : wifiStatus).append("\n");
        String swapInfo = executeCommand("free -t | grep -i 'swap'");
        if (swapInfo.isEmpty()) {
            info.append("Процент используемой виртуальной памяти: Своп отключен\n");
        } else {
            String totalSwap = executeCommand("free -t | awk '/Swap:/ {print $2}'");
            String usedSwap = executeCommand("free -t | awk '/Swap:/ {print $3}'");
            if (totalSwap.isEmpty() || usedSwap.isEmpty()) {
                info.append("Процент используемой виртуальной памяти: N/A\n");
            } else {
                try {
                    long totalSwapValue = Long.parseLong(formatNumber(totalSwap));
                    long usedSwapValue = Long.parseLong(formatNumber(usedSwap));
                    if (totalSwapValue == 0) {
                        info.append("Процент используемой виртуальной памяти: Своп не настроен\n");
                    } else if (usedSwapValue == 0) {
                        info.append("Процент используемой виртуальной памяти: Своп настроен, но не используется\n");
                    } else {
                        double swapPercentage = (double) usedSwapValue / totalSwapValue * 100;
                        info.append("Процент используемой виртуальной памяти: ")
                                .append(String.format("%.2f", swapPercentage)).append("%\n");
                    }
                } catch (NumberFormatException e) {
                    info.append("Процент используемой виртуальной памяти: Ошибка при парсинге данных\n");
                }
            }
        }
        String ramUsage = executeCommand("free -m | awk '/Mem:/ {print $3}'");
        if (ramUsage.isEmpty()) {
            ramUsage = executeCommand("grep 'Active:' /proc/meminfo | awk '{print $2/1024}'");
        }
        info.append("Размер используемой оперативной памяти: ")
                .append(ramUsage.isEmpty() ? "N/A" : Math.round(Double.parseDouble(formatNumber(ramUsage))) + " MB").append("\n");
        return info.toString();
    }

    private String formatNumber(String number) {
        return number.replace(',', '.').trim();
    }

    private String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "LANG=C " + command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line.trim()).append("\n");
            }
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line.trim()).append("\n");
            }
            process.waitFor();
            if (errors.length() > 0) {
                System.err.println("Ошибки выполнения команды '" + command + "': " + errors);
            }
            return output.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void saveLogToFile(String data, String fileName) {
        try {
            String documentsPath = fileManager.getDocumentsPath();
            File logFile = new File(documentsPath, fileName);
            Files.write(logFile.toPath(), data.getBytes());
            fileManager.refreshDirectoryNode(new File(documentsPath));
            JOptionPane.showMessageDialog(this,
                    "Лог успешно сохранен в файл: " + logFile.getAbsolutePath(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при сохранении лога: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }




    private Process popupProcess;

    private void createPopupWindow() {
        filesystem.Logger.log("Открыто всплывающее окно");
        try {

            String javaHome = System.getProperty("java.home") + "/bin/java";
            String classpath = System.getProperty("java.class.path");
            String className = "filesystem.PopupWindow";

            // Запускаем PopupWindow как отдельный процесс
            ProcessBuilder pb = new ProcessBuilder(javaHome, "-cp", classpath, className);
            pb.redirectErrorStream(true); // Перенаправляем ошибки

            popupProcess = pb.start(); // Запускаем процесс

            OutputStreamWriter writer = new OutputStreamWriter(popupProcess.getOutputStream());

            // Отправляем данные каждые 5 секунд
            new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        String systemInfo = collectSystemInfo();
                        writer.write(systemInfo + "\n");
                        writer.flush();
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        writer.close();
                    } catch (IOException ignored) {}
                }
            }).start();

            // Добавляем информацию о новом процессе в tracker
            long pid = popupProcess.pid();
            processTracker.getProcesses().add(
                    new ProcessTracker.ProcessInfo("PopupWindow", LocalDateTime.now(), pid)
            );


            JOptionPane.showMessageDialog(this,
                    "Всплывающее окно запущено как отдельный процесс.",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при запуске всплывающего окна: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        filesystem.Logger.logStartup();
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }

}