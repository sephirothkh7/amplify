import com.fasterxml.jackson.core.JsonProcessingException;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PandoraLikeTest {

    private static final int MAX_PAGES = 100;
    private static final String SEARCH = "https://itunes.apple.com/search?term=TERM&entity=ENTITY&media=MEDIA";
    private static final String SEARCH_ALBUM_BY_TITLE = "https://itunes.apple.com/search?term=TITLE&entity=album";
    private static final String SEARCH_ALBUM_BY_ALBUM = "https://itunes.apple.com/search?term=ALBUM&entity=album";

    private static final String PATH_WINDOWS = "C:\\Users\\vbalaji\\Desktop\\temp";
    private static final String PATH_MAC_OSX = "/Users/Sephiroth/Desktop/";

    private PandoraLike pl;

    @Before
    public void before() {
        pl = new PandoraLike("shortballer38");
    }

    //@Test
    public void testGet() throws IOException, DocumentException {

        URL stationsURL = new URL("http://feeds.pandora.com/feeds/people/sephirothkh7/stations.xml");

        SAXReader reader = new SAXReader();
        org.dom4j.Document document = reader.read(stationsURL);

        List<Node> stationCodes = document.selectNodes( "//pandora:stationCode" );
        List<String> stations = new ArrayList<String>();

        Iterator<Node> it = stationCodes.iterator();
        while (it.hasNext()) {
            Node stationCode = it.next();

            if (stationCode.getText().startsWith("sh")) {
                stations.add(stationCode.getText().replaceAll("sh", ""));
            }
        }

        Set<String> likes = new HashSet<String>();

        for (String station : stations) {

            for (int i = 0; i < MAX_PAGES; i++) {

                Document doc =
                    Jsoup.connect("http://www.pandora.com/content/" +
                                  "station_track_thumbs?" +
                                  "stationId=" + station +
                                  "&posFeedbackStartIndex=" + (i * 5)).get();
                for (Element e : doc.select("h3>a:first-of-type")) {
                    likes.add(e.attr("href"));
                }
            }
        }

        for (String like : likes) {
            System.out.println(like);
        }
    }

    //@Test
    public void testAppleSearch() {
        try {
            JSONObject obj = new JSONObject(EXAMPLE_JSON);

            JSONArray results = obj.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                if (result.getString("artistViewUrl").contains("blink-182")) {
                    System.out.println(result.getString("artworkUrl100"));
                }
            }
        } catch (Exception e) {

        }
    }

    //@Test
    public void testRead() {

        pl.initFromFile(new File(PATH_WINDOWS, "test-xml-read.txt"));

        for (LikeInfo like : pl.getLikes()) {
            System.out.println(like.toStringAll());
        }
    }

    //@Test
    public void testWrite() {

        testRead();

        File out = new File(PATH_WINDOWS, "test-xml-write.txt");

        if (out.exists()) {
            out.delete();
        }

        pl.writeLikesToFile(out);

        for (LikeInfo like : pl.getLikes()) {
            //System.out.println(toString());
        }
    }

    //@Test
    public void testGetArt() {

        //testRead();

        pl.readLikesFromFile(new File("/Users/Sephiroth/Desktop/test.txt"));

        int limit = 20;

        for (LikeInfo like : pl.getLikes()) {
            if (like.getAlbum().contains("-explicit") && (--limit > 0)) {
                try {
                    System.out.print(like.toStringNoArt() + ": ");
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

                            //System.out.format("%s\n%s\n", tArtist, lArtist);

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

                                //System.out.format("%s\n%s\n", tArtist, lArtist);

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

                                //System.out.format("%s\n%s\n%s\n%s\n", tArtist, tAlbum, lArtist, lAlbum);

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
                    //System.out.println(toHTML(like.getArtist(), like.getAlbum(), like.getTitle(), like.getArt()));
                    //System.out.format("%s\n%s\n%s\n%s\n\n", like.getRealArtist(), like.getRealAlbum(), like.getRealTitle(), like.getArt());
                } catch (Exception e) {

                }
            }
        }
    }

    //@Test
    public void testToXml() throws JsonProcessingException {

        testRead();

        /*ObjectMapper xmlMapper = new XmlMapper();

        for (LikeInfo like : pl.getLikes()) {
            String xml = xmlMapper.writeValueAsString(like);
            System.out.println(xml);
        }*/

        try {

            File file = new File(PATH_WINDOWS, "test-xml.xml");

            if (file.exists()) {
                file.delete();
            }

            //JAXBContext jaxbContext = JAXBContext.newInstance(LikeInfo.class);
            JAXBContext jaxbContext = JAXBContext.newInstance(Likes.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            /*for (LikeInfo like : pl.getLikes()) {
                jaxbMarshaller.marshal(like, file);
                jaxbMarshaller.marshal(like, System.out);
            }*/

            jaxbMarshaller.marshal(new Likes().withLikes(pl.getLikes()), file);
            jaxbMarshaller.marshal(new Likes().withLikes(pl.getLikes()), System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testJade4J() throws IOException {

        //pl.initFromFileWithArt(new File(PATH_WINDOWS, "test-xml-read.xml"));
        pl.init();

        String jade = this.getClass().getResource("/likes.jade").getFile();

        JadeConfiguration config = new JadeConfiguration();
        config.setPrettyPrint(true);

        JadeTemplate template = config.getTemplate(jade);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("likes", pl.getLikes());
        model.put("pageName", "Pandora Likes");

        String html = config.renderTemplate(template, model);

        File out = new File(PATH_WINDOWS, "test-html.html");

        if (out.exists()) {
            out.delete();
        }

        IOUtils.write(html, new FileOutputStream(out));
    }

    //@Test
    public void testTransWords() {

        pl.readLikesFromFile(new File("/Users/Sephiroth/Desktop/test.txt"));

        for (LikeInfo like : pl.getLikes()) {
            System.out.println(transform2(like.toStringNoArt()));
        }
    }

    private String transform(String string) {

        return string
                .replaceAll("\ba", "")
                .replaceAll("\ban", "")
                .replaceAll("\bthe", "")
                .replaceAll("-explicit", "");
    }

    //@Test
    public void testTransform2() {
        Pattern p = Pattern.compile("\\b\\d+");
        String string = "/3-days-grace/1-x";
        Matcher m = p.matcher(string);
        String result = string;
        while (m.find()) {
            result = result.replaceFirst(m.group(), NumberToWords.processor.getName(m.group()));
        }
        System.out.println(result);
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

    private static final String EXAMPLE_JSON = "{\n" +
            " \"resultCount\":7,\n" +
            " \"results\": [\n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":116851, \"collectionId\":293530080, \"amgArtistId\":211247, \"artistName\":\"Blink-182\", \"collectionName\":\"Dude Ranch\", \"collectionCensoredName\":\"Dude Ranch\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/blink-182/id116851?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/dude-ranch/id293530080?uo=4\", \"artworkUrl60\":\"http://a5.mzstatic.com/us/r1000/050/Music/df/91/fa/mzi.xdafvncb.60x60-50.jpg\", \"artworkUrl100\":\"http://a4.mzstatic.com/us/r1000/050/Music/df/91/fa/mzi.xdafvncb.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":15, \"copyright\":\"℗ 2008 Geffen Records\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2008-10-21T07:00:00Z\", \"primaryGenreName\":\"Rock\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":20834495, \"collectionId\":528098459, \"amgArtistId\":200363, \"artistName\":\"Not Breathing\", \"collectionName\":\"Dude Ranch EP - EP\", \"collectionCensoredName\":\"Dude Ranch EP - EP\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/not-breathing/id20834495?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/dude-ranch-ep-ep/id528098459?uo=4\", \"artworkUrl60\":\"http://a2.mzstatic.com/us/r30/Music/v4/70/47/dd/7047dd01-d35c-367c-e9e1-a9810471ff7e/859708260892_cover.60x60-50.jpg\", \"artworkUrl100\":\"http://a2.mzstatic.com/us/r30/Music/v4/70/47/dd/7047dd01-d35c-367c-e9e1-a9810471ff7e/859708260892_cover.100x100-75.jpg\", \"collectionPrice\":3.96, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":4, \"copyright\":\"℗ 2012 instant shoggoth\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2012-05-16T07:00:00Z\", \"primaryGenreName\":\"Electronic\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":6316181, \"collectionId\":158061711, \"amgArtistId\":668804, \"artistName\":\"Trent Willmon\", \"collectionName\":\"Trent Willmon\", \"collectionCensoredName\":\"Trent Willmon\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/trent-willmon/id6316181?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/trent-willmon/id158061711?uo=4\", \"artworkUrl60\":\"http://a2.mzstatic.com/us/r1000/036/Music/46/ef/05/mzi.viauigaz.60x60-50.jpg\", \"artworkUrl100\":\"http://a5.mzstatic.com/us/r1000/036/Music/46/ef/05/mzi.viauigaz.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":11, \"copyright\":\"℗ 2004 SONY BMG MUSIC ENTERTAINMENT\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2004-10-12T07:00:00Z\", \"primaryGenreName\":\"Country\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":36270, \"collectionId\":320618284, \"artistName\":\"Various Artists\", \"collectionName\":\"Rhythms & Underscores\", \"collectionCensoredName\":\"Rhythms & Underscores\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/rhythms-underscores/id320618284?uo=4\", \"artworkUrl60\":\"http://a3.mzstatic.com/us/r1000/018/Music/ea/b0/b6/mzi.uctqlkcl.60x60-50.jpg\", \"artworkUrl100\":\"http://a2.mzstatic.com/us/r1000/018/Music/ea/b0/b6/mzi.uctqlkcl.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":28, \"copyright\":\"℗ 1996 Abaco Music Library\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2009-06-29T07:00:00Z\", \"primaryGenreName\":\"Jazz\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":275556493, \"collectionId\":346354881, \"artistName\":\"5 Alarm Music\", \"collectionName\":\"Rhythms & Underscores\", \"collectionCensoredName\":\"Rhythms & Underscores\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/5-alarm-music/id275556493?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/rhythms-underscores/id346354881?uo=4\", \"artworkUrl60\":\"http://a1.mzstatic.com/us/r1000/038/Music/6f/5a/46/mzi.dimxyqcv.60x60-50.jpg\", \"artworkUrl100\":\"http://a4.mzstatic.com/us/r1000/038/Music/6f/5a/46/mzi.dimxyqcv.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":28, \"copyright\":\"℗ 2008 5 Alarm Music\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2008-11-15T08:00:00Z\", \"primaryGenreName\":\"Pop\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":97416551, \"collectionId\":97418306, \"amgArtistId\":950661, \"artistName\":\"Orange Monkey\", \"collectionName\":\"Phat Berries\", \"collectionCensoredName\":\"Phat Berries\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/orange-monkey/id97416551?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/phat-berries/id97418306?uo=4\", \"artworkUrl60\":\"http://a3.mzstatic.com/us/r30/Music/29/0a/9c/mzi.tiayivzq.60x60-50.jpg\", \"artworkUrl100\":\"http://a2.mzstatic.com/us/r30/Music/29/0a/9c/mzi.tiayivzq.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":23, \"copyright\":\"℗ 2002 Orange Monkey\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2005-10-03T07:00:00Z\", \"primaryGenreName\":\"Alternative\"}, \n" +
            "{\"wrapperType\":\"collection\", \"collectionType\":\"Album\", \"artistId\":97416551, \"collectionId\":99250817, \"amgArtistId\":950661, \"artistName\":\"Orange Monkey\", \"collectionName\":\"O.N.K.A.\", \"collectionCensoredName\":\"O.N.K.A.\", \"artistViewUrl\":\"https://itunes.apple.com/us/artist/orange-monkey/id97416551?uo=4\", \"collectionViewUrl\":\"https://itunes.apple.com/us/album/o.n.k.a./id99250817?uo=4\", \"artworkUrl60\":\"http://a4.mzstatic.com/us/r30/Music/83/1a/c0/mzi.egxnrgih.60x60-50.jpg\", \"artworkUrl100\":\"http://a4.mzstatic.com/us/r30/Music/83/1a/c0/mzi.egxnrgih.100x100-75.jpg\", \"collectionPrice\":9.99, \"collectionExplicitness\":\"notExplicit\", \"trackCount\":19, \"copyright\":\"℗ 2005 Orange Monkey\", \"country\":\"USA\", \"currency\":\"USD\", \"releaseDate\":\"2005-11-04T08:00:00Z\", \"primaryGenreName\":\"Electronic\"}]\n" +
            "}";
}
