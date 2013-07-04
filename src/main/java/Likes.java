import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

@XmlRootElement(name="likes")
public class Likes implements Iterable<LikeInfo> {

    private List<LikeInfo> likes = new ArrayList<LikeInfo>();

    public Likes withLikes(List<LikeInfo> likes) {
        this.likes = new ArrayList<LikeInfo>(new HashSet<LikeInfo>(likes));
        return this;
    }

    @XmlElementRef
    public List<LikeInfo> getLikes() {
        return likes;
    }

    public void setLikes(List<LikeInfo> likes) {
        this.likes = likes;
    }

    public Likes removeDuplicates () {
        this.likes = new ArrayList<LikeInfo>(new HashSet<LikeInfo>(likes));
        return this;
    }

    public Likes sortByTitle() {
        Collections.sort(likes, new Comparator<LikeInfo>() {
            @Override
            public int compare(LikeInfo o1, LikeInfo o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });

        return this;
    }

    public Likes sortByArtist() {
        Collections.sort(likes, new Comparator<LikeInfo>() {
            @Override
            public int compare(LikeInfo o1, LikeInfo o2) {
                return o1.getArtist().compareTo(o2.getArtist());
            }
        });

        return this;
    }

    public Likes sortByAlbum() {
        Collections.sort(likes, new Comparator<LikeInfo>() {
            @Override
            public int compare(LikeInfo o1, LikeInfo o2) {
                return o1.getAlbum().compareTo(o2.getAlbum());
            }
        });

        return this;
    }

    @Override
    public Iterator<LikeInfo> iterator() {
        return likes.iterator();
    }
}
