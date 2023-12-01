package behnke19.imageTagging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface ImageRepository extends JpaRepository<Image, Long> {
    /**
     * find only the entries that have at least one of the tags provided
     */
    @Query(value = "SELECT * FROM image WHERE id IN (SELECT DISTINCT image_id FROM image_detected_objects WHERE detected_objects IN :detections)",
        nativeQuery = true)
    List<Image> findByDetectedObjectsIn(@Param("detections") Collection<String> detections);
}
