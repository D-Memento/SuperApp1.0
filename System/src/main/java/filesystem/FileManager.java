package filesystem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private File documentsDir = new File("Мои документы");
    private File trashDir = new File("Корзина");
    private String documentsPath = documentsDir.getAbsolutePath();
    private String trashPath = trashDir.getAbsolutePath();
    private List<File> selectedFiles = new ArrayList<>();
    private FileDragAndDropHandler dragAndDropHandler;
    private File systemDir = new File("System");
    private String systemPath = systemDir.getAbsolutePath();

    public FileManager(JFrame parentFrame) {
        ensureDirectoriesExist();
        fileTree = new JTree(buildFileTree());
        treeModel = (DefaultTreeModel) fileTree.getModel();
        setCustomTreeFont();

        systemDir = new File("System");
        systemPath = systemDir.getAbsolutePath();

        
        dragAndDropHandler = new FileDragAndDropHandler(this, documentsPath, trashPath);
        dragAndDropHandler.addDropTarget(fileTree);
        dragAndDropHandler.enableDragAndDrop(fileTree);

        setupContextMenu();


    }

    public void refreshRemovableDevicesNode() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode removableNode = null;

        
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            if ("Съемные устройства".equals(child.getUserObject())) {
                removableNode = child;
                break;
            }
        }

        if (removableNode != null) {
            
            removableNode.removeAllChildren();

            
            RemovableDeviceManager deviceManager = new RemovableDeviceManager();
            List<File> devices = deviceManager.getMountedDevices();
            if (devices.isEmpty()) {
                removableNode.add(new DefaultMutableTreeNode("Нет подключенных устройств"));
            } else {
                for (File device : devices) {
                    removableNode.add(buildTree(device, device.getName()));
                }
            }

            treeModel.reload(removableNode);
        }
    }

    public JTree getFileTree() {
        return fileTree;
    }

    public String getDocumentsPath() {
        return documentsPath;
    }

    public String getTrashPath() {
        return trashPath;
    }

    public String getSystemPath() {
        return systemPath;
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    private DefaultMutableTreeNode buildFileTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Файловый менеджер");
        root.add(buildTree(documentsDir, "Мои документы"));
        root.add(buildTree(trashDir, "Корзина"));
        root.add(buildTree(systemDir, "System"));
        root.add(buildRemovableDevicesTree());
        return root;
    }

    private DefaultMutableTreeNode buildTree(File dir, String displayName) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(displayName);
        if (dir.exists() && dir.isDirectory()) {
            addFilesToNode(node, dir);
        }
        return node;
    }

    private void addFilesToNode(DefaultMutableTreeNode node, File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    node.add(new DefaultMutableTreeNode(file.getName()));
                } else if (file.isDirectory()) {
                    DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(file.getName());
                    node.add(subNode);
                    addFilesToNode(subNode, file);
                }
            }
        }
    }

    private DefaultMutableTreeNode buildRemovableDevicesTree() {
        DefaultMutableTreeNode removableNode = new DefaultMutableTreeNode("Съемные устройства");
        RemovableDeviceManager deviceManager = new RemovableDeviceManager();
        List<File> devices = deviceManager.getMountedDevices();
        if (devices.isEmpty()) {
            removableNode.add(new DefaultMutableTreeNode("Нет подключенных устройств"));
        } else {
            for (File device : devices) {
                removableNode.add(buildTree(device, device.getName()));
            }
        }
        return removableNode;
    }

    private void setCustomTreeFont() {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setFont(new Font("Arial", Font.BOLD, 24));
        fileTree.setCellRenderer(renderer);
    }

    private void setupContextMenu() {
        fileTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
        String fileName = (path != null) ? path.getLastPathComponent().toString() : null;
        JPopupMenu contextMenu = createContextMenu(fileName);
        contextMenu.show(fileTree, e.getX(), e.getY());
    }

    public JPopupMenu createContextMenu(String fileName) {
        JPopupMenu menu = new JPopupMenu();
        Font font = new Font("Arial", Font.BOLD, 20);
        JMenuItem open = new JMenuItem("Открыть");
        JMenuItem copy = new JMenuItem("Копировать");
        JMenuItem paste = new JMenuItem("Вставить");
        JMenuItem rename = new JMenuItem("Переименовать");
        JMenuItem delete = new JMenuItem("Удалить в корзину");
        JMenuItem restore = new JMenuItem("Восстановить из корзины");
        JMenuItem clearTrash = new JMenuItem("Очистить корзину");
        JMenuItem properties = new JMenuItem("Свойства");
        JMenuItem createFileItem = new JMenuItem("Создать файл");
        JMenuItem createFolderItem = new JMenuItem("Создать папку");

        open.setFont(font);
        copy.setFont(font);
        paste.setFont(font);
        rename.setFont(font);
        delete.setFont(font);
        restore.setFont(font);
        clearTrash.setFont(font);
        properties.setFont(font);
        createFileItem.setFont(font);
        createFolderItem.setFont(font);

        if ("Корзина".equals(fileName)) {
            
            menu.add(clearTrash);
            menu.add(properties);
            clearTrash.addActionListener(e -> clearTrash());
            properties.addActionListener(e -> showProperties(trashDir.getAbsolutePath()));
        } else if ("Мои документы".equals(fileName)) {
            
            menu.add(createFileItem);
            menu.add(createFolderItem);
            menu.add(properties);
            createFileItem.addActionListener(e -> createFile());
            createFolderItem.addActionListener(e -> createFolder());
            properties.addActionListener(e -> showProperties(documentsDir.getAbsolutePath()));
        } else {

            File file = findFileInAllDirectories(fileName);
            if (file != null && file.getAbsolutePath().startsWith(systemPath)) {
                Logger.log("Попытка выполнения действия с папкой System: " + fileName);
                
                JMenuItem propertiesOnly = new JMenuItem("Свойства");
                propertiesOnly.setFont(font);
                propertiesOnly.addActionListener(e -> showProperties(file.getAbsolutePath()));
                menu.add(propertiesOnly);
                return menu;
            }

            if (file != null && file.isDirectory()) {
                menu.add(createFileItem);
                menu.add(createFolderItem);
                menu.add(open);
                menu.add(paste);
                menu.add(rename);
                menu.add(delete);
                menu.add(properties);
                createFileItem.addActionListener(e -> createFile());
                createFolderItem.addActionListener(e -> createFolder());
                open.addActionListener(e -> openFile(fileName));
                paste.addActionListener(e -> pasteFile());
                rename.addActionListener(e -> renameFile(fileName));
                delete.addActionListener(e -> moveFileToTrash(fileName));
                properties.addActionListener(e -> showProperties(file.getAbsolutePath()));
            } else if (file != null && file.isFile()) {
                menu.add(open);
                menu.add(copy);
                menu.add(paste);
                menu.add(rename);
                menu.add(delete);
                if (file.getAbsolutePath().startsWith(trashPath)) {
                    menu.add(restore);
                }
                menu.add(properties);
                open.addActionListener(e -> openFile(fileName));
                copy.addActionListener(e -> copyFile(fileName));
                paste.addActionListener(e -> pasteFile());
                rename.addActionListener(e -> renameFile(fileName));
                delete.addActionListener(e -> moveFileToTrash(fileName));
                restore.addActionListener(e -> restoreFile(fileName));
                properties.addActionListener(e -> showProperties(file.getAbsolutePath()));
            }
        }
        return menu;
    }

    public void openFile(String fileName) {

        if (fileName != null) {
            try {
                File file = findFileInAllDirectories(fileName);
                if (file != null) {
                    Desktop.getDesktop().open(file);
                    filesystem.Logger.logFileAction("Открыт", fileName);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void copyFile(String fileName) {
        filesystem.Logger.logFileAction("Скопирован", fileName);
        if (fileName != null) {
            selectedFiles.clear();
            File file = findFileInAllDirectories(fileName);
            if (file != null && file.exists()) {
                selectedFiles.add(file);
            }
        }
    }

    private void pasteFile() {

        TreePath currentSelection = fileTree.getSelectionPath();
        File destinationDir = new File(documentsPath);
        if (currentSelection != null) {
            DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) currentSelection.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof String) {
                String selectedPath = (String) selectedNode.getUserObject();
                File selectedFile = findFileInAllDirectories(selectedPath);
                if (selectedFile != null && selectedFile.isDirectory()) {
                    destinationDir = selectedFile;
                } else if (selectedFile != null) {
                    destinationDir = selectedFile.getParentFile();
                }
            }
        }
        for (File file : selectedFiles) {
            try {
                String baseName = "Копия_" + file.getName();
                String destinationPath = destinationDir.getAbsolutePath() + "/" + baseName;
                File dest = new File(destinationPath);
                int copyNumber = 1;
                while (dest.exists()) {
                    String newName = "Копия_" + copyNumber + "_" + file.getName();
                    destinationPath = destinationDir.getAbsolutePath() + "/" + newName;
                    dest = new File(destinationPath);
                    copyNumber++;
                }
                Files.copy(file.toPath(), dest.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        refreshDirectoryNode(destinationDir);
    }

    private void renameFile(String fileName) {

        if (fileName != null) {
            File oldFile = findFileInAllDirectories(fileName);
            if (oldFile == null) return;
            JDialog dialog = new JDialog();
            dialog.setTitle("Переименовать файл");
            dialog.setSize(600, 300);
            dialog.setMinimumSize(new Dimension(350, 150));
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(true);
            Font font = new Font("Arial", Font.BOLD, 16);
            JTextField newNameField = new JTextField(fileName);
            newNameField.setFont(font);
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(new JLabel("Новое имя файла:"), BorderLayout.NORTH);
            panel.add(newNameField, BorderLayout.CENTER);
            JButton okButton = new JButton("ОК");
            JButton cancelButton = new JButton("Отмена");
            okButton.setFont(font);
            cancelButton.setFont(font);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(okButton);
            buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPanel.add(cancelButton);
            dialog.add(panel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            okButton.addActionListener(e -> {
                File newFile = new File(oldFile.getParent() + "/" + newNameField.getText());
                if (oldFile.renameTo(newFile)) {
                    refreshDirectoryNode(newFile.getParentFile()); 
                }
                dialog.dispose();
            });
            cancelButton.addActionListener(e -> dialog.dispose());
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }
    }

    public void moveFileToTrash(String fileName) {
        filesystem.Logger.logFileAction("Удалён", fileName);
        if (fileName != null) {
            File file = findFileInAllDirectories(fileName);
            if (file != null && file.exists()) {
                try {
                    String uniqueName = getUniqueFileNameInTrash(file.getName());
                    File trashFile = new File(trashPath + "/" + uniqueName);

                    if (file.isDirectory()) {
                        copyDirectoryToTrash(file, trashFile);
                    } else {
                        Files.move(file.toPath(), trashFile.toPath());
                    }

                    deleteFileOrDirectory(file);

                    refreshDirectoryNode(trashDir);
                    refreshDirectoryNode(file.getParentFile());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void copyDirectoryToTrash(File sourceDir, File targetDir) throws Exception {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        for (File file : sourceDir.listFiles()) {
            File destFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                
                copyDirectoryToTrash(file, destFile);
            } else {
                
                Files.copy(file.toPath(), destFile.toPath());
            }
        }
    }

    private String getUniqueFileNameInTrash(String fileName) {
        File trashFile = new File(trashPath + "/" + fileName);
        int copyNumber = 1;
        while (trashFile.exists()) {
            String baseName = fileName.contains(".")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
            String extension = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf('.'))
                    : "";
            String newName = baseName + " (" + copyNumber + ")" + extension;
            trashFile = new File(trashPath + "/" + newName);
            copyNumber++;
        }
        return trashFile.getName();
    }

    public void restoreFile(String fileName) {
        if (fileName != null) {
            File file = findFileInAllDirectories(fileName);
            if (file != null && file.getAbsolutePath().startsWith(trashPath)) {
                String originalName = fileName;

                if (originalName.contains(" (")) {
                    int index = originalName.lastIndexOf(" (");
                    if (originalName.contains(".")) {
                        String extension = originalName.substring(originalName.lastIndexOf("."));
                        originalName = originalName.substring(0, index) + extension;
                    } else {
                        originalName = originalName.substring(0, index);
                    }
                }
                File destDir = new File(documentsPath);

                File dest = new File(destDir, file.getName());

                try {
                    Files.move(file.toPath(), dest.toPath());
                    refreshDirectoryNode(dest.getParentFile());
                    refreshDirectoryNode(new File(trashPath));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void clearTrash() {
        filesystem.Logger.log("Корзина очищена");
        deleteDirectoryContents(trashDir);
        refreshDirectoryNode(trashDir);
    }

    private void deleteDirectoryContents(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    
                    deleteDirectoryContents(file);
                }
                
                file.delete();
            }
        }
    }

    private void showProperties(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                StringBuilder info = new StringBuilder();

                
                info.append("Имя: ").append(file.getName()).append("\n");
                info.append("Тип: ").append(file.isDirectory() ? "Папка" : "Файл").append("\n");

                
                if (file.isFile()) {
                    info.append("Размер: ").append(file.length()).append(" байт\n");
                } else if (file.isDirectory()) {
                    long size = calculateDirectorySize(file);
                    info.append("Размер: ").append(size).append(" байт\n");
                }

                info.append("Путь: ").append(file.getAbsolutePath());

                
                JTextArea textArea = new JTextArea(info.toString());
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 200));
                JDialog dialog = new JDialog();
                dialog.setTitle("Свойства");
                dialog.getContentPane().add(scrollPane);
                dialog.setSize(600, 300);
                dialog.setMinimumSize(new Dimension(300, 200));
                dialog.setResizable(true);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        }
    }

    
    private long calculateDirectorySize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }



    public void createFile() {
        createFileOrFolder(true);
    }

    public void createFolder() {
        createFileOrFolder(false);
    }

    private void createFileOrFolder(boolean isFile) {
        final File[] destinationDir = {new File(documentsPath)};
        TreePath currentSelection = fileTree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) currentSelection.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof String) {
                String selectedPath = (String) selectedNode.getUserObject();
                File selectedFile = findFileInAllDirectories(selectedPath);
                if (selectedFile != null && selectedFile.isDirectory()) {
                    destinationDir[0] = selectedFile;
                } else if (selectedFile != null) {
                    destinationDir[0] = selectedFile.getParentFile();
                }
            }
        }

        JDialog dialog = new JDialog();
        dialog.setTitle(isFile ? "Создать файл" : "Создать папку");
        dialog.setSize(600, 300);
        dialog.setMinimumSize(new Dimension(350, 150));
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);

        Font font = new Font("Arial", Font.BOLD, 16);
        JTextField nameField = new JTextField();
        nameField.setFont(font);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel(isFile ? "Имя нового файла:" : "Имя новой папки:"), BorderLayout.NORTH);
        panel.add(nameField, BorderLayout.CENTER);

        JButton okButton = new JButton("ОК");
        JButton cancelButton = new JButton("Отмена");
        okButton.setFont(font);
        cancelButton.setFont(font);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(cancelButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                File newEntity = new File(destinationDir[0].getAbsolutePath() + "/" + name);
                try {
                    if (isFile ? newEntity.createNewFile() : newEntity.mkdir()) {
                        refreshDirectoryNode(destinationDir[0]);

                        Logger.logFileAction(
                                isFile ? "Создан" : "Создана папка",
                                name
                        );
                    }
                } catch (Exception ignored) {}
            }
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public File findFileInAllDirectories(String fileName) {
        File file = findFileRecursively(new File(documentsPath), fileName);
        if (file != null) return file;
        file = findFileRecursively(new File(trashPath), fileName);
        if (file != null) return file;
        file = findFileRecursively(new File(systemPath), fileName);
        if (file != null) return file;
        RemovableDeviceManager deviceManager = new RemovableDeviceManager();
        List<File> devices = deviceManager.getMountedDevices();
        for (File device : devices) {
            file = findFileRecursively(device, fileName);
            if (file != null) return file;
        }
        return null;
    }

    private File findFileRecursively(File directory, String fileName) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName)) {
                    return file;
                }
                if (file.isDirectory()) {
                    File result = findFileRecursively(file, fileName);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private void ensureDirectoriesExist() {
        if (!documentsDir.exists()) documentsDir.mkdirs();
        if (!trashDir.exists()) trashDir.mkdirs();
        if (!systemDir.exists()) systemDir.mkdirs();
    }

    public void refreshDirectoryNode(File directory) {

        List<TreePath> expandedPaths = saveExpandedPaths(fileTree);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath path = findTreePathForDirectory(root, directory);

        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

            node.removeAllChildren();

            addFilesToNode(node, directory);

            treeModel.reload(node);
        }

        restoreExpandedPaths(fileTree, expandedPaths);
    }
    private List<TreePath> saveExpandedPaths(JTree tree) {
        List<TreePath> expandedPaths = new ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        enumerateExpandedPaths(tree, new TreePath(root), expandedPaths);
        return expandedPaths;
    }


    private void enumerateExpandedPaths(JTree tree, TreePath parentPath, List<TreePath> expandedPaths) {
        if (tree.isExpanded(parentPath)) {
            expandedPaths.add(parentPath);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                TreePath childPath = parentPath.pathByAddingChild(childNode);
                enumerateExpandedPaths(tree, childPath, expandedPaths);
            }
        }
    }

    private void restoreExpandedPaths(JTree tree, List<TreePath> expandedPaths) {
        for (TreePath path : expandedPaths) {
            tree.expandPath(path);
        }
    }

    private TreePath findTreePathForDirectory(DefaultMutableTreeNode node, File targetDir) {
        String nodeName = (String) node.getUserObject();
        File nodeFile = new File(targetDir.getParent(), nodeName);

        if (nodeFile.equals(targetDir)) {
            return new TreePath(node.getPath());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath result = findTreePathForDirectory(childNode, targetDir);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public boolean deleteFileOrDirectory(File file) {
        if (file.isDirectory()) {
            
            for (File child : file.listFiles()) {
                deleteFileOrDirectory(child);
            }
            return file.delete(); 
        } else {
            return file.delete(); 
        }
    }

    public static class FileListTransferable implements Transferable {
        private final List<File> fileList;

        public FileListTransferable(List<File> files) {
            this.fileList = new ArrayList<>(files);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{
                    DataFlavor.javaFileListFlavor,
                    DataFlavor.stringFlavor
            };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.javaFileListFlavor) ||
                    flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                return fileList;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                StringBuilder sb = new StringBuilder();
                for (File file : fileList) {
                    sb.append(file.getAbsolutePath()).append("\n");
                }
                return sb.toString();
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public void addFileToTree(File file) {
        try {
            File dest = new File(documentsPath + "/" + file.getName());
            Files.copy(file.toPath(), dest.toPath());
            refreshDirectoryNode(dest.getParentFile()); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<File> searchFilesAndFolders(String query) {
        List<File> results = new ArrayList<>();
        searchRecursively(new File(documentsPath), query, results);
        searchRecursively(new File(trashPath), query, results);
        searchRecursively(new File(systemPath), query, results);

        RemovableDeviceManager deviceManager = new RemovableDeviceManager();
        List<File> devices = deviceManager.getMountedDevices();
        for (File device : devices) {
            searchRecursively(device, query, results);
        }

        if (!results.isEmpty()) {
            highlightFileInTree(results.get(0));
        }

        Logger.logSearchPerformed(query);

        return results;
    }

    private void searchRecursively(File directory, String query, List<File> results) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    results.add(file);
                }
                if (file.isDirectory()) {
                    searchRecursively(file, query, results);
                }
            }
        }
    }

    public void highlightFileInTree(File fileToHighlight) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath path = findTreePath(root, fileToHighlight);
        if (path != null) {
            fileTree.setSelectionPath(path);
            fileTree.scrollPathToVisible(path);
        }
    }

    private TreePath findTreePath(DefaultMutableTreeNode node, File targetFile) {
        String nodeName = (String) node.getUserObject();
        File nodeFile = new File(targetFile.getParent(), nodeName);
        if (nodeFile.equals(targetFile)) {
            return new TreePath(node.getPath());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath result = findTreePath(childNode, targetFile);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    public File getSelectedFile() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            return null; 
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        String nodeName = (String) selectedNode.getUserObject(); 

        
        if ("Мои документы".equals(nodeName)) {
            return documentsDir;
        } else if ("Корзина".equals(nodeName)) {
            return trashDir;
        } else if ("System".equals(nodeName)) {
            Logger.log("Попытка доступа к защищённой папке: System");
            return systemDir;
        }

        if ("Съемные устройства".equals(nodeName)) {
            
            return new File("/media/user");
        }

        File selectedFile = findFileInAllDirectories(nodeName);
        if (selectedFile != null && selectedFile.getAbsolutePath().startsWith(systemPath)) {
            return null; 
        }

        return selectedFile;
    }

}
