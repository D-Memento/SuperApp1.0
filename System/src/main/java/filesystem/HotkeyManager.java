package filesystem;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class HotkeyManager {
    private JFrame frame;
    private FileManager fileManager;

    public HotkeyManager(JFrame frame, FileManager fileManager) {
        this.frame = frame;
        this.fileManager = fileManager;
        setupHotkeys();
    }
    private void setupHotkeys() {
        addHotkey(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, "createFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Logger.log("Горячая клавиша: Ctrl+N - создан файл");
                fileManager.createFile();
            }
        });
        addHotkey(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK, "moveFileToTrash", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFileName = getSelectedFileName();
                if (selectedFileName != null) {
                    Logger.log("Горячая клавиша: Ctrl+D - удален файл");
                    fileManager.moveFileToTrash(selectedFileName);
                }
            }
        });
        addHotkey(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, "restoreFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFileName = getSelectedFileName();
                if (selectedFileName != null) {
                    Logger.log("Горячая клавиша: Ctrl+R - восстановлен файл из корзины");
                    fileManager.restoreFile(selectedFileName);
                }
            }
        });
        addHotkey(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, "clearTrash", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Logger.log("Горячая клавиша: Ctrl+T - очищена корзина");
                fileManager.clearTrash();
            }
        });
        addHotkey(KeyEvent.VK_F1, 0, "aboutApp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Logger.log("Горячая клавиша: F1 - открыто окно 'О программе'");
                showAbout();
            }
        });
    }
    private void addHotkey(int keyCode, int modifiers, String actionName, Action action) {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
        frame.getRootPane().getActionMap().put(actionName, action);
    }
    private String getSelectedFileName() {
        TreePath selectionPath = fileManager.getFileTree().getSelectionPath();
        if (selectionPath != null) {
            String fileName = selectionPath.getLastPathComponent().toString();
            File selectedFile = fileManager.findFileInAllDirectories(fileName);
            if (selectedFile != null && selectedFile.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                JOptionPane.showMessageDialog(null, "Нельзя выполнять это действие с файлами из папки System.");
                Logger.log("Попытка выполнить действие с файлом из папки System заблокирована");
                return null;
            }
            return fileName;
        }
        return null;
    }

    private void showAbout() {
        JDialog aboutDialog = new JDialog(frame, "О программе", true);
        aboutDialog.setSize(1000, 700);
        aboutDialog.setLocationRelativeTo(frame);

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
}