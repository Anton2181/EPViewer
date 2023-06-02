import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class recapBrowser {
    private static Map<Integer, String> yearToRecapUrl = new HashMap<Integer, String>() {{
        put(1500, "https://old.reddit.com/r/empirepowers/comments/10jrlp3/meta_season_xi_recap_year_1500/");
        put(1501, "https://old.reddit.com/r/empirepowers/comments/10phc4y/meta_season_xi_recap_year_1501/");
        put(1502, "https://old.reddit.com/r/empirepowers/comments/10vlrcf/meta_season_xi_recap_year_1502/");
        put(1503, "https://old.reddit.com/r/empirepowers/comments/111mogl/meta_season_xi_recap_year_1503/");
        put(1504, "https://old.reddit.com/r/empirepowers/comments/117nybf/meta_season_xi_recap_year_1504/");
        put(1505, "https://old.reddit.com/r/empirepowers/comments/11dsjg2/meta_season_xi_recap_year_1505/");
        put(1506, "https://old.reddit.com/r/empirepowers/comments/11kxynj/meta_season_xi_recap_year_1506/");
        put(1507, "https://old.reddit.com/r/empirepowers/comments/11rb0ap/meta_season_xi_recap_year_1507/");
        put(1508, "https://www.reddit.com/r/empirepowers/comments/11xnans/meta_season_xi_recap_year_1508/?");
        put(1509, "https://old.reddit.com/r/empirepowers/comments/12527eo/meta_season_xi_recap_year_1509/");
        put(1510, "https://www.reddit.com/r/empirepowers/comments/12bxc35/meta_season_xi_recap_year_1510/");
        put(1511, "https://www.reddit.com/r/empirepowers/comments/12iz61e/meta_season_xi_recap_year_1511/?");
        put(1512, "https://www.reddit.com/r/empirepowers/comments/12rsa8p/meta_season_xi_recap_year_1512/");
        put(1513, "https://www.reddit.com/r/empirepowers/comments/12zl1ef/meta_season_xi_recap_year_1513/?");
        put(1514, "https://old.reddit.com/r/empirepowers/comments/138fnc4/meta_season_xi_recap_year_1514/");
        put(1515, "https://old.reddit.com/r/empirepowers/comments/13dsjb5/meta_season_xi_recap_year_1515/");
        put(1516, "https://old.reddit.com/r/empirepowers/comments/13kewkh/meta_season_xi_recap_year_1516/");
        put(1517, "https://www.reddit.com/r/empirepowers/comments/13sqgl8/meta_season_xi_recap_year_1517/");
        put(1518, "https://www.reddit.com/r/empirepowers/comments/13xdlfs/meta_season_xi_recap_year_1518/?");
    }};

    static void openRecapBrowser(int year) {
        if (!java.awt.Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported");
            return;
        }

        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if(desktop.isSupported(Desktop.Action.BROWSE)){
            String url = yearToRecapUrl.get(year);
            try {
                if (url != null) {
                    desktop.browse(new URI(url));
                } else {
                    System.err.println("No recap found for year: " + year);
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
