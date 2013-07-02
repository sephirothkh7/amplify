import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name="likes")
public class Likes {

    private Set<LikeInfo> likes;

    public Likes withLikes(Set<LikeInfo> likes) {
        this.likes = likes;
        return this;
    }

    @XmlElementRef
    public Set<LikeInfo> getLikes() {
        return likes;
    }

    public void setLikes(Set<LikeInfo> likes) {
        this.likes = likes;
    }
}
