import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImagePixelColor {
    private static List<String[]> csvData;
    private static List<String[]> holdingData;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BufferedImage baseImage = null;
        BufferedImage overlayImage = null;
        try {
            baseImage = ImageIO.read(new File("input2.png"));
            overlayImage = ImageIO.read(new File("input1.png"));
            csvData = new CSVReader(new InputStreamReader(new FileInputStream("input3.csv"), "UTF-8")).readAll();
            holdingData = new CSVReader(new InputStreamReader(new FileInputStream("input4.csv"), "UTF-8")).readAll();

        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.exit(1);
        }
        ImagePanel imagePanel = new ImagePanel(baseImage, overlayImage);

        frame.add(new JScrollPane(imagePanel));
        frame.setSize(1200, 800);
        frame.setVisible(true);
    }

    static class ImagePanel extends JPanel {
        BufferedImage baseImage;
        BufferedImage overlayImage;
        HashMap<String, JFrame> holdingWindows = new HashMap<>();
        JScrollPane holdingListScrollPane; // Added instance variable for the holding list scroll pane
        JFrame listWindow; // Added instance variable for the list window

        public ImagePanel(BufferedImage baseImage, BufferedImage overlayImage) {
            this.baseImage = baseImage;
            this.overlayImage = overlayImage;
            setPreferredSize(new Dimension(overlayImage.getWidth(), overlayImage.getHeight()));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int color = baseImage.getRGB(e.getX(), e.getY());
                    int blue = color & 0xff;
                    int green = (color & 0xff00) >> 8;
                    int red = (color & 0xff0000) >> 16;
                    String hex = String.format("%02x%02x%02x", red, green, blue);

                    // Convert the point to the screen's coordinate system
                    Point screenPoint = new Point(e.getPoint());
                    SwingUtilities.convertPointToScreen(screenPoint, e.getComponent());

                    showMatchingData(hex, screenPoint);
                }
            });
        }

        private void showMatchingData(String hex, Point point) {
            for (String[] row : csvData) {
                String csvHex = row[1].trim();
                // Removing leading zeros
                csvHex = csvHex.replaceFirst("^0+(?!$)", "");
                hex = hex.replaceFirst("^0+(?!$)", "");

                if (csvHex.equalsIgnoreCase(hex)) {
                    JTextArea textArea = new JTextArea("Matching data: \n"
                            + "Hexadecimal ID: " + row[1] + "\n"
                            + "Province Name: " + row[2] + "\n"
                            + "Province Owner: " + row[3] + "\n"
                            + "Region: " + row[4]);
                    textArea.setEditable(false);

                    JButton button = new JButton("Show Holdings");
                    String finalHex = hex;
                    button.addActionListener(e -> {
                        showHoldingList(finalHex, point);
                    });

                    JButton hideButton = new JButton("Hide Holdings");
                    hideButton.addActionListener(e -> {
                        hideHoldingList();
                    });

                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.add(textArea, BorderLayout.CENTER);
                    panel.add(button, BorderLayout.PAGE_END);
                    panel.add(hideButton, BorderLayout.PAGE_START);

                    JFrame frame = showInNewFrame(panel, point);
                    frame.setSize(frame.getWidth() * 3 / 2, frame.getHeight());
                    frame.setAlwaysOnTop(true); // Set the frame to be always on top
                    break;
                }
            }
        }

        private void showHoldingList(String hex, Point point) {
            if (listWindow != null) {
                listWindow.dispose(); // Close the existing list window if it is already open
            }

            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String[] row : holdingData) {
                String csvHex = row[5].trim();
                csvHex = csvHex.replaceFirst("^0+(?!$)", "");
                hex = hex.replaceFirst("^0+(?!$)", "");

                if (csvHex.equalsIgnoreCase(hex)) {
                    listModel.addElement(row[0]);
                }
            }

            JList<String> list = new JList<>(listModel);
            String finalHex = hex;
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    JList list = (JList) evt.getSource();
                    if (evt.getClickCount() == 2) {
                        // Double-click detected
                        int index = list.locationToIndex(evt.getPoint());
                        String selectedHoldingName = listModel.get(index);
                        showSingleHoldingData(selectedHoldingName, finalHex, point);
                    }
                }
            });

            holdingListScrollPane = new JScrollPane(list); // Store the list in the instance variable

            // Adjusting the position of the list by 100 pixels
            Point adjustedPoint = new Point(point.x + 200, point.y + 000);

            listWindow = showInNewFrame(holdingListScrollPane, adjustedPoint);
            listWindow.setAlwaysOnTop(true); // Set the list window to be always on top
        }

        private void hideHoldingList() {
            if (listWindow != null) {
                listWindow.dispose(); // Close the list window
                listWindow = null; // Reset the reference to the list window
            }

            // Close the popup holding windows
            for (JFrame frame : holdingWindows.values()) {
                frame.dispose();
            }
            holdingWindows.clear();
        }

        private void showSingleHoldingData(String holdingName, String hex, Point point) {
            for (String[] row : holdingData) {
                String csvHex = row[5].trim();
                csvHex = csvHex.replaceFirst("^0+(?!$)", "");
                hex = hex.replaceFirst("^0+(?!$)", "");

                if (csvHex.equalsIgnoreCase(hex) && row[0].equals(holdingName)) {
                    JTextArea textArea = new JTextArea("Holding Data: \n"
                            + "Holding Name: " + row[0] + "\n"
                            + "Trade Good: " + row[1] + "\n"
                            + "Holding Occupied: " + row[2] + "\n"
                            + "Total Value of Holding Production: " + row[3] + "\n"
                            + "Holdings Owner: " + row[4] + "\n"
                            + "Province Location Hex ID: " + row[5] + "\n"
                            + "Province Name: " + row[6] + "\n"
                            + "Province Owner: " + row[7]);
                    textArea.setEditable(false);

                    // Adjust the position of the window by 100 pixels
                    Point adjustedPoint = new Point(point.x + 320, point.y);

                    JFrame frame = showInNewFrame(new JScrollPane(textArea), adjustedPoint);
                    holdingWindows.put(row[0], frame);
                    frame.setAlwaysOnTop(true); // Set the frame to be always on top
                    break;

                }
            }
        }


        private JFrame showInNewFrame(Component component, Point point) {
            JFrame frame = new JFrame();
            frame.add(component);
            frame.pack();
            frame.setLocation(point);
            frame.setVisible(true);
            return frame;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(baseImage, 0, 0, this);
            g.drawImage(overlayImage, 0, 0, this);
        }
    }
}
