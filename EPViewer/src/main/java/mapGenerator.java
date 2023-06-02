import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class mapGenerator {
    public static String taxIncomeMapPath = "";
    public static String holdingsMapPath = "";

    public static Color taxIncomeGradient(double value, double min, double max) {
        float ratio = (float) ((value - min) / (max - min));
        int red = (int) (255 * ratio);
        int green = (int) (255 * (1 - ratio));
        return new Color(red, green, 0);
    }

    private static Color holdingsCountGradient(int holdingsCount, int maxHoldingsCount) {
        // Define the color ranges for the heatmap
        Color[] colorRanges = {Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED};
        int maxRange = colorRanges.length - 1;

        // Calculate the index based on the holdings count relative to the maximum holdings count
        int index = (int) ((double) holdingsCount / maxHoldingsCount * maxRange);
        index = Math.min(index, maxRange);

        return colorRanges[index];
    }

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
                    Color newColor = taxIncomeGradient(income, maxIncome, minIncome);
                    resultImage.setRGB(x, y, newColor.getRGB());
                }
            }
        }

        return resultImage;
    }

    public static BufferedImage heatmapFromCSV(List<String[]> csvData, BufferedImage baseImage) {
        Map<Color, Integer> holdingsCountByColor = new HashMap<>();

        // Skip header line
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);

            // Assuming the color hex string is in column 5 (Hex ID)
            String hexString = row[5];
            String formattedHexString = String.format("#%6s", hexString).replace(' ', '0');
            Color color = Color.decode(formattedHexString);

            holdingsCountByColor.put(color, holdingsCountByColor.getOrDefault(color, 0) + 1);
        }

        BufferedImage resultImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int maxHoldingsCount = Collections.max(holdingsCountByColor.values());
        for (int x = 0; x < baseImage.getWidth(); x++) {
            for (int y = 0; y < baseImage.getHeight(); y++) {
                Color baseColor = new Color(baseImage.getRGB(x, y));
                if (holdingsCountByColor.containsKey(baseColor)) {
                    int holdingsCount = holdingsCountByColor.get(baseColor);
                    Color newColor = holdingsCountGradient(holdingsCount, maxHoldingsCount);
                    resultImage.setRGB(x, y, newColor.getRGB());
                }
            }
        }

        return resultImage;
    }
}
