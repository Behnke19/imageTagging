package behnke19.imageTagging;

import jakarta.persistence.*;

import java.util.LinkedList;
import java.util.List;

@Entity
public class Image {
    private @Id @GeneratedValue Long id;
    @ElementCollection
    private List<String> detectedObjects;

    private String label;

    @Column(length = 500)
    private String imageUrl;

    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(length = 2000000)
    private byte[] imageContent;
    // I'm not a big fan of storing the image data like this and the prompts are a little unclear because they say
    // things like return the image metadata, return the images, return a JSON response including the image data which
    // all sound like different but similar things to me.
    //
    // Initially I was not saving off the url or file content but after rereading the problem, my interpretation is that
    // we want to persist the actual file contents when a file is provided and also include that data in the JSON
    // response. Its kind of ugly since it's a giant encoded string in the response but another program could certainly
    // consume it.
    //
    // My real preference would be to save the file content as its own object type that is just referenced by this
    // image metadata class. Then you could go to another endpoint like /imageContent/fileId to actually download the
    // file. On the other hand, I could see wanting to have a preview of the image in a UI and having the image
    // content right away would save an extra API call. Or in the case of a list of images it would save many calls.
    // These files shouldn't be huge so not being able to stream them isn't a huge issue. The GET images could end up
    // with a huge payload though if there are many images. That could be resolved with paginated results but that is
    // out of scope for this project.

    Image() {}

    Image(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getDetectedObjects() {
        if (detectedObjects == null) {
            detectedObjects = new LinkedList<>();
        }
        return detectedObjects;
    }

    public void setDetectedObjects(List<String> detectedObjects) {
        this.detectedObjects = detectedObjects;
    }

    public void addDetectedObject(String detectedObject) {
        getDetectedObjects().add(detectedObject);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public byte[] getImageContent() {
        return imageContent;
    }

    public void setImageContent(byte[] content) {
        this.imageContent = content;
    }
}
