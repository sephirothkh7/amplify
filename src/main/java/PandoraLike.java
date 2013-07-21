import com.google.common.base.Joiner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;

public class PandoraLike {

    private static final int MAX_PAGES = 200;
    private static final String STATIONS_LINK = "http://feeds.pandora.com/feeds/people/username/stations.xml";
    private static final String SEARCH = "https://itunes.apple.com/search?term=TERM&entity=ENTITY&media=MEDIA";
    private static final String SEARCH_ALBUM_BY_TITLE = "https://itunes.apple.com/search?term=TITLE&entity=album";
    private static final String SEARCH_ALBUM_BY_ALBUM = "https://itunes.apple.com/search?term=ALBUM&entity=album";

    private static final Logger LOG = LoggerFactory.getLogger(PandoraLike.class);

    private String username;
    private Set<String> stations;
    private Likes likes;

    private static final int POOL_SIZE = 64;
    private ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

    public PandoraLike(String username) {
        this.username = username;
        this.stations = new HashSet<String>();
        this.likes = new Likes().withLikes(new ArrayList<LikeInfo>());
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

    public void initStations() {
        URL stationsURL = null;
        try {
            stationsURL = new URL(STATIONS_LINK.replace("username", username));

            SAXReader reader = new SAXReader();
            org.dom4j.Document document = reader.read(stationsURL);

            List<Node> stationCodes = document.selectNodes( "//pandora:stationCode" );
            Set<String> stations = new HashSet<String>();

            for (Node stationCode : stationCodes) {
                if (stationCode.getText().startsWith("sh")) {
                    stations.add(stationCode.getText().replaceAll("sh", ""));
                }
            }

            this.stations = stations;
        } catch (Exception e) {

        }
    }

    public void initLikes() {

        List<Callable<String>> tasks = new ArrayList<Callable<String>>();

        for (String station : stations) {
            for (int i = 0; i < MAX_PAGES; i++) {
                tasks.add(new LikeTask(station, i));
            }
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initLikeArt() {

        List<Callable<String>> tasks = new ArrayList<Callable<String>>();

        for (LikeInfo like : likes) {
                tasks.add(new SearchTask(like));
            }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean search(String term, LikeInfo like) throws IOException, JSONException {
        JSONArray results = trackQuery(term);
        for (int i = 0; i < results.length(); i++) {
            JSONObject trackResult = results.getJSONObject(i);
            try {

                List<String> iTitleTokens = tokenize(clean(trackResult.getString("trackName")), " ");
                List<String> iArtistTokens = tokenize(clean(trackResult.getString("artistName")), " ");
                List<String> iAlbumTokens = tokenize(clean(trackResult.getString("collectionName")), " ");
                List<String> lTitleTokens = tokenize(clean(like.getTitle()), "-");
                List<String> lArtistTokens = tokenize(clean(like.getArtist()), "-");
                List<String> lAlbumTokens = tokenize(clean(like.getAlbum()), "-");

                if (fuzzyMatch(iTitleTokens, lTitleTokens) &&
                        (fuzzyMatch(iArtistTokens, lArtistTokens) ||
                                fuzzyMatch(iAlbumTokens, lAlbumTokens))) {

                    like.setArt(trackResult.getString("artworkUrl100"));
                    like.setRealAlbum(trackResult.getString("collectionName"));
                    like.setRealArtist(trackResult.getString("artistName"));
                    like.setRealTitle(trackResult.getString("trackName"));
                    return true;
                }


            } catch (Exception e) {

            }
        }

        return false;
    }

    private static String abbreviate(String string, String delim) {

        List<String> tokens = tokenize(string, delim);
        Joiner joiner = Joiner.on(delim);
        return joiner.join(tokens.subList(0, tokens.size() / 2));
    }

    private JSONArray trackQuery(String param) throws IOException, JSONException {
        String url = SEARCH
                .replaceAll("TERM", param)
                .replaceAll("ENTITY", "song")
                .replaceAll("MEDIA", "music");
        String track = Jsoup.connect(url)
                .get().body().text();
        JSONObject trackJson = new JSONObject(track);
        return trackJson.getJSONArray("results");
    }

    private static List<String> tokenize(String item, String delim) {
        return new ArrayList<String>(asList(StringUtils.tokenizeToStringArray(item, delim)));
    }

    private boolean fuzzyMatch(Collection<String> iTokens, Collection<String> lTokens) {
        int iArtistTokenSize = iTokens.size();

        try {
            iTokens.removeAll(lTokens);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        double iDiff = (double) (iArtistTokenSize - iTokens.size());
        double lSize = 0.5 * (double) lTokens.size();

        return iDiff >= lSize;
    }

    private static String clean(String string) {
        return string.toLowerCase()
                .replaceAll("\ba", "")
                .replaceAll("\ban", "")
                .replaceAll("\bthe", "")
                .replaceAll("explicit", "")
                .replaceAll("\'", "")
                .replaceAll("\\.", "");
    }

    public void readLikesFromFile(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Likes.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            this.likes = (Likes) unmarshaller.unmarshal(file);
            this.likes.removeDuplicates();

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


            jaxbMarshaller.marshal(likes, file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getStations() {
        return stations;
    }

    public Likes getLikes() {
        return likes;
    }

    class LikeTask implements Callable<String> {

        String station;
        int index;

        private LikeTask(String station, int index) {
            this.station = station;
            this.index = index;
        }

        @Override
        public String call() {
            try {
                Document doc =
                        Jsoup.connect("http://www.pandora.com/content/" +
                                "station_track_thumbs?" +
                                "stationId=" + station +
                                "&posFeedbackStartIndex=" + (index * 5)).timeout(10000).get();
                for (Element e : doc.select("h3>a:first-of-type")) {
                    String[] info = e.attr("href").split("/");

                    LikeInfo like = new LikeInfo()
                            .withTitle(info[3])
                            .withArtist(info[1])
                            .withAlbum(info[2]);

                    synchronized (likes) {
                        likes.getLikes().add(like);
                    }
                }
            } catch (Exception e) {
                LOG.error(ExceptionUtils.getStackTrace(e));
            }

            return "Done!";
        }
    }

    class SearchTask implements Callable<String> {

        LikeInfo like;

        private SearchTask(LikeInfo like) {
            this.like = like;
        }

        @Override
        public String call() {

            try {
                List<String> terms = new ArrayList<String>();

                for (String string : asList(like.getTitle(), like.getAlbum(), like.getArtist())) {
                    terms.add(string);
                    terms.add(abbreviate(string, "-"));
                }

                for (String term : terms) {
                    search(term, like);
                }

                if (like.getArt() != null) {
                    String msg = like.toStringReal();
                    LOG.info(msg);
                    return msg;
                }

            } catch (Exception e) {

            }

            return null;
        }
    }
}
