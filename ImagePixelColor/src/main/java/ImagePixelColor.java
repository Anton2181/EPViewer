import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import javax.imageio.ImageIO;

public class ImagePixelColor {
    private static List<String[]> csvData;
    private static List<String[]> holdingData;
    private static BufferedImage baseImage;
    private static HashMap<Integer, BufferedImage> overlayImages = new HashMap<>();
    private static boolean isProvinces = false;
    private static boolean isRegions = false;
    private static BufferedImage regionsImage;
    private static JRadioButton lastSelected = null;



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

        try {
            baseImage = ImageIO.read(new File("input2.png"));
            regionsImage = ImageIO.read(new File("regions.png")); // Load regions image
            for (int i = 0; i <= 19; i++) {
                overlayImages.put(i, ImageIO.read(new File("map" + i + ".png")));
            }
            csvData = new CSVReader(new FileReader("input3.csv")).readAll();
            holdingData = new CSVReader(new FileReader("input4.csv")).readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ImagePanel imagePanel = new ImagePanel(baseImage, overlayImages.get(19));

        JSlider yearSlider = new JSlider(JSlider.HORIZONTAL, 0, 19, 19);
        yearSlider.setPreferredSize(new Dimension(650, 50));
        yearSlider.setMajorTickSpacing(1);
        yearSlider.setPaintTicks(true);
        yearSlider.setSnapToTicks(true);
        yearSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = 0; i <= 19; i++) {
            JLabel label = new JLabel(String.valueOf(1500 + i));
            label.setFont(new Font("Arial", Font.BOLD, 10)); // Adjust the font size here
            labelTable.put(i, label);
        }
        yearSlider.setLabelTable(labelTable);
        yearSlider.addChangeListener(e -> {
            if (isProvinces || isRegions) {
                return;
            }
            imagePanel.changeOverlayImage(overlayImages.get(yearSlider.getValue()));
        });
        yearSlider.setBorder(new EmptyBorder(0, 0, 0, 50));

        JRadioButton provincesButton = new JRadioButton("Provinces");
        provincesButton.addActionListener(e -> {
            if (provincesButton.equals(lastSelected)) {
                provincesButton.setSelected(false);
                lastSelected = null;
                isProvinces = false;
                imagePanel.changeOverlayImage(overlayImages.get(yearSlider.getValue()));
            } else {
                lastSelected = provincesButton;
                isProvinces = true;
                isRegions = false;
                imagePanel.changeOverlayImage(baseImage);
            }
        });

        JRadioButton regionsButton = new JRadioButton("Regions");
        regionsButton.addActionListener(e -> {
            if (regionsButton.equals(lastSelected)) {
                regionsButton.setSelected(false);
                lastSelected = null;
                isRegions = false;
                imagePanel.changeOverlayImage(overlayImages.get(yearSlider.getValue()));
            } else {
                lastSelected = regionsButton;
                isRegions = true;
                isProvinces = false;
                imagePanel.changeOverlayImage(regionsImage);
            }
        });
        class NoneSelectedButtonGroup extends ButtonGroup {

            @Override
            public void setSelected(ButtonModel model, boolean selected) {
                if (selected) {
                    super.setSelected(model, selected);
                } else {
                    clearSelection();
                }
            }
        }

        NoneSelectedButtonGroup buttonGroup = new NoneSelectedButtonGroup();
        buttonGroup.add(provincesButton);
        buttonGroup.add(regionsButton);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(provincesButton);
        buttonsPanel.add(regionsButton);

        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(buttonsPanel, BorderLayout.WEST);
        controlsPanel.add(yearSlider, BorderLayout.EAST);

        frame.add(controlsPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(imagePanel), BorderLayout.CENTER);
        frame.setSize(1200, 800);
        frame.setVisible(true);
    }

    static class ImagePanel extends JPanel {
        BufferedImage baseImage;
        BufferedImage overlayImage;
        HashMap<String, JFrame> holdingWindows = new HashMap<>();
        JScrollPane holdingListScrollPane;
        JFrame listWindow;


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
        public void changeOverlayImage(BufferedImage newOverlayImage) {
            this.overlayImage = newOverlayImage;
            repaint();
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
            g.drawImage(baseImage, 0, 0, null);
            g.drawImage(overlayImage, 0, 0, null);
        }
    }
}
