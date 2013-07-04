import javax.xml.bind.annotation.*;

import static java.lang.String.format;

@XmlRootElement(name="like")
@XmlType(propOrder = {
        "title",
        "artist",
        "album",
        "art",
        "realTitle",
        "realArtist",
        "realAlbum"
})

public class LikeInfo {

    @XmlElement
    private String title;
    @XmlElement
    private String artist;
    @XmlElement
    private String album;
    private String art;
    private String realTitle;
    private String realArtist;
    private String realAlbum;

    public LikeInfo withTitle(String title) {
        this.title = title;
        return this;
    }

    public LikeInfo withArtist(String artist) {
        this.artist = artist;
        return this;
    }
    public LikeInfo withAlbum(String album) {
        this.album = album;
        return this;
    }

    public LikeInfo withArt(String art) {
        this.art = art;
        return this;
    }

    public LikeInfo withRealTitle(String realTitle) {
        this.realTitle = realTitle;
        return this;
    }

    public LikeInfo withRealArtist(String realArtist) {
        this.realArtist = realArtist;
        return this;
    }
    public LikeInfo withRealAlbum(String realAlbum) {
        this.realAlbum = realAlbum;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getArt() {
        return art;
    }

    public String getRealTitle() {
        return realTitle;
    }

    public String getRealArtist() {
        return realArtist;
    }

    public String getRealAlbum() {
        return realAlbum;
    }

    public void setArt(String art) {
        this.art = art;
    }

    /*public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }*/

    public void setRealTitle(String realTitle) {
        this.realTitle = realTitle;
    }

    public void setRealArtist(String realArtist) {
        this.realArtist = realArtist;
    }

    public void setRealAlbum(String realAlbum) {
        this.realAlbum = realAlbum;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LikeInfo)) return false;

        LikeInfo l = (LikeInfo) o;

        return (l.title.equals(title) &&
                l.artist.equals(artist) &&
                l.album.equals(album));

    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + title.hashCode();
        hash = hash * 31 + artist.hashCode();
        hash = hash * 13 + album.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return format("/%s/%s/%s/%s", artist, album, title, art);
    }

    public String toStringNoArt() {
        return format("/%s/%s/%s/", artist, album, title);
    }

    public String toStringReal() {
        return format("/%s/%s/%s/%s", realArtist, realAlbum, realTitle, art);
    }

    public String toStringRealNoArt() {
        return format("/%s/%s/%s/", realArtist, realAlbum, realTitle);
    }

    public String toStringAll() {
        return format("/%s/%s/%s/%s/%s/%s/%s", artist, album, title, art, realArtist, realAlbum, realTitle);
    }

    public String toStringAllButArt() {
        return format("/%s/%s/%s/%s/%s/%s", artist, album, title, realArtist, realAlbum, realTitle);
    }
}
