package behnke19.imageTagging;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.*;

@RestController
public class ImageController {
    private final ImageRepository repository;

    ImageController(ImageRepository repository) {
        this.repository = repository;
    }

    /**
     * GET endpoint to fetch all images in the database. An optional 'objects' query param
     * can be provided to search only for images that contain the given objects.
     * @param objects comma separated list of objects
     * @return
     */
    @GetMapping("/images")
    List<Image> getImages(@RequestParam(value = "objects", required = false) String objects) {
        if (null == objects || objects.isBlank()) {
            return repository.findAll();
        } else {
            // TODO trim whitespaces if I have time later?
            objects = objects.replaceAll("\"", "");
            List<String> targetObjects = Arrays.asList(objects.split(","));
            LinkedList<Image> matchingImages = new LinkedList<>();
            List<Image> imagesWithTags = repository.findByDetectedObjectsIn(targetObjects);
            for (Image potentialMatch : imagesWithTags) {
                // for performance reasons convert the list to a set so contains checks are more efficient.
                // we don't care about duplicates when checking contains anyway
                HashSet<String> uniqueDetectedObjects = new HashSet<>(potentialMatch.getDetectedObjects());
                if (uniqueDetectedObjects.containsAll(targetObjects)){
                    matchingImages.add(potentialMatch);
                }
            }
            return matchingImages;
        }
    }

    /**
     * GET a specific image from the database.
     * @param id the id of the image to fetch.
     * @return
     */
    @GetMapping("/images/{id}")
    Image getImage(@PathVariable Long id) {
        Optional<Image> result = repository.findById(id);
        if (result.isPresent()) {
            return result.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Image found with the id " + id);
        }
    }

    /**
     * POST call to upload an image and optionally scan it to detect objects in the image.
     * Provide either a URL or a file but not both.
     * @param label optional label for the image
     * @param detectObjects if true, detect objects in the image
     * @param URL a url for an image.
     * @param file an image file to process.
     * @return
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Image uploadImage(@RequestPart(name = "label", required = false) String label,
                      @RequestPart(name = "detectObjects", required = false) Boolean detectObjects,
                      @RequestPart(name = "url", required = false) String URL,
                      @RequestPart(name = "file", required = false) MultipartFile file) { // query params for label and object detection allowed?
        // no image provided so nothing to do. Ask the user to provide one.
        if ((URL == null || URL.isBlank()) && file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A url or image file is required");
        }
        // if we have a URL and image file how do we know what to process? Just throw an error.
        if ((URL != null && !URL.isBlank()) && file != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both a url and image file were provided. Please provide only one of them.");
        }

        Image image = new Image();
        if (label != null && !label.isBlank()) {
            image.setLabel(label);
        } else {
            // no label provided so create a default image name with the current time as part of the name for uniqueness
            image.setLabel("image-" + System.currentTimeMillis());
        }

        if (detectObjects != null && detectObjects) {
            ImageTaggingService taggingService = new ImageTaggingService();
            if (file != null) {
                // a file was provided... lets upload it to imagga and process it.
                File tmpFile = new File("/tmp/image" + System.currentTimeMillis() + ".tmp");
                try {
                    file.transferTo(tmpFile);
                    taggingService.detectObjectsInImage(tmpFile, image);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing image file.");
                } finally {
                    tmpFile.delete(); // cleanup
                }
            } else {
                // we know file is null and URL is non null so send the url to imagga for processing
                try {
                    taggingService.detectObjectsInImage(URL, image);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error tagging image");
                }

            }
        }

        repository.save(image);
        return image;
    }


}
