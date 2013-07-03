import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PandoraLike {

    private static final int MAX_PAGES = 100;
    private static final String STATIONS_LINK = "http://feeds.pandora.com/feeds/people/username/stations.xml";
    private static final String SEARCH = "https://itunes.apple.com/search?term=TERM&entity=ENTITY&media=MEDIA";
    private static final String SEARCH_ALBUM_BY_TITLE = "https://itunes.apple.com/search?term=TITLE&entity=album";
    private static final String SEARCH_ALBUM_BY_ALBUM = "https://itunes.apple.com/search?term=ALBUM&entity=album";

    private String username;
    private Set<String> stations;
    private Set<LikeInfo> likes;

    public PandoraLike(String username) {
        this.username = username;
        this.stations = new HashSet<String>();
        this.likes = new HashSet<LikeInfo>();
    }

    public void init() {
        initStations();
        initLikes();
        initLikeArt();
    }

    public void initFromFile(File file) {
        readLikesFromFile(file);
        initLikeArt();
    }

    public void initFromFileWithArt(File file) {
        readLikesFromFile(file);
    }

    private void initStations() {
        URL stationsURL = null;
        try {
            stationsURL = new URL(STATIONS_LINK.replace("username", username));

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

        for (LikeInfo like : likes) {
            try {
                String art = null;

                String track = Jsoup.connect(SEARCH
                        .replaceAll("TERM", like.getTitle())
                        .replaceAll("ENTITY", "musicTrack")
                        .replaceAll("MEDIA", "music"))
                        .get().body().text();
                JSONObject trackJson = new JSONObject(track);
                JSONArray trackResults = trackJson.getJSONArray("results");

                for (int i = 0; i < trackResults.length(); i++) {
                    JSONObject trackResult = trackResults.getJSONObject(i);

                    try {
                        String tArtist = transform(trackResult.getString("artistViewUrl"));
                        String tAlbum = transform(trackResult.getString("collectionViewUrl"));
                        String tTitle = transform(trackResult.getString("trackViewUrl"));

                        String lArtist = transform(like.getArtist());
                        String lAlbum  = transform(like.getAlbum());
                        String lTitle = transform(like.getTitle());

                        if (tArtist.contains(like.getArtist()) ||
                                tAlbum.contains(like.getAlbum()) ||
                                transform2(tArtist).contains(lArtist) ||
                                transform2(tAlbum).contains(lAlbum) ||
                                transform2(tArtist).contains(transform2(lArtist)) ||
                                transform2(tAlbum).contains(transform2(lAlbum))) {

                            art = trackResult.getString("artworkUrl60");
                            like.setRealAlbum(trackResult.getString("collectionName"));
                            like.setRealArtist(trackResult.getString("artistName"));
                            like.setRealTitle(trackResult.getString("trackName"));
                            break;
                        }
                    } catch (Exception e) {

                    }
                }

                if (art == null) {

                    String album = Jsoup.connect(SEARCH_ALBUM_BY_ALBUM.replaceAll("ALBUM", like.getAlbum())).get().body().text();
                    JSONObject albumJson = new JSONObject(album);
                    JSONArray albumResults = albumJson.getJSONArray("results");

                    for (int i = 0; i < albumResults.length(); i++) {
                        JSONObject albumResult = albumResults.getJSONObject(i);

                        try {
                            String tArtist = transform(albumResult.getString("artistViewUrl"));
                            String lArtist = transform(like.getArtist());

                            if (tArtist.contains(like.getArtist()) ||
                                    transform2(tArtist).contains(lArtist) ||
                                    transform2(tArtist).contains(transform2(lArtist))) {
                                art = albumResult.getString("artworkUrl60");
                                like.setRealAlbum(albumResult.getString("collectionName"));
                                like.setRealArtist(albumResult.getString("artistName"));
                                break;
                            }
                        } catch (Exception e) {

                        }
                    }
                }

                if (art == null) {

                    String title = Jsoup.connect(SEARCH_ALBUM_BY_TITLE.replaceAll("TITLE", like.getTitle())).get().body().text();
                    JSONObject titleJson = new JSONObject(title);
                    JSONArray titleResults = titleJson.getJSONArray("results");

                    for (int i = 0; i < titleResults.length(); i++) {
                        JSONObject titleResult = titleResults.getJSONObject(i);

                        try {
                            String tArtist = transform(titleResult.getString("artistViewUrl"));
                            String tAlbum  = transform(titleResult.getString("collectionViewUrl"));
                            String lArtist = transform(like.getArtist());
                            String lAlbum  = transform(like.getAlbum());

                            if (tArtist.contains(like.getArtist()) ||
                                    tAlbum.contains(like.getAlbum()) ||
                                    transform2(tArtist).contains(lArtist) ||
                                    transform2(tAlbum).contains(lAlbum) ||
                                    transform2(tArtist).contains(transform2(lArtist)) ||
                                    transform2(tAlbum).contains(transform2(lAlbum))) {
                                art = titleResult.getString("artworkUrl60");
                                like.setRealAlbum(titleResult.getString("collectionName"));
                                like.setRealArtist(titleResult.getString("artistName"));
                                break;
                            }
                        } catch (Exception e) {

                        }
                    }
                }

                like.setArt(art);

            } catch (Exception e) {

            }
        }
    }

    public void readLikesFromFile(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Likes.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            this.likes = ((Likes) unmarshaller.unmarshal(file)).getLikes();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeLikesToFile(File file) {

        try {
            if (file.exists()) {
                file.delete();
            }

            JAXBContext jaxbContext = JAXBContext.newInstance(Likes.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);


            jaxbMarshaller.marshal(new Likes().withLikes(getLikes()), file);

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

    private String transform2(String string) {
        Pattern p = Pattern.compile("\\b\\d+");
        Matcher m = p.matcher(string);
        String result = string;
        while (m.find()) {
            result = result.replaceFirst(m.group(), NumberToWords.processor.getName(m.group()));
        }
        return result == null ? string : result;
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
