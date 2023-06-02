import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class displayHandler {
    public static float opacity = 1.0f;
    static BufferedImage baseImage;
    static BufferedImage regionsImage;
    static LinkedHashMap<Integer, BufferedImage> overlayImages = new LinkedHashMap<>(25);

    static class mapDisplay extends JPanel {
        BufferedImage baseImage;
        BufferedImage overlayImage;
        BufferedImage superOverlayImage;
        HashMap<String, JFrame> holdingWindows = new HashMap<>();
        JScrollPane holdingListScrollPane;
        JFrame listWindow;

        public mapDisplay(BufferedImage baseImage, BufferedImage overlayImage) {
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

        public void changeRegionsOrProvincesImage(BufferedImage newSuperOverlayImage) {
            this.superOverlayImage = newSuperOverlayImage;
            repaint();
        }

        private void showMatchingData(String hex, Point point) {
            String[] rowData = dataManager.findMatchingData(hex);
            if (rowData != null) {
                JTextArea textArea = null;
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
                panel.add(button, BorderLayout.PAGE_END);
                panel.add(hideButton, BorderLayout.PAGE_START);

                if (dataManager.doesExtraDataContain(rowData)) {
                    textArea = new JTextArea("Matching data: \n"
                            + "Hexadecimal ID: " + rowData[0] + "\n"
                            + "Province Name: " + rowData[1] + "\n"
                            + "Tax Income: " + rowData[2] + "\n"
                            + "Damage: " + rowData[3] + "\n"
                            + "Occupied: " + rowData[4] + "\n"
                            + "Region: " + rowData[5]);
                } else {
                    textArea = new JTextArea("Matching data: \n"
                            + "Hexadecimal ID: " + rowData[1] + "\n"
                            + "Province Name: " + rowData[2] + "\n"
                            + "Province Owner: " + rowData[3] + "\n"
                            + "Region: " + rowData[4]);
                }

                textArea.setEditable(false);
                panel.add(textArea, BorderLayout.CENTER);

                JFrame frame = showInNewFrame(panel, point);
                frame.setSize(frame.getWidth() * 3 / 2, frame.getHeight());
                frame.setAlwaysOnTop(true);
            }
        }
        private void showHoldingList(String hex, Point point) {
            if (listWindow != null) {
                listWindow.dispose(); // Close the existing list window if it is already open
            }

            DefaultListModel<String> listModel = dataManager.getHoldingList(hex);

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

            // Adjusting the position of the list by 200 pixels
            Point adjustedPoint = new Point(point.x + 200, point.y);

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
            String[] row = dataManager.getSingleHoldingData(holdingName, hex);

            if (row != null) {
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

                // Adjust the position of the window by 320 pixels
                Point adjustedPoint = new Point(point.x + 320, point.y);

                JFrame frame = showInNewFrame(new JScrollPane(textArea), adjustedPoint);
                holdingWindows.put(row[0], frame);
                frame.setAlwaysOnTop(true); // Set the frame to be always on top
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
            if (superOverlayImage != null) {
                g2d.setComposite(AlphaComposite.SrcOver.derive(opacity));
                g2d.drawImage(superOverlayImage, 0, 0, null);
            }
            g2d.dispose();
        }

    }
}
