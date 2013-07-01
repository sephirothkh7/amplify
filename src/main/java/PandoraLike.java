import org.apache.commons.io.IOUtils;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;

public class PandoraLike {

    private static final int MAX_PAGES = 100;

    private static final String stationsLink = "http://feeds.pandora.com/feeds/people/username/stations.xml";
    private static final String appleSearch  = "https://itunes.apple.com/search?term=theory-of-a-deadman&entity=album";

    private String username;
    private Set<String> stations;
    private Set<LikeInfo> likes;

    public PandoraLike(String username) {
        this.username = username;
        this.stations = new HashSet<String>();
        this.likes = new HashSet<LikeInfo>();
    }

    private void initStations() {
        URL stationsURL = null;
        try {
            stationsURL = new URL(stationsLink.replace("username", username));

            SAXReader reader = new SAXReader();
            org.dom4j.Document document = reader.read(stationsURL);

            List<Node> stationCodes = document.selectNodes( "//pandora:stationCode" );
            Set<String> stations = new HashSet<String>();

            Iterator<Node> it = stationCodes.iterator();
            while (it.hasNext()) {
                Node stationCode = it.next();

                if (stationCode.getText().startsWith("sh")) {
                    stations.add(stationCode.getText().replaceAll("sh", ""));
                }
            }

            this.stations = stations;
        } catch (Exception e) {

        }


    }

    private void initLikes() {

        Set<LikeInfo> likes = new HashSet<LikeInfo>();

        try {
            for (String station : stations) {

                for (int i = 0; i < MAX_PAGES; i++) {

                    Document doc =
                            Jsoup.connect("http://www.pandora.com/content/" +
                                    "station_track_thumbs?" +
                                    "stationId=" + station +
                                    "&posFeedbackStartIndex=" + (i * 5)).get();
                    for (Element e : doc.select("h3>a:first-of-type")) {
                        String[] info = e.attr("href").split("/");

                        likes.add(new LikeInfo()
                                .withTitle(info[3])
                                .withArtist(info[1])
                                .withAlbum(info[2]));
                    }
                }
            }
        } catch (Exception e) {

        }

        this.likes = likes;
    }

    private void initLikeArt() {


    }

    public void readLikesFromFile(File file, boolean withArt) {
        try {
            FileInputStream in = new FileInputStream(file);
            List<String> lines = IOUtils.readLines(in);

            for (String line : lines) {
                String[] info = line.split("/");
                LikeInfo like = new LikeInfo();
                like = like.withTitle(info[3])
                           .withAlbum(info[2])
                           .withArtist(info[1]);
                if (withArt) {
                    like = like.withArt(info[4]);
                }

                likes.add(like);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeLikesToFile(File file, boolean withArt) {

        try {
            FileOutputStream out = new FileOutputStream(file);

            if (withArt) {
                IOUtils.writeLines(likes, "\n", out);
            }

            else {
                for (LikeInfo like : likes) {
                    IOUtils.write(like.toStringNoArt() + "\n", out);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String transform(String string) {

        return string
                .replaceAll("\ba", "")
                .replaceAll("\ban", "")
                .replaceAll("\bthe", "")
                .replaceAll("-explicit/", "")
                .replaceAll("-radio-single/", "");
    }

    public static String transformExplicit(String string) {
        return string.replaceAll("-explicit", "");
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getStations() {
        return stations;
    }

    public Set<LikeInfo> getLikes() {
        return likes;
    }
}
