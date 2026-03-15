package filesystem;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class PopupWindow extends JFrame {
    private JTextArea infoArea;

    public PopupWindow() {
        setTitle("Системная информация");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(infoArea), BorderLayout.CENTER);
        new Thread(this::readFromStdIn).start();
    }
    private void readFromStdIn() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine())
                SwingUtilities.invokeLater(() -> infoArea.append(finalLine + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PopupWindow().setVisible(true);
        });
    }
}
