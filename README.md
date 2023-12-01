# imageTagging

This application was created as part of the interview process for a Software job.

This application provides REST endpoints for finding objects in images using the Imagga apis.

### Running the App
First compile the jar file using maven ```./mvnw install```  
Next, build the docker image. ```docker build -t behnke19/imagetagging .```  
Create a file called .env in the project directory that has two entries with your Imagga credentials.  
```
imaggakey=yourapikey
imaggasecret=yoursecretkey
```
Finally, start the app with docker compose. This will start a mysql container along with the image tagging app.  
```docker-compose up -d```

### Using the APIs

#### GET /images
Returns a list of all the images in the database. And optional query param ```?objects=cat,hat``` can be used to fetch only  
images that contain the provided objects.

#### GET /images/{imageId}
Returns a specific image specified by its ID.

#### POST /images
Submit an image to the service and optionally run object detection on it. 
The payload should be a multipart form with the following fields.  
**label** - A text label for the image. (Optional)  
**detectObjects** - A boolean value. If true then object detection will be processed for the given image. (Optional)  
**url** - A url that points to an image. (Either an url or file must be provided)  
**file** - An image file to be processed. (Either an url or file must be provided)

This endpoint returns the image with a list of detected objects if object detection was run.