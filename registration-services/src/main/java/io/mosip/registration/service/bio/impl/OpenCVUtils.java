package io.mosip.registration.service.bio.impl;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
@Component
public class OpenCVUtils {
    public static BufferedImage matToBufferedImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }


    public static Mat bufferedImageToMat(BufferedImage bi) {
        if (bi == null) {
            throw new IllegalArgumentException("BufferedImage cannot be null");
        }

        Mat mat;
        if (bi.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else {
            BufferedImage convertedImg = new BufferedImage(
                    bi.getWidth(),
                    bi.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(bi, 0, 0, null);
            mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        }
        return mat;
    }

    public static BufferedImage copyBufferedImage(BufferedImage originalImage) {
        if (originalImage == null) return null;

        // Create a new BufferedImage with the same width, height, and type
        BufferedImage copy = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                originalImage.getType()
        );

        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return copy;
    }

}
