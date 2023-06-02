import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;


public class EPViewerGUI {
    public static boolean provinceDataLoaded=false;
    public static JSlider zoomSlider; // Adding here

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void createAndShowGUI() throws IOException {

        JFrame frame = new JFrame("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JRadioButton holdingsButton = new JRadioButton("Holdings Distribution");
        displayHandler.overlayImages.put(19, ImageIO.read(new File("maps/map" + 19 + ".png")));

        try {
            displayHandler.baseImage = ImageIO.read(new File("maps/input2.png"));
            displayHandler.regionsImage = ImageIO.read(new File("maps/regions.png"));
            dataManager.csvData = new CSVReader(new InputStreamReader(new FileInputStream("data/input3.csv"), StandardCharsets.UTF_8)).readAll();
            dataManager.holdingData = new CSVReader(new InputStreamReader(new FileInputStream("data/input4.csv"), StandardCharsets.UTF_8)).readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Can't find input file - " + e.getMessage());
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

        JRadioButton taxIncomeButton = new JRadioButton("Tax Income");
        taxIncomeButton.setEnabled(false);

        displayHandler.mapDisplay mapDisplay = new displayHandler.mapDisplay(displayHandler.baseImage, displayHandler.overlayImages.get(19));

        JScrollPane scrollPane = new JScrollPane(mapDisplay);
        JPanel controlsPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        NoneSelectedButtonGroup buttonGroup = new NoneSelectedButtonGroup();
        buttonGroup.add(holdingsButton);
        buttonGroup.add(taxIncomeButton);


        taxIncomeButton.addActionListener(f -> {
            BufferedImage taxIncomeMap = null;
            try {
                taxIncomeMap = ImageIO.read(new File(mapGenerator.taxIncomeMapPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mapDisplay.changeRegionsOrProvincesImage(taxIncomeButton.isSelected() ? taxIncomeMap : null);
            if (taxIncomeButton.isSelected()) {
                buttonGroup.createTransparencyControl(taxIncomeButton, "Tax Income", mapDisplay);

            }
        });

        JRadioButton provincesButton = new JRadioButton("Provinces");
        provincesButton.addActionListener(e -> {
            mapDisplay.changeRegionsOrProvincesImage(provincesButton.isSelected() ? displayHandler.baseImage : null);
            if (provincesButton.isSelected()) {
                buttonGroup.createTransparencyControl(provincesButton, "Provinces", mapDisplay);
            }
        });

        buttonPanel.add(provincesButton);

        JRadioButton regionsButton = new JRadioButton("Regions");
        regionsButton.addActionListener(e -> {
            mapDisplay.changeRegionsOrProvincesImage(regionsButton.isSelected() ? displayHandler.regionsImage : null);
            if (regionsButton.isSelected()) {
                buttonGroup.createTransparencyControl(regionsButton, "Regions", mapDisplay);
            }

        });

        JButton newWindowButton = new JButton("Import Data");
        newWindowButton.addActionListener(e -> {

                JLabel confirmationLabel = new JLabel();

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

        buttonGroup.add(provincesButton);
        buttonGroup.add(regionsButton);

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

            // Calculate the center point of the visible rectangle
            int centerX = visibleRect.x + visibleRect.width / 2;
            int centerY = visibleRect.y + visibleRect.height / 2;

            // Convert the center point to the screen's coordinate system
            Point screenCenter = new Point(centerX, centerY);
            SwingUtilities.convertPointToScreen(screenCenter, scrollPane);

            // Update the zoomCenter and scale in the mapDisplay
            mapDisplay.zoomCenter = screenCenter;
            mapDisplay.scale = zoomSlider.getValue() / 100.0;

            // Update the preferred size of mapDisplay
            int w = (int)(mapDisplay.baseImage.getWidth() * mapDisplay.scale);
            int h = (int)(mapDisplay.baseImage.getHeight() * mapDisplay.scale);
            mapDisplay.setPreferredSize(new Dimension(w, h));

            // Notify the JScrollPane of the changes
            scrollPane.revalidate();
            scrollPane.repaint();

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

        JPanel mapModesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mapModesPanel.add(provincesButton);
        mapModesPanel.add(regionsButton);
        mapModesPanel.add(holdingsButton);
        mapModesPanel.add(taxIncomeButton);
        mapModesPanel.setVisible(false);

        holdingsButton.addActionListener(e -> {
            BufferedImage holdingsImage = null;
            try {
                holdingsImage = ImageIO.read(new File(mapGenerator.holdingsMapPath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mapDisplay.changeRegionsOrProvincesImage(holdingsButton.isSelected() ? holdingsImage : null);
            if (holdingsButton.isSelected()) {
                buttonGroup.createTransparencyControl(holdingsButton, "Holdings", mapDisplay);
            }
        });


        bottomPanel.add(mapModesPanel, BorderLayout.CENTER);
        bottomPanel.add(zoomPanel, BorderLayout.WEST);

        JButton mapModesButton = new JButton("Map Modes");
        buttonPanel.add(mapModesButton);

        bottomPanel.setPreferredSize(new Dimension(frame.getWidth(), minBottomPanelHeight));

        expandButton.addActionListener(e -> {
            zoomPanel.setVisible(true);
            mapModesPanel.setVisible(false);
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



        mapModesButton.addActionListener(e -> {
            mapModesPanel.setVisible(!mapModesPanel.isVisible());
            zoomPanel.setVisible(false);

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

    static class NoneSelectedButtonGroup extends ButtonGroup {
        private Map<JRadioButton, JFrame> buttonFrames = new HashMap<>();
        private Map<JRadioButton, JSlider> buttonSliders = new HashMap<>();

        @Override
        public void setSelected(ButtonModel model, boolean selected) {
            if (selected) {
                closeAllSliders();
                super.setSelected(model, selected);
            } else {
                closeAllSliders();
                clearSelection();
            }
        }

        private void closeAllSliders() {
            for (JFrame transparencyFrame : buttonFrames.values()) {
                if (transparencyFrame != null) {
                    transparencyFrame.dispose();
                }
            }
            buttonFrames.clear();
            buttonSliders.clear();
        }

        public void createTransparencyControl(JRadioButton button, String title, displayHandler.mapDisplay mapDisplay) {
            JFrame transparencyFrame = new JFrame();
            transparencyFrame.setSize(200, 100);
            buttonFrames.put(button, transparencyFrame);

            JSlider transparencySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (100 * displayHandler.opacity));
            transparencySlider.setMajorTickSpacing(10);
            transparencySlider.setPaintTicks(true);
            transparencySlider.addChangeListener(e -> {
                displayHandler.opacity = transparencySlider.getValue() / 100.0f;
                mapDisplay.repaint();
            });
            buttonSliders.put(button, transparencySlider);
            transparencyFrame.add(transparencySlider, BorderLayout.CENTER);

            JLabel titleLabel = new JLabel(title + " Transparency");
            titleLabel.setHorizontalAlignment(JLabel.CENTER);

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
    }

}
