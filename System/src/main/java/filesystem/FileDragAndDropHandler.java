package filesystem;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileDragAndDropHandler {
    private FileManager fileManager;
    private String documentsPath;
    private String trashPath;

    public FileDragAndDropHandler(FileManager fileManager, String documentsPath, String trashPath) {
        this.fileManager = fileManager;
        this.documentsPath = documentsPath;
        this.trashPath = trashPath;
    }

    public void addDropTarget(JComponent component) {
        new DropTarget(component, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isFileListFlavorSupported(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
                } else {
                    dtde.rejectDrag();
                }
            }
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (!isFileListFlavorSupported(dtde)) {
                    dtde.rejectDrag();
                }
            }
            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                
            }
            @Override
            public void dragExit(DropTargetEvent dte) {
                
            }
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (isFileListFlavorSupported(dtde)) {
                        int dropAction = dtde.getDropAction();
                        dtde.acceptDrop(dropAction);
                        Transferable transferable = dtde.getTransferable();
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (component instanceof JTree) {
                            JTree tree = (JTree) component;
                            TreePath path = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
                            if (path != null) {
                                String targetNodeName = path.getLastPathComponent().toString();
                                File targetDir = resolveDirectory(targetNodeName);

                                if (targetDir != null && targetDir.isDirectory()) {
                                    
                                    if (targetDir.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                                        JOptionPane.showMessageDialog(null, "Нельзя копировать или перемещать файлы в папку System.");
                                        Logger.log("Попытка переместить/скопировать файл в папку System заблокирована");
                                        dtde.dropComplete(false);
                                        return;
                                    }

                                    for (File file : files) {
                                        moveOrCopyFile(file, targetDir, dropAction == DnDConstants.ACTION_MOVE);
                                    }
                                    fileManager.refreshDirectoryNode(targetDir);
                                }
                            }
                        } else {
                            File targetDir = new File(documentsPath);
                            for (File file : files) {
                                File dest = new File(targetDir, file.getName());
                                if (file.isDirectory()) {
                                    dest.mkdirs();
                                    copyDirectoryRecursively(file, dest);

                                    if (dropAction == DnDConstants.ACTION_MOVE) {
                                        deleteDirectoryContents(file);
                                        file.delete();
                                    }
                                } else {
                                    if (dropAction == DnDConstants.ACTION_MOVE) {
                                        Files.move(file.toPath(), dest.toPath());
                                    } else {
                                        Files.copy(file.toPath(), dest.toPath());
                                    }
                                }

                                fileManager.addFileToTree(dest);
                            }
                            fileManager.refreshDirectoryNode(targetDir);
                        }
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    dtde.rejectDrop();
                }
            }
        });
    }

    private boolean isFileListFlavorSupported(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    private boolean isFileListFlavorSupported(DropTargetDropEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    private File resolveDirectory(String nodeName) {
        switch (nodeName) {
            case "Мои документы":
                return new File(documentsPath);
            case "Корзина":
                return new File(trashPath);
            default:
                File file = fileManager.findFileInAllDirectories(nodeName);
                return (file != null && file.isDirectory()) ? file : null;
        }
    }

    private void moveOrCopyFile(File sourceFile, File targetDir, boolean isMove) {
        try {
            if (sourceFile.isDirectory()) {
                
                File destDir = new File(targetDir, sourceFile.getName());
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }

                
                copyDirectoryRecursively(sourceFile, destDir);

                
                if (isMove) {
                    deleteDirectoryContents(sourceFile);
                    sourceFile.delete(); 
                }
            } else {
                
                File destFile = new File(targetDir, sourceFile.getName());
                if (!destFile.exists()) {
                    if (isMove) {
                        Files.move(sourceFile.toPath(), destFile.toPath());
                    } else {
                        Files.copy(sourceFile.toPath(), destFile.toPath());
                    }
                } else {
                    
                    String baseName = "Копия_" + sourceFile.getName();
                    destFile = new File(targetDir, baseName);
                    int copyNumber = 1;
                    while (destFile.exists()) {
                        String newName = "Копия_" + copyNumber + "_" + sourceFile.getName();
                        destFile = new File(targetDir, newName);
                        copyNumber++;
                    }
                    if (isMove) {
                        Files.move(sourceFile.toPath(), destFile.toPath());
                    } else {
                        Files.copy(sourceFile.toPath(), destFile.toPath());
                    }
                }

                
                if (isMove) {
                    sourceFile.delete();
                }
            }

            
            if (isMove) {
                fileManager.refreshDirectoryNode(sourceFile.getParentFile());
            }
            fileManager.refreshDirectoryNode(targetDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyDirectoryRecursively(File sourceDir, File targetDir) throws Exception {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(targetDir, file.getName());
                if (file.isDirectory()) {
                    
                    copyDirectoryRecursively(file, destFile);
                } else {
                    
                    Files.copy(file.toPath(), destFile.toPath());
                }
            }
        }
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

    public void enableDragAndDrop(JTree tree) {
        tree.setDragEnabled(true);
        tree.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree tree = (JTree) c;
                TreePath[] paths = tree.getSelectionPaths();
                if (paths == null) return null;

                List<File> files = new ArrayList<>();
                boolean isSystemFileSelected = false;

                for (TreePath path : paths) {
                    String fileName = path.getLastPathComponent().toString();
                    File file = fileManager.findFileInAllDirectories(fileName);
                    if (file != null) {
                        
                        if (file.getAbsolutePath().startsWith(fileManager.getSystemPath())) {
                            isSystemFileSelected = true;
                        } else {
                            files.add(file);
                        }
                    }
                }

                
                if (isSystemFileSelected) {
                    JOptionPane.showMessageDialog(null, "Нельзя перетаскивать файлы из папки System.");
                    return null;
                }

                if (files.isEmpty()) return null;
                return new FileManager.FileListTransferable(files);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE; 
            }
        });
    }
}
