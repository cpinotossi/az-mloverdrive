package com.cpinotossi.az;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.*;

/**
 * Azure Functions with Service Bus Trigger.
 */
public class ServiceBusQueueHandler {

    static final String SERVICEBUS_QUEUE_NAME = "mloverdrive-servicebusqueue"; // Not possible to retrieve via System.getenv
                                                                           // because of annotation
    static final String OUTPUT_STORAGE_CONNECTION_STRING = System.getenv("OUTPUT_STORAGE_CONNECTION_STRING");
    static final String INPUT_STORAGE_CONNECTION_STRING = "INPUT_STORAGE_CONNECTION_STRING";
    static final String SERVICE_BUS_CONNECTION_STRING = "SERVICE_BUS_CONNECTION_STRING";
    static final String OUTPUT_STORAGE_CONTAINER_NAME = System.getenv("OUTPUT_STORAGE_CONTAINER_NAME");
    static final String INPUT_STORAGE_CONTAINER_NAME = System.getenv("INPUT_STORAGE_CONTAINER_NAME");

    /**
     * This function will be invoked when a new message is received at the Service
     * Bus Queue.
     */
    @FunctionName("ServiceBusQueueTriggerJava1")
    @StorageAccount(INPUT_STORAGE_CONNECTION_STRING)
    public void run(
            @ServiceBusQueueTrigger(name = "message", queueName = SERVICEBUS_QUEUE_NAME, connection = SERVICE_BUS_CONNECTION_STRING) String message,
            @BlobInput(name = "file", dataType = "binary", path = "{data.url}") byte[] inputBlobByteArray,
            final ExecutionContext context) {
        context.getLogger().info("Java Service Bus Queue trigger function executed.");
        try {
            // Helper Class to transform incomming Message into an Java Object.
            final Gson gson = new GsonBuilder().create();
            POJOEventSchema eventGridEvent = gson.fromJson(message, POJOEventSchema.class);
            String urlString = eventGridEvent.data.get("url").toString();
            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1, urlString.length());
            //String filePath =  (urlString.split("INPUT_STORAGE_CONTAINER_NAME"+"/"))[1];
            context.getLogger().info("The size of \"" + fileName + "\" is: " + inputBlobByteArray.length + " bytes");
            // Resize Image
            ByteArrayInputStream inputByteArrayInputStream = new ByteArrayInputStream(inputBlobByteArray);
            BufferedImage inputBufferedImage = ImageIO.read(inputByteArrayInputStream);

            Dimension imgSize = new Dimension(inputBufferedImage.getWidth(), inputBufferedImage.getHeight());
            Dimension boundary = new Dimension(300, 300);
            Dimension resizeImgSize = getScaledDimension(imgSize, boundary);
            byte[] outputByteArray = resizeImage(inputBufferedImage, resizeImgSize.width, resizeImgSize.height);
            // Upload new blob
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(OUTPUT_STORAGE_CONNECTION_STRING);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(OUTPUT_STORAGE_CONTAINER_NAME);
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            ByteArrayInputStream outputByteArrayInputStream = new ByteArrayInputStream(outputByteArray);
            blob.upload(outputByteArrayInputStream, outputByteArray.length);
            outputByteArrayInputStream.close();
        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
            // return "Access Error!";
        }
    }

    private static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {

        int original_width = imgSize.width;
        int original_height = imgSize.height;
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            // scale width to fit
            new_width = bound_width;
            // scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            // scale height to fit instead
            new_height = bound_height;
            // scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }

        return new Dimension(new_width, new_height);
    }

    private static byte[] resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws Exception {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        ByteArrayOutputStream outputByteArrayOutupStream = new ByteArrayOutputStream();
        ImageIO.write(outputImage, "jpg", outputByteArrayOutupStream);
        return outputByteArrayOutupStream.toByteArray();
    }

}

class POJOEventSchema {
    public String topic;
    public String subject;
    public String eventType;
    public Date eventTime;
    public String id;
    public String dataVersion;
    public String metadataVersion;
    public Map<String, Object> data;
}
