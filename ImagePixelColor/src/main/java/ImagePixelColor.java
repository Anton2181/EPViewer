import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImagePixelColor {
    private static List<String[]> csvData;
    public static List<String[]> extraData;
    private static List<String[]> holdingData;
    private static BufferedImage baseImage;
    private static BufferedImage regionsImage;
    private static HashMap<Integer, BufferedImage> overlayImages = new HashMap<>();
    public static float opacity = 1.0f;
    private static boolean isProvinces = false;
    private static JFrame transparencyFrame = null;
    private static JFrame dataImportWindow;
    public static boolean provinceDataLoaded=false;
    public static String taxIncomeMapPath = "";
    public static String holdingsMapPath = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    // Generate a color for a value in the range [min, max]
    public static Color colorForValue(double value, double min, double max) {
        float ratio = (float) ((value - min) / (max - min));
        int red = (int) (255 * ratio);
        int green = (int) (255 * (1 - ratio));
        return new Color(red, green, 0);
    }

    // Generate an overlay image based on the tax income data in a CSV file
    public static BufferedImage imageFromCSV(List<String[]> csvData, BufferedImage baseImage) throws IOException {
        double minIncome = Double.MAX_VALUE;
        double maxIncome = Double.MIN_VALUE;
        Map<Color, Double> incomeByColor = new HashMap<>();

        // Skip header line
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);

            // Assuming the color hex string is in column 0 and the income is in column 2
            String hexString = row[0];
            String formattedHexString = String.format("#%6s", hexString).replace(' ', '0');
            Color color = Color.decode(formattedHexString);
            double income = Double.parseDouble(row[2]);
            incomeByColor.put(color, income);

            minIncome = Math.min(minIncome, income);
            maxIncome = Math.max(maxIncome, income);
        }

        BufferedImage resultImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < baseImage.getWidth(); x++) {
            for (int y = 0; y < baseImage.getHeight(); y++) {
                Color baseColor = new Color(baseImage.getRGB(x, y));
                if (incomeByColor.containsKey(baseColor)) {
                    double income = incomeByColor.get(baseColor);
                    Color newColor = colorForValue(income, maxIncome, minIncome);
                    resultImage.setRGB(x, y, newColor.getRGB());
                }
            }
        }

        return resultImage;
    }



    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            baseImage = ImageIO.read(new File("input2.png"));
            regionsImage = ImageIO.read(new File("regions.png"));
            for (int i = 0; i <= 19; i++) {
                overlayImages.put(i, ImageIO.read(new File("map" + i + ".png")));
            }
            csvData = new CSVReader(new InputStreamReader(new FileInputStream("input3.csv"), "UTF-8")).readAll();
            holdingData = new CSVReader(new InputStreamReader(new FileInputStream("input4.csv"), "UTF-8")).readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ImagePanel imagePanel = new ImagePanel(baseImage, overlayImages.get(19));

        JPanel controlsPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        NoneSelectedButtonGroup buttonGroup = new NoneSelectedButtonGroup();
        JRadioButton provincesButton = new JRadioButton("Provinces");
        provincesButton.addActionListener(e -> {
            imagePanel.changeRegionsOrProvincesImage(provincesButton.isSelected() ? baseImage : null);
            if (provincesButton.isSelected()) {
                createTransparencyControl(provincesButton, "Provinces", imagePanel);
            }
        });
        buttonGroup.add(provincesButton);
        buttonPanel.add(provincesButton);

        JRadioButton regionsButton = new JRadioButton("Regions");
        regionsButton.addActionListener(e -> {
            imagePanel.changeRegionsOrProvincesImage(regionsButton.isSelected() ? regionsImage : null);
            if (regionsButton.isSelected()) {
                createTransparencyControl(regionsButton, "Regions", imagePanel);
            }
        });
        buttonGroup.add(regionsButton);
        buttonPanel.add(regionsButton);

        JButton newWindowButton = new JButton("Import Data");
        newWindowButton.addActionListener(e -> {
            if (dataImportWindow == null || !dataImportWindow.isVisible()) {
                dataImportWindow = new JFrame("Import Data");
                dataImportWindow.setSize(300, 400);
                Point p = newWindowButton.getLocationOnScreen();
                dataImportWindow.setLocation(p.x, p.y + 50);
                dataImportWindow.setAlwaysOnTop(true);

                Box box = Box.createVerticalBox();

                JButton fileChooserButton = new JButton("Select CSV");
                JLabel confirmationLabel = new JLabel();

                fileChooserButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                JRadioButton taxIncomeButton = new JRadioButton("Tax Income");

                taxIncomeButton.setEnabled(false);
                buttonGroup.add(taxIncomeButton);
                taxIncomeButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                fileChooserButton.addActionListener(event -> {
                    JFileChooser fileChooser = new JFileChooser();
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
                    fileChooser.setFileFilter(filter);
                    int returnVal = fileChooser.showOpenDialog(null);
                    confirmationLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            extraData = new CSVReader(new FileReader(fileChooser.getSelectedFile())).readAll();
                            // Generate the overlay image
                            BufferedImage overlayImage = imageFromCSV(extraData, baseImage);
                            // Save the overlay image to a temporary file
                            File tempFile = File.createTempFile("taxoverlay", ".png");
                            tempFile.deleteOnExit();
                            String tempFilePath = tempFile.getAbsolutePath();
                            taxIncomeMapPath=tempFilePath;
                            System.out.println("Generated file path: " + tempFilePath);
                            ImageIO.write(overlayImage, "png", tempFile);

                            confirmationLabel.setText("File loaded successfully!");
                            provinceDataLoaded = true;
                            taxIncomeButton.setEnabled(true);
                        } catch (IOException | CsvException ex) {
                            ex.printStackTrace();
                            confirmationLabel.setText("Failed to load file!");
                            taxIncomeButton.setEnabled(false);
                        }
                    }
                });
                taxIncomeButton.addActionListener(f -> {
                    BufferedImage taxIncomeMap = null;
                    try {
                        taxIncomeMap = ImageIO.read(new File(taxIncomeMapPath));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    imagePanel.changeRegionsOrProvincesImage(taxIncomeButton.isSelected() ? taxIncomeMap : null);
                    if (taxIncomeButton.isSelected()) {
                        createTransparencyControl(taxIncomeButton, "Tax Income", imagePanel);
                    }
                });

                box.add(Box.createVerticalStrut(50));
                box.add(fileChooserButton);
                box.add(Box.createVerticalStrut(50));
                box.add(confirmationLabel);

                JPanel radioButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

                JRadioButton holdingsButton = new JRadioButton("Holdings Distribution");
                holdingsButton.addActionListener(f -> {
                    BufferedImage holdingsMap = null;
                    try {
                        holdingsMap = ImageIO.read(new File(holdingsMapPath));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    imagePanel.changeRegionsOrProvincesImage(holdingsButton.isSelected() ? holdingsMap : null);
                    if (holdingsButton.isSelected()) {
                        createTransparencyControl(holdingsButton, "Tax Income", imagePanel);
                    }
                });
                buttonGroup.add(holdingsButton);

                radioButtonPanel.add(taxIncomeButton);
                radioButtonPanel.add(holdingsButton);

                box.add(radioButtonPanel);

                dataImportWindow.add(box);

                dataImportWindow.setVisible(true);
                dataImportWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        dataImportWindow = null;
                    }
                });
            }
        });

        buttonPanel.add(newWindowButton);

        JSlider yearSlider = new JSlider(JSlider.HORIZONTAL, 0, 19, 19);
        yearSlider.setPreferredSize(new Dimension(600, 50));
        yearSlider.setMajorTickSpacing(1);
        yearSlider.setPaintTicks(true);
        yearSlider.setSnapToTicks(true);
        yearSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = 0; i <= 19; i++) {
            JLabel label = new JLabel(String.valueOf(1500 + i));
            label.setFont(new Font("Arial", Font.BOLD, 10));
            labelTable.put(i, label);
        }
        yearSlider.setLabelTable(labelTable);
        yearSlider.addChangeListener(e -> imagePanel.changeOverlayImage(overlayImages.get(yearSlider.getValue())));
        yearSlider.setBorder(new EmptyBorder(0, 0, 0, 50));

        controlsPanel.add(buttonPanel, BorderLayout.WEST);
        controlsPanel.add(yearSlider, BorderLayout.EAST);

        frame.add(controlsPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(imagePanel), BorderLayout.CENTER);

        frame.setSize(1200, 800);
        frame.setVisible(true);
    }

    private static void createTransparencyControl(JRadioButton button, String title, ImagePanel imagePanel) {
        if (transparencyFrame == null) {
            transparencyFrame = new JFrame();
            transparencyFrame.setSize(200, 100);
        }
        JLabel titleLabel = new JLabel(title + " Transparency");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        JSlider transparencySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (100*opacity));
        transparencySlider.setMajorTickSpacing(10);
        transparencySlider.setPaintTicks(true);
        transparencySlider.addChangeListener(e -> {
            opacity = transparencySlider.getValue() / 100.0f;
            imagePanel.repaint();  // This line is added to repaint the imagePanel when the slider changes
        });
        transparencyFrame.getContentPane().removeAll();
        transparencyFrame.setLayout(new BorderLayout());
        transparencyFrame.add(titleLabel, BorderLayout.NORTH);
        transparencyFrame.add(transparencySlider, BorderLayout.CENTER);
        Point location = button.getLocationOnScreen();
        transparencyFrame.setLocation(location.x, location.y + 50);
        transparencyFrame.setSize(200, 100);
        transparencyFrame.setVisible(true);
        transparencyFrame.setAlwaysOnTop(true);
    }

    static class NoneSelectedButtonGroup extends ButtonGroup {
        @Override
        public void setSelected(ButtonModel model, boolean selected) {
            if (selected) {
                super.setSelected(model, selected);
            } else {
                clearSelection();
            }
        }
    }

    static class ImagePanel extends JPanel {
        BufferedImage baseImage;
        BufferedImage overlayImage;
        BufferedImage regionsOrProvincesImage;
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

        public void changeRegionsOrProvincesImage(BufferedImage newRegionsOrProvincesImage) {
            this.regionsOrProvincesImage = newRegionsOrProvincesImage;
            repaint();
        }

        private void showMatchingData(String hex, Point point) {
            boolean extraDataInUse = false;
            for (String[] row : extraData) {
                String csvHex = row[0].trim();
                // Removing leading zeros
                csvHex = csvHex.replaceFirst("^0+(?!$)", "");
                hex = hex.replaceFirst("^0+(?!$)", "");

                if (csvHex.equalsIgnoreCase(hex)) {
                    JTextArea textArea = new JTextArea("Matching data: \n"
                            + "Hexadecimal ID: " + row[0] + "\n"
                            + "Province Name: " + row[1] + "\n"
                            + "Tax Income: " + row[2] + "\n"
                            + "Damage: " + row[3] + "\n"
                            + "Occupied: " + row[4] + "\n"
                            + "Region: " + row[5]);
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
                    extraDataInUse = true;
                    break;
                }
            }

            if (!extraDataInUse) {
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
                        return; // Exit the method once a match is found in csvData
                    }
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
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.drawImage(baseImage, 0, 0, null);
            if(overlayImage != null) {
                g2d.drawImage(overlayImage, 0, 0, null);
            }
            if (regionsOrProvincesImage != null) {
                g2d.setComposite(AlphaComposite.SrcOver.derive(opacity));
                g2d.drawImage(regionsOrProvincesImage, 0, 0, null);
            }
            g2d.dispose();
        }

    }
}
