package behnke19.imageTagging;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * This class has logic to reach out to Imagga apis for image tagging.
 */
public class ImageTaggingService {
    private final String basicAuth;
    private final String tags_endpoint = "https://api.imagga.com/v2/tags";
    private final String uploads_endpoint = "https://api.imagga.com/v2/uploads";

    ImageTaggingService() {
        String key = System.getenv("imagga-key");
        String secret = System.getenv("imagga-secret");
        // init credentials
        String credentials = String.format("%s:%s", key, secret);
        basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Uses imagga to detect objects in the given image and populates the detectedObjects field of the image
     * @param image
     * @throws Exception
     */
    public void detectObjectsInImage(Image image) throws Exception {
        // the image should only have one of URL or content populated
        if (image.getImageUrl() != null) {
            detectObjectsInImageUrl(image);
        } else {
            detectObjectsInImageFile(image);
        }
    }

    /**
     * Uses imagga to tag objects in the image using the imageContent to upload a file.
     * @param image
     * @throws Exception
     */
    public void detectObjectsInImageFile(Image image) throws Exception {
        // save the image content to a temp file to
        File tmpFile = new File("/tmp/image" + System.currentTimeMillis() + ".tmp");
        FileOutputStream outputStream = new FileOutputStream(tmpFile);
        try {
            outputStream.write(image.getImageContent());
        } finally {
            outputStream.close();
        }
        // first we must upload the image
        String upload_id;
        try {
            upload_id = uploadFileToImagga(tmpFile);
        } catch (Exception ex) {
            throw new Exception("Error uploading file");
        } finally {
            tmpFile.delete(); // make sure the tmp file is cleaned up even if there was an error on upload
        }


        // now we can pass the upload id to the tags endpoint
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(tags_endpoint);
        urlBuilder.append("?image_upload_id=");
        urlBuilder.append(upload_id);
        // the imagga docs say tags with a confidence lower than 30 are likely to be wrong so ignore them
        urlBuilder.append("&threshold=30.0");
        String tagsJsonResponse = getImageTags(urlBuilder.toString());
        processResultingTags(tagsJsonResponse, image);
    }

    /**
     * Uses imagga to tag objects in the image using the image url.
     * @param image
     * @throws Exception
     */
    public void detectObjectsInImageUrl(Image image) throws Exception {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(tags_endpoint);
        urlBuilder.append("?image_url=");
        urlBuilder.append(image.getImageUrl());
        // the imagga docs say tags with a confidence lower than 30 are likely to be wrong so ignore them
        urlBuilder.append("&threshold=30.0");
        String tagsJsonResponse = getImageTags(urlBuilder.toString());
        processResultingTags(tagsJsonResponse, image);
    }

    /**
     * Uploads the given image file to imagga and returns the uploaded image id.
     * @param imageFile
     * @return
     * @throws Exception
     */
    private String uploadFileToImagga(File imageFile) throws Exception {
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "Image Upload";
        URI uri = new URI(uploads_endpoint);
        URL urlObject = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + imageFile.getName() + "\"" + crlf);
        request.writeBytes(crlf);
        InputStream inputStream = new FileInputStream(imageFile);
        int bytesRead;
        byte[] dataBuffer = new byte[1024];
        while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
            request.write(dataBuffer, 0, bytesRead);
        }
        request.writeBytes(crlf);
        request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
        request.flush();
        request.close();

        InputStream responseStream = new BufferedInputStream(connection.getInputStream());
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();

        String jsonResponse = stringBuilder.toString();
        Map<String, Object> responseMap = new ObjectMapper().readValue(jsonResponse, HashMap.class);

        responseStream.close();
        connection.disconnect();

        Map<String, Object> statusMap = (Map<String, Object>) responseMap.get("status");
        Map<String, Object> resultMap = (Map<String, Object>) responseMap.get("result");
        String upload_id = null;
        if (statusMap != null && "success".equals(statusMap.get("type")) && resultMap != null) {
            upload_id = (String) resultMap.get("upload_id");
        }

        if (upload_id == null) {
            throw new Exception("error uploading file");
        }
        return upload_id;
    }


    /**
     * Get the tags for an image using the imagga tags endpoint. The URL should specify the
     * param for either an imageUrl or the upload_id
     * @param url
     * @return
     * @throws Exception
     */
    private String getImageTags(String url) throws Exception {
        URI tagsUri = new URI(url);
        URL tagsUrlObject = tagsUri.toURL();

        HttpURLConnection tagsConnection = (HttpURLConnection) tagsUrlObject.openConnection();
        tagsConnection.setRequestProperty("Authorization", "Basic " + basicAuth);

        if (tagsConnection.getResponseCode() == 200) {
            //success! now lets take note of the tags
            BufferedReader responseBody = new BufferedReader(new InputStreamReader(tagsConnection.getInputStream()));

            // Parse the json response to find the tags and add them to the image
            String tagsJsonResponse = responseBody.readLine();

            responseBody.close();
            return tagsJsonResponse;
        } else {
            throw new Exception("Error processing image");
        }
    }

    /**
     * Add the tags found by imagga to the image object
     * @param jsonResponse
     * @param image
     * @throws Exception
     */
    private void processResultingTags(String jsonResponse, Image image) throws Exception {
        Map<String, Object> responseMap = new ObjectMapper().readValue(jsonResponse, HashMap.class);
        Map<String, Object> resultMap = (Map<String, Object>) responseMap.get("result");
        Map<String, Object> statusMap = (Map<String, Object>) responseMap.get("status");
        if (statusMap != null && "success".equals(statusMap.get("type")) && resultMap != null) { // image was successfully labeled.
            ArrayList<Map<String, Object>> tagsList = (ArrayList<Map<String, Object>>) resultMap.get("tags");
            if (tagsList != null) {
                for (Map<String, Object> tag : tagsList) {
                    Map<String, Object> tagMap = (Map<String, Object>) tag.get("tag");
                    if (tagMap != null) {
                        image.addDetectedObject((String) tagMap.get("en")); // there are only english tags
                    }
                }
            }
        }
    }

}
