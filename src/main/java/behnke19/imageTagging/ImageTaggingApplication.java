package behnke19.imageTagging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("org.springframework.cloud.gcp.vision")
@SpringBootApplication
public class ImageTaggingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageTaggingApplication.class, args);
	}

}
