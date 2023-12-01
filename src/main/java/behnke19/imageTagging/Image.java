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
}
