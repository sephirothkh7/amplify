public class LikeInfo {

    private String title;
    private String artist;
    private String album;
    private String art;

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

    public String toStringNoArt() {
        return String.format("/%s/%s/%s/", artist, album, title);
    }

    public void setArt(String art) {
        this.art = art;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Override
    public String toString() {
        return String.format("/%s/%s/%s/%s", artist, album, title, art);
    }
}
