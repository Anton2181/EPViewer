import javax.swing.*;
import java.util.List;

public class dataManager {
    static List<String[]> extraData;
    static List<String[]> csvData;
    static List<String[]> holdingData;

    public static boolean doesExtraDataContain(String[] rowData) {
        if(extraData == null) {
            return false;
        } else {
            return extraData.contains(rowData);
        }
    }

    public static String[] findMatchingData(String hex) {
        if(extraData != null)
            for (String[] row : extraData) {
                String csvHex = row[0].trim();
                csvHex = csvHex.replaceFirst("^0+(?!$)", "");
                hex = hex.replaceFirst("^0+(?!$)", "");

                if (csvHex.equalsIgnoreCase(hex)) {
                    return row;
                }
            }

        for (String[] row : csvData) {
            String csvHex = row[1].trim();
            csvHex = csvHex.replaceFirst("^0+(?!$)", "");
            hex = hex.replaceFirst("^0+(?!$)", "");

            if (csvHex.equalsIgnoreCase(hex)) {
                return row;
            }
        }

        return null; // no matching data found
    }

    public static DefaultListModel<String> getHoldingList(String hex) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String[] row : holdingData) {
            String csvHex = row[5].trim();
            csvHex = csvHex.replaceFirst("^0+(?!$)", "");
            hex = hex.replaceFirst("^0+(?!$)", "");

            if (csvHex.equalsIgnoreCase(hex)) {
                listModel.addElement(row[0]);
            }
        }
        return listModel;
    }

    public static String[] getSingleHoldingData(String holdingName, String hex) {
        for (String[] row : holdingData) {
            String csvHex = row[5].trim();
            csvHex = csvHex.replaceFirst("^0+(?!$)", "");
            hex = hex.replaceFirst("^0+(?!$)", "");

            if (csvHex.equalsIgnoreCase(hex) && row[0].equals(holdingName)) {
                return row;
            }
        }
        return null;
    }
}
