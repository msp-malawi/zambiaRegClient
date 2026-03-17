package io.mosip.registration.service.bio.impl;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import nu.pattern.OpenCV;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

import static io.mosip.registration.constants.LoggerConstants.FACE_DETECTION;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;


@Component
public class FaceDetection {
    private static final Logger LOGGER = AppConfig.getLogger(FaceDetection.class);

    private static final double DESIRED_FACE_RATIO_MIN = 0.60;
    private static final double DESIRED_FACE_RATIO_MAX = 0.80;
    private static final double FACE_CENTER_TOLERANCE = 0.1;
    private static final double MIN_QC_SCORE = 60.0;
    private static final double BACKGROUND_UNIFORMITY_THRESHOLD = 20.0;
    private static final int PASSPORT_WIDTH = 480; //480
    private static final int PASSPORT_HEIGHT = 600; //600

    static {
        OpenCV.loadLocally();
    }

    public AtomicReference<Rect> lastDetectedFace = new AtomicReference<>();
    private int eyeClosedFrames = 0;
    private boolean blinkDetected = false;
    @Autowired
    private FaceStreamer faceStreamer;
    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeCascade;

    {
        try {
            loadCascadeClassifier("/haarcascade_frontalface_alt.xml", "face");
            loadCascadeClassifier("/haarcascade_eye.xml", "eye");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize face detection classifiers", e);
        }
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
            // Convert to TYPE_3BYTE_BGR first
            BufferedImage convertedImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(bi, 0, 0, null);
            mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        }
        return mat;
    }

    public boolean detectFacesAndDrawICAO(Mat mat, BufferedImage img, AtomicReference<Double> scoreOut) {
        if (mat == null || mat.empty()) {
            LOGGER.info(FACE_DETECTION, APPLICATION_NAME, APPLICATION_ID, "Empty frame received");
            return false;
        }

        Mat gray = null;
        MatOfRect faceDetections = null;
        Graphics2D g2d = null;
        Rect[] faces = null;
        try {
            if (faceCascade == null || faceCascade.empty()) {
                LOGGER.error(FACE_DETECTION, APPLICATION_NAME, APPLICATION_ID, "Face classifier not loaded");
                return false;
            }

            // Convert to grayscale for detection
            gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            // Detect faces
            faceDetections = new MatOfRect();
            faceCascade.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(100, 100), new Size());

            // Calculate image quality score
            double qcScore = calculateImageQuality(gray);
            scoreOut.set(qcScore);
            faceStreamer.setQualityScore(qcScore);

            g2d = img.createGraphics();
            g2d.setStroke(new BasicStroke(2));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Validate exactly one face detected
            faces = faceDetections.toArray();
            if (faces.length != 1) {
                drawErrorMessage(g2d, faces.length > 1 ? "Multiple faces detected" : "No face detected");
                return false;
            }

            Rect faceRect = faces[0];
            lastDetectedFace.set(faceRect);

            // Calculate all requirements
            double faceRatio = calculateFaceRatio(faceRect, mat);
            boolean ratioOk = isFaceRatioValid(faceRatio);
            boolean centered = isFaceCentered(faceRect, mat);
            boolean eyesOpen = areEyesOpen(gray.submat(faceRect));
            boolean qualityOk = qcScore >= MIN_QC_SCORE;

            // Combine all checks
            boolean faceOk = ratioOk && centered && eyesOpen && qualityOk;

            // Draw face rectangle with appropriate color
            drawFaceRectangle(g2d, faceRect, faceOk);

            // Draw information overlay
            drawInformationOverlay(g2d, mat, faceRect, qcScore, faceRatio, centered, eyesOpen);

            // Draw ideal face area guides
            drawFaceAreaGuides(g2d, mat);

            return faceOk;
        } catch (Exception e) {
            LOGGER.error(FACE_DETECTION, APPLICATION_NAME, APPLICATION_ID, "Error during face detection: " + e.getMessage());
            return false;
        } finally {
            // Release resources in reverse order of creation
            if (g2d != null) {
                g2d.dispose();
            }
            if (faceDetections != null) {
                faceDetections.release();
            }
            if (gray != null) {
                gray.release();
            }
            if (faces != null) {
                faces = null;
            }


        }
    }

    public boolean detectBlink(Mat frame) {
        if (frame == null || frame.empty()) {
            return false;
        }

        Mat gray = null;
        MatOfRect faces = null;

        try {
            gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            faces = new MatOfRect();
            faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(100, 100), new Size());

            for (Rect face : faces.toArray()) {
                Mat faceROI = null;
                MatOfRect eyes = null;

                try {
                    faceROI = gray.submat(face);
                    eyes = new MatOfRect();
                    eyeCascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0, new Size(20, 20), new Size());

                    if (eyes.toArray().length == 0) {
                        eyeClosedFrames++;
                    } else {
                        if (eyeClosedFrames >= 1) {
                            blinkDetected = true;
                        }
                        eyeClosedFrames = 0;
                    }
                    return blinkDetected;
                } finally {
                    if (eyes != null) {
                        eyes.release();
                    }
                    if (faceROI != null) {
                        faceROI.release();
                    }
                }
            }
            return false;
        } finally {
            if (faces != null) {
                faces.release();
            }
            if (gray != null) {
                gray.release();
            }
        }
    }

    private synchronized void loadCascadeClassifier(String resourcePath, String classifierType) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException(classifierType + " cascade file not found: " + resourcePath);
        }

        File tempFile = File.createTempFile("haarcascade_" + classifierType, ".xml");
        tempFile.deleteOnExit();
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
        if (classifier.empty()) {
            throw new Exception("Failed to load " + classifierType + " cascade classifier");
        }

        switch (classifierType) {
            case "face":
                faceCascade = classifier;
                break;
            case "eye":
                eyeCascade = classifier;
                break;

        }
    }

    public boolean addUiElements(BufferedImage image) {
        try {
            if (image == null) {
                throw new IllegalArgumentException("Input image cannot be null");
            }

            Mat frameMat = bufferedImageToMat(image);
            if (frameMat.empty()) {
                throw new IllegalArgumentException("Input image is empty");
            }

            AtomicReference<Double> qcScore = new AtomicReference<>(0.0);
            return detectFacesAndDrawICAO(frameMat, image, qcScore);
        } catch (Exception e) {
            System.err.println("Error adding UI elements: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private double calculateImageQuality(Mat grayImage) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(grayImage, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);

        double variance = stddev.get(0, 0)[0] * stddev.get(0, 0)[0];
        double maxVariance = 240.0;
        laplacian.release();
        return Math.min((variance / maxVariance) * 100.0, 100.0);
    }

    private boolean isFaceRatioValid(double faceRatio) {
        return faceRatio >= DESIRED_FACE_RATIO_MIN && faceRatio <= DESIRED_FACE_RATIO_MAX;
    }


    private double calculateFaceRatio(Rect face, Mat frame) {
        if (face == null || frame == null || frame.width() == 0 || frame.height() == 0) return 0.0;

        double frameArea = frame.width() * frame.height();

        // ICAO desired area ratios

        // Convert area ratios to box sizes (assume square boxes for guides)
        double minArea = frameArea * DESIRED_FACE_RATIO_MIN;
        double maxArea = frameArea * DESIRED_FACE_RATIO_MAX;

        int minBoxSize = (int) Math.sqrt(minArea);
        int maxBoxSize = (int) Math.sqrt(maxArea);

        // Calculate positions (centered)
        int minX = (frame.width() - minBoxSize) / 2;
        int minY = (frame.height() - minBoxSize) / 2;
        int maxX = (frame.width() - maxBoxSize) / 2;
        int maxY = (frame.height() - maxBoxSize) / 2;

        Rect minBox = new Rect(minX, minY, minBoxSize, minBoxSize);
        Rect maxBox = new Rect(maxX, maxY, maxBoxSize, maxBoxSize);

        // Check if face is within max box and outside min box
        boolean fitsWithinMax = maxBox.contains(face.tl()) && maxBox.contains(face.br());
        boolean exceedsMin = minBox.contains(face.tl()) || minBox.contains(face.br());

        if (fitsWithinMax && exceedsMin) {
            return 0.65; // midpoint ratio: face is compliant
        } else if (!fitsWithinMax) {
            return 0.9; // too big
        } else {
            return 0.4; // too small
        }
    }


    private boolean isFaceCentered(Rect face, Mat frame) {
        Point frameCenter = new Point(frame.width() / 2.0, frame.height() / 2.0);
        Point faceCenter = new Point(face.x + face.width / 2.0, face.y + face.height / 2.0);

        double xOffset = Math.abs(faceCenter.x - frameCenter.x) / frame.width();
        double yOffset = Math.abs(faceCenter.y - frameCenter.y) / frame.height();

        return xOffset < FACE_CENTER_TOLERANCE && yOffset < FACE_CENTER_TOLERANCE;
    }

    private boolean areEyesOpen(Mat faceROI) {
        MatOfRect eyes = new MatOfRect();
        eyeCascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0, new Size(20, 20), new Size());
        return eyes.toArray().length >= 2;
    }


    private boolean isBackgroundUniform(Mat image, Rect faceRect) {
        Mat mask = Mat.ones(image.size(), CvType.CV_8UC1);
        Imgproc.rectangle(mask, faceRect, new Scalar(0), -1);

        Mat bgOnly = new Mat();
        image.copyTo(bgOnly, mask);

        Mat gray = new Mat();
        Imgproc.cvtColor(bgOnly, gray, Imgproc.COLOR_BGR2GRAY);

        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(gray, new MatOfDouble(), stddev);

        return stddev.get(0, 0)[0] < BACKGROUND_UNIFORMITY_THRESHOLD;
    }

    private void drawErrorMessage(Graphics2D g2d, String message) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(message, 10, 50);
    }

    private void drawFaceRectangle(Graphics2D g2d, Rect faceRect, boolean isValid) {
        g2d.setColor(isValid ? Color.GREEN : Color.RED);
        g2d.drawRect(faceRect.x, faceRect.y, faceRect.width, faceRect.height);
    }

    private void drawInformationOverlay(Graphics2D g2d, Mat mat, Rect faceRect, double qcScore, double faceRatio, boolean centered, boolean eyesOpen) { //, boolean uniformBg boolean mouthClosed
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        int yPos = 25;

        // Draw quality score
        g2d.setColor(qcScore >= MIN_QC_SCORE ? Color.GREEN : Color.RED);
        g2d.drawString(String.format("QC: %.1f/100", qcScore), 10, yPos);
        yPos += 20;

        // Draw face ratio
        g2d.setColor(isFaceRatioValid(faceRatio) ? Color.GREEN : Color.RED);
        g2d.drawString(String.format("Face Ratio: %.1f%%", faceRatio * 100), 10, yPos);
        yPos += 20;

        // Draw centering status
        g2d.setColor(centered ? Color.GREEN : Color.RED);
        g2d.drawString("Centered: " + (centered ? "✓" : "✗"), 10, yPos);
        yPos += 20;

        // Draw eyes status
        g2d.setColor(eyesOpen ? Color.GREEN : Color.RED);
        g2d.drawString("Eyes: " + (eyesOpen ? "Open" : "Closed"), 10, yPos);
        yPos += 20;

        // Draw mouth status


//        g2d.setColor(mouthClosed ? Color.GREEN : Color.RED);
//        System.out.println("mouthClosed = " + mouthClosed);
//        g2d.drawString("Mouth: " + (mouthClosed ? "Closed" : "Open"), 10, yPos);
//        yPos += 20;

        // Draw background status
//        g2d.setColor(uniformBg ? Color.GREEN : Color.RED);
//        g2d.drawString("Background: " + (uniformBg ? "Uniform" : "Not Uniform"), 10, yPos);
    }


    private void drawFaceAreaGuides(Graphics2D g2d, Mat mat) {
        // Set color and optional stroke thickness
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2)); // optional for thicker border

        // Define the height of the rectangle using ICAO max ratio (or any fixed value)
        int rectHeight = (int) (mat.height() * DESIRED_FACE_RATIO_MAX); // 80% of image height
        int rectWidth = (int) (mat.width() * DESIRED_FACE_RATIO_MIN);   // 70% of image width

        // Center the rectangle
        int rectX = (mat.width() - rectWidth) / 2;
        int rectY = (mat.height() - rectHeight) / 2;

        // Draw the rectangle
        g2d.drawRect(rectX, rectY, rectWidth, rectHeight);
    }


