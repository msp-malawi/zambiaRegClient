package io.mosip.registration.service.bio.impl;


import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageProcessingService {





    public byte[] convertToJP2ByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG2000");
        if (!writers.hasNext()) {
            throw new RuntimeException("No JPEG2000 writers found. Make sure jai-imageio is in classpath.");
        }

        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(image);
        ios.close();
        writer.dispose();

        return baos.toByteArray();
    }


}
