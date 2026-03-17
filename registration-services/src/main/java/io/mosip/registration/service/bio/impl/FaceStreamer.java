package io.mosip.registration.service.bio.impl;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mosip.registration.constants.LoggerConstants.FACE_STREAMER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Service
public class FaceStreamer {
    private BufferedImage currentFaceFrame;

    private static final Logger LOGGER = AppConfig.getLogger(FaceStreamer.class);

    private final AtomicBoolean blinkDetected = new AtomicBoolean(false);
    private final AtomicBoolean cameraActive = new AtomicBoolean(false);
    private volatile double qualityScore;
    private VideoCapture faceCam;
    private ScheduledExecutorService cameraTimer;

//    @Autowired
//    @Qualifier("webcamSarxosServiceImpl")
//    private WebcamSarxosServiceImpl sarxosService;

    public void setFaceCam(VideoCapture faceCam) {
        this.faceCam = faceCam;
    }

    public boolean isCameraActive() {
        return cameraActive.get();
    }

    public void setCameraActive(boolean active) {
        cameraActive.set(active);
    }

    public void setCameraTimer(ScheduledExecutorService cameraTimer) {
        this.cameraTimer = cameraTimer;
    }


    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void stopCam() {
        if (!cameraActive.getAndSet(false)) {
            return; // Already stopped
        }

        try {
            if (cameraTimer != null) {
                try {
                    cameraTimer.shutdownNow();
                    if (!cameraTimer.awaitTermination(1, TimeUnit.SECONDS)) {
                        LOGGER.info(FACE_STREAMER, APPLICATION_NAME, APPLICATION_ID, "Camera timer shutdown success");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error(FACE_STREAMER, APPLICATION_NAME, APPLICATION_ID, "Interrupted stopping camera timer");
                }
            }

            // Close camera safely
            if (faceCam != null && faceCam.isOpened()) {
                faceCam.release();
            }

        } finally {
            cameraActive.set(false);
        }
    }

    public void setBlinkDetected(boolean detected) {
        blinkDetected.set(detected);
    }

    public boolean isBlinkDetected() {
        return blinkDetected.get();
    }

    public boolean isCameraReady() {
        return cameraActive.get() && faceCam != null && faceCam.isOpened();
    }


    public void setFaceCamFrame(BufferedImage faceFrame) {
        this.currentFaceFrame = faceFrame;
    }

    public BufferedImage getFaceCamFrame() {
        return this.currentFaceFrame;
    }
}