//    public BufferedImage cropToPassportSize(BufferedImage originalImage) {
//        int passportWidth = 413;
//        int passportHeight = 531;
//
//        int imageWidth = originalImage.getWidth();
//        int imageHeight = originalImage.getHeight();
//
//        // Calculate cropping box centered in the image
//        int cropWidth = Math.min(passportWidth, imageWidth);
//        int cropHeight = Math.min(passportHeight, imageHeight);
//
//        int x = Math.max(0, (imageWidth - cropWidth) / 2);
//        int y = Math.max(0, (imageHeight - cropHeight) / 2);
//
//        // Ensure crop rectangle is within bounds
//        if (x + cropWidth > imageWidth) cropWidth = imageWidth - x;
//        if (y + cropHeight > imageHeight) cropHeight = imageHeight - y;
//
//        BufferedImage cropped = originalImage.getSubimage(x, y, cropWidth, cropHeight);
//
//        // Prepare the 480x600 canvas
//        BufferedImage resized = new BufferedImage(passportWidth, passportHeight, BufferedImage.TYPE_3BYTE_BGR);
//        Graphics2D g2 = resized.createGraphics();
//
//        // Fill background with white
//        g2.setColor(Color.WHITE);
//        g2.fillRect(0, 0, passportWidth, passportHeight);
//
//        // Resize proportionally
//        double scale = Math.min((double) passportWidth / cropWidth, (double) passportHeight / cropHeight);
//        int newW = (int) (cropWidth * scale);
//        int newH = (int) (cropHeight * scale);
//
//        int x1 = (passportWidth - newW) / 2;
//        int y1 = (passportHeight - newH) / 2;
//
//        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2.drawImage(cropped, x1, y1, newW, newH, null);
//        g2.dispose();
//
//        return resized;
//    }


    public BufferedImage cropToPassportSize(BufferedImage originalImage) {
        int passportWidth = 480;
        int passportHeight = 600;
        double targetRatio = (double) passportWidth / passportHeight; // 0.8

        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();
        double currentRatio = (double) imageWidth / imageHeight;

        int cropWidth, cropHeight, x, y;

        if (currentRatio > targetRatio) {
            // Image too wide → crop width
            cropHeight = imageHeight;
            cropWidth = (int) (cropHeight * targetRatio);
            x = (imageWidth - cropWidth) / 2;
            y = 0;
        } else {
            // Image too tall → crop height
            cropWidth = imageWidth;
            cropHeight = (int) (cropWidth / targetRatio);
            x = 0;
            y = (imageHeight - cropHeight) / 2;
        }

        // Crop to 4:5 ratio
        BufferedImage cropped = originalImage.getSubimage(x, y, cropWidth, cropHeight);

        BufferedImage resized = new BufferedImage(passportWidth, passportHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = resized.createGraphics();

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, passportWidth, passportHeight);

        float brightnessFactor = 1.2f; // 1.0 = original, >1 = brighter, <1 = darker
        RescaleOp rescaleOp = new RescaleOp(brightnessFactor, 0, null);
        BufferedImage brightened = rescaleOp.filter(cropped, null); // create a new brightened image

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(brightened, 0, 0, passportWidth, passportHeight, null);

        g2.dispose();

        return resized;
    }


    private Rect detectFaceRect(Mat matImage) {
        Mat gray = new Mat();
        try {
            Imgproc.cvtColor(matImage, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faceDetections = new MatOfRect();
            faceCascade.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(100, 100), new Size());

            Rect[] faces = faceDetections.toArray();
            if (faces.length == 0) {
                return null;
            }

            // Return largest face
            Rect largest = faces[0];
            for (Rect r : faces) {
                if (r.area() > largest.area()) {
                    largest = r;
                }
            }
            return largest;
        } finally {
            gray.release();
        }
    }

}