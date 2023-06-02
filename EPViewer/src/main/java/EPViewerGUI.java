import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;


public class EPViewerGUI {
    private static JFrame transparencyFrame = null;
    private static JFrame dataImportWindow;
    public static boolean provinceDataLoaded=false;
    public static JSlider zoomSlider; // Adding here

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void createAndShowGUI() throws IOException {

        JFrame frame = new JFrame("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        displayHandler.overlayImages.put(19, ImageIO.read(new File("maps/map" + 19 + ".png")));

        try {
            displayHandler.baseImage = ImageIO.read(new File("maps/input2.png"));
            displayHandler.regionsImage = ImageIO.read(new File("maps/regions.png"));
            dataManager.csvData = new CSVReader(new InputStreamReader(new FileInputStream("data/input3.csv"), "UTF-8")).readAll();
            dataManager.holdingData = new CSVReader(new InputStreamReader(new FileInputStream("data/input4.csv"), "UTF-8")).readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            // Generate the overlay image
            BufferedImage holdingsImage = mapGenerator.heatmapFromCSV(dataManager.holdingData, displayHandler.baseImage);
            // Save the overlay image to a temporary file
            File tempHoldingsFile = File.createTempFile("holdingsImage", ".png");
            tempHoldingsFile.deleteOnExit();
            mapGenerator.holdingsMapPath = tempHoldingsFile.getAbsolutePath();
            System.out.println("Generated file path: " + mapGenerator.holdingsMapPath);
            ImageIO.write(holdingsImage, "png", tempHoldingsFile);
        } catch (IOException e ) {
            e.printStackTrace();
            System.exit(1);
        }



        displayHandler.mapDisplay mapDisplay = new displayHandler.mapDisplay(displayHandler.baseImage, displayHandler.overlayImages.get(19));

        JScrollPane scrollPane = new JScrollPane(mapDisplay);
        JPanel controlsPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        NoneSelectedButtonGroup buttonGroup = new NoneSelectedButtonGroup();
        JRadioButton provincesButton = new JRadioButton("Provinces");
        provincesButton.addActionListener(e -> {
            mapDisplay.changeRegionsOrProvincesImage(provincesButton.isSelected() ? displayHandler.baseImage : null);
            if (provincesButton.isSelected()) {
                createTransparencyControl(provincesButton, "Provinces", mapDisplay);
            }
        });
        buttonGroup.add(provincesButton);
        buttonPanel.add(provincesButton);

        JRadioButton regionsButton = new JRadioButton("Regions");
        regionsButton.addActionListener(e -> {
            mapDisplay.changeRegionsOrProvincesImage(regionsButton.isSelected() ? displayHandler.regionsImage : null);
            if (regionsButton.isSelected()) {
                createTransparencyControl(regionsButton, "Regions", mapDisplay);
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
                            dataManager.extraData = new CSVReader(new FileReader(fileChooser.getSelectedFile())).readAll();
                            // Generate the overlay image
                            BufferedImage overlayImage = mapGenerator.imageFromCSV(dataManager.extraData, displayHandler.baseImage);
                            // Save the overlay image to a temporary file
                            File tempFile = File.createTempFile("taxoverlay", ".png");
                            tempFile.deleteOnExit();
                            String tempFilePath = tempFile.getAbsolutePath();
                            mapGenerator.taxIncomeMapPath=tempFilePath;
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
                        taxIncomeMap = ImageIO.read(new File(mapGenerator.taxIncomeMapPath));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    mapDisplay.changeRegionsOrProvincesImage(taxIncomeButton.isSelected() ? taxIncomeMap : null);
                    if (taxIncomeButton.isSelected()) {
                        createTransparencyControl(taxIncomeButton, "Tax Income", mapDisplay);
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
                        holdingsMap = ImageIO.read(new File(mapGenerator.holdingsMapPath));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    mapDisplay.changeRegionsOrProvincesImage(holdingsButton.isSelected() ? holdingsMap : null);
                    if (holdingsButton.isSelected()) {
                        createTransparencyControl(holdingsButton, "Tax Income", mapDisplay);
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
                        // Check if the transparency frame is visible and close it
                        if (transparencyFrame != null && transparencyFrame.isVisible() && (holdingsButton.isSelected() || taxIncomeButton.isSelected()) ) {
                            transparencyFrame.dispose();
                        }
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
        yearSlider.addChangeListener(e -> {
            int year = yearSlider.getValue();
            BufferedImage image = displayHandler.overlayImages.get(year);
            if (image == null) {
                try {
                    image = ImageIO.read(new File("maps/map" + year + ".png"));
                    displayHandler.overlayImages.put(year, image);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            mapDisplay.changeOverlayImage(image);
        });

        JButton openBrowserButton = new JButton("Open Browser");
        openBrowserButton.addActionListener(e -> {
            int year = 1500 + yearSlider.getValue();
            recapBrowser.openRecapBrowser(year);
        });
        buttonPanel.add(openBrowserButton);

        // This will be the new bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        // This will be the new zoom slider panel
        JPanel zoomPanel = new JPanel();
        zoomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        zoomSlider = new JSlider(JSlider.HORIZONTAL, 25, 300, 100);
        zoomSlider.setPreferredSize(new Dimension(200, 50));
        zoomSlider.setMajorTickSpacing(25);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTableZoom = new Hashtable<>();
        JLabel label25 = new JLabel("25%");
        label25.setFont(new Font("Arial", Font.BOLD, 10));
        labelTableZoom.put(25, label25);
        JLabel label100 = new JLabel("100%");
        label100.setFont(new Font("Arial", Font.BOLD, 10));
        labelTableZoom.put(100, label100);
        JLabel label300 = new JLabel("300%");
        label300.setFont(new Font("Arial", Font.BOLD, 10));
        labelTableZoom.put(300, label300);
        zoomSlider.setLabelTable(labelTableZoom);
        zoomPanel.setVisible(false);

        zoomPanel.add(zoomSlider);

        zoomSlider.addChangeListener(e -> {
            // Get the visible rectangle
            Rectangle visibleRect = scrollPane.getViewport().getViewRect();
            // Calculate the center
            Point viewCenter = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
            mapDisplay.zoomCenter = viewCenter;
            mapDisplay.scale = zoomSlider.getValue() / 100.0;
            mapDisplay.repaint();
        });


        bottomPanel.add(zoomPanel, BorderLayout.WEST);

        // Create the button
        ImageIcon icon = new ImageIcon("EPViewer/magnifying_glass_4-0.png");
        JButton expandButton = new JButton(icon);
        buttonPanel.add(expandButton);

        // Initially set bottom panel height to minimum (only zoom slider visible)
        int minBottomPanelHeight = 0;
        int maxBottomPanelHeight = 50;  // Set the maximum height according to your needs

        bottomPanel.setPreferredSize(new Dimension(frame.getWidth(), minBottomPanelHeight));

        expandButton.addActionListener(e -> {
            zoomPanel.setVisible(true);
            int startHeight = bottomPanel.getHeight();
            int targetHeight = startHeight == minBottomPanelHeight ? maxBottomPanelHeight : minBottomPanelHeight;

            Timer timer = new Timer(2, null);
            timer.setRepeats(true);
            timer.addActionListener(evt -> {
                int newHeight = bottomPanel.getHeight() + (targetHeight - startHeight) / 10;
                if ((startHeight < targetHeight && newHeight >= targetHeight) || (startHeight > targetHeight && newHeight <= targetHeight)) {
                    newHeight = targetHeight;
                    timer.stop();
                }
                bottomPanel.setPreferredSize(new Dimension(frame.getWidth(), newHeight));
                bottomPanel.revalidate();
            });
            timer.start();
        });


        yearSlider.setBorder(new EmptyBorder(0, 0, 0, 50));

        controlsPanel.add(buttonPanel, BorderLayout.WEST);
        controlsPanel.add(yearSlider, BorderLayout.EAST);

        frame.add(controlsPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setSize(1200, 800);
        frame.setVisible(true);
    }

    private static void createTransparencyControl(JRadioButton button, String title, displayHandler.mapDisplay mapDisplay) {
        if (transparencyFrame == null) {
            transparencyFrame = new JFrame();
            transparencyFrame.setSize(200, 100);
        }
        JLabel titleLabel = new JLabel(title + " Transparency");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        JSlider transparencySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (100* displayHandler.opacity));
        transparencySlider.setMajorTickSpacing(10);
        transparencySlider.setPaintTicks(true);
        transparencySlider.addChangeListener(e -> {
            displayHandler.opacity = transparencySlider.getValue() / 100.0f;
            mapDisplay.repaint();  // This line is added to repaint the imagePanel when the slider changes
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
                transparencyFrame.dispose();
            }
        }
    }

}
