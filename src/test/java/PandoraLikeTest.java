import com.google.common.base.Joiner;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;

public class PandoraLikeTest {

    private static final String SEARCH = "https://itunes.apple.com/search?term=TERM&entity=ENTITY&media=MEDIA";
    private static final String PIRATE_SEARCH = "http://thepiratebay.sx/search/TERM/0/7/100";
    private static final String PATH_WINDOWS = "C:\\Users\\vbalaji\\Desktop\\";
    private static final String PATH_MAC_OSX = "/Users/Sephiroth/Desktop/";

    private static final Logger LOG = LoggerFactory.getLogger(PandoraLikeTest.class);

    private PandoraLike pl;
    private ExecutorService executor = Executors.newCachedThreadPool();

    @Before
    public void before() {
        pl = new PandoraLike("shortballer38");
    }

    //@Test
    public void testGetArt(List<LikeInfo> likes) throws InterruptedException {

        //testRead();

        //pl.readLikesFromFile(new File(PATH_WINDOWS, "test-xml-read-concurrent.xml"));

        int limit = 2000;
        List<Callable<String>> tasks = new ArrayList<Callable<String>>();

        for (LikeInfo likeInfo : likes) {
            final LikeInfo like = likeInfo;
            if ((limit-- > 0)) {
                tasks.add(
                        new Callable<String>() {
                            @Override
                            public String call() {

                                String result = null;

                                try {
                                    List<String> terms = new ArrayList<String>();

                                    for (String string : asList(like.getTitle(), like.getAlbum(), like.getArtist())) {
                                        terms.add(string);
                                        terms.add(abbreviate(string, "-"));
                                    }

                                    for (String term : terms) {
                                        if (search(term, like)) {
                                            pirateSearch(like);
                                            break;
                                        }
                                    }

                                    LOG.info(like.getMagnet());

                                } catch (Exception e) {

                                }

                                return result;
                            }
                        }
                );
            }
        }

        executor.invokeAll(tasks);

        //pl.writeLikesToFile(new File(PATH_MAC_OSX, "test-xml-write-concurrent.xml"));
    }

    @Test
    public void testJade4J() throws IOException, InterruptedException {

        pl.initFromFileWithArt(new File(PATH_MAC_OSX, "test-xml-read-concurrent.xml"));
        //pl.init();
        //pl.readLikesFromFile(new File(PATH_MAC_OSX, "test-write.txt"));
        testGetArt(pl.getLikes().getLikes().subList(0, 30));

        String jade = this.getClass().getResource("/likes.jade").getFile();

        JadeConfiguration config = new JadeConfiguration();
        config.setPrettyPrint(true);

        JadeTemplate template = config.getTemplate(jade);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("likes", pl.getLikes().getLikes().subList(0, 30));
        model.put("pageName", "Pandora Likes");

        String html = config.renderTemplate(template, model);

        File out = new File(PATH_MAC_OSX, "test-html-real.html");

        if (out.exists()) {
            out.delete();
        }

        IOUtils.write(html, new FileOutputStream(out));
    }

    //@Test
    public void getPirateSearch() {

        pl.initFromFileWithArt(new File(PATH_MAC_OSX, "test-xml-read-concurrent.xml"));

        for (LikeInfo like : pl.getLikes().getLikes().subList(0, 10)) {
            try {
                Document doc =
                        Jsoup.connect(
                                PIRATE_SEARCH.replace("TERM",
                                        String.format("%s-%s", like.getArtist(), like.getAlbum()))).get();
                System.out.println(doc.baseUri());
                String magnetUrl = doc.select("tr:first-child div.detName+a").attr("href");
                System.out.println(magnetUrl);

            } catch (Exception e) {

            }
        }
    }

    //@Test
    public void testGetSrc() throws URISyntaxException {
        System.out.println(this.getClass().getResource("/likes.jade").getFile());
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

    public boolean pirateSearch(LikeInfo like) {
        try {
            Document doc =
                    Jsoup.connect(
                            PIRATE_SEARCH.replace("TERM",
                                    String.format("%s-%s", like.getArtist(), like.getAlbum())))
                                        .timeout(3000).get();
            String magnet = doc.select("tr:first-child div.detName+a").attr("href");

            if (magnet == null) return false;

            like.setMagnet(magnet);
        } catch (Exception e) {
            if (!(e instanceof SocketTimeoutException)) e.printStackTrace();
            return false;
        }

        return true;
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

        if (iArtistTokenSize == 1) return lTokens.size() == 1;

        String p1 = String.format("Tokens to remove: %s\nTokens before removal: %s\n", lTokens, iTokens);

        try {
            iTokens.removeAll(lTokens);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        String p2 = String.format("Tokens after removal: %s\n", iTokens);

        float iDiff = (float) (iArtistTokenSize - iTokens.size());
        double lSize = 0.5 * (float) lTokens.size();
        boolean match = iDiff >= lSize;

        String p3 = String.format("%s > %s: %s\n", iDiff, lSize, match);

        //if (!match) System.out.format("%s%s%s", p1, p2, p3);

        return match;
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
