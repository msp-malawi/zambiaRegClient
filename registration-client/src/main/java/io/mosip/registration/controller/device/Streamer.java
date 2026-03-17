package io.mosip.registration.controller.device;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.service.bio.impl.FaceDetection;
import io.mosip.registration.service.bio.impl.FaceStreamer;
import io.mosip.registration.service.bio.impl.OpenCVUtils;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mosip.registration.constants.LoggerConstants.STREAMER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Component
public class Streamer {

	private static final Logger LOGGER = AppConfig.getLogger(Streamer.class);

	private static final String CONTENT_LENGTH = "Content-Length:";
	private static final long CAMERA_TIMEOUT_SECONDS = 30;
	private static final long SHUTDOWN_TIMEOUT_MS = 500;
	private static final int BLINK_CONFIRMATION_COUNT = 3;
	private static final int FRAME_RATE_MS = 30; // ~30 FPS

	private static Image streamImage;
	private static ImageView imageView;
	private final AtomicBoolean closeAfterCapture = new AtomicBoolean(false);
	private final AtomicBoolean cameraActive = new AtomicBoolean(false);
	private final AtomicBoolean shouldStop = new AtomicBoolean(false);
	private final AtomicInteger blinkCount = new AtomicInteger(0);
	private boolean isRunning = false;
	private byte[] imageBytes = null;
	private ScheduledExecutorService cameraTimer;
	private ScheduledFuture<?> captureTask;
	private ScheduledFuture<?> timeoutTask;
	@Autowired
	private FaceStreamer faceStreamer;

	@Autowired
	private FaceDetection faceDetection;
	private volatile VideoCapture camera;
	private InputStream urlStream;
	private volatile Thread streamerThread = null;


	public void setUrlStream(InputStream inputStream) {

		if (urlStream != null) {
			try {
				urlStream.close();
			} catch (IOException exception) {
				LOGGER.error(STREAMER, RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			}
			urlStream = null;
		}

		if (inputStream != null) {
			this.urlStream = inputStream;
			isRunning = true;
		} else {
			this.urlStream = null;
			isRunning = false;
		}

	}

	public InputStream getUrlStream() {
		return urlStream;
	}



	private Thread streamer_thread = null;



	public void setStreamImage(Image streamImage) {
		this.streamImage = streamImage;
	}

	// Get Streaming image
	public Image getStreamImage() {
		return streamImage;
	}

	public byte[] getStreamImageBytes() {
		return imageBytes;
	}

	// Set ImageView
	public static void setImageView(ImageView imageView) {
		Streamer.imageView = imageView;
	}

	// Set Streaming image to ImageView
	public void setStreamImageToImageView() {
		imageView.setImage(streamImage);
	}

	public void startStream(InputStream inputStream, ImageView streamImage, ImageView scanImage) {

		LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID,
				"Streamer Thread initiation started for : " + System.currentTimeMillis());

		streamer_thread = new Thread(new Runnable() {

			public void run() {

				setUrlStream(inputStream);

				while (null != urlStream) {
					try {
						imageBytes = retrieveNextImage(urlStream);
						ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes);
						Image img = new Image(imageStream);
						streamImage.setImage(img);
						if(scanImage == null){
							streamImage.setFitHeight(350.0);
							streamImage.setFitWidth(350.0);
							streamImage.setX(-45.0);
							if(img.getHeight()<720.0d){
								streamImage.setX(25.0);
								streamImage.setFitHeight(250.0);
								streamImage.setFitWidth(250.0);
							}
						}
						if (null != scanImage) {
							// scanImage.setImage(img);
//							System.out.println("inside scan image"+scanImage.getFitHeight() +"* "+scanImage.getFitWidth());
							setImageView(scanImage);
							setStreamImage(img);
						}
					} catch (RuntimeException | IOException exception) {

						LOGGER.error(STREAMER, RegistrationConstants.APPLICATION_NAME,
								RegistrationConstants.APPLICATION_ID,
								exception.getMessage() + ExceptionUtils.getStackTrace(exception));
						urlStream = null;

					}
				}
			}

		}, "STREAMER_THREAD");

		streamer_thread.start();

		LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID,
				"Streamer Thread initiated completed for : " + System.currentTimeMillis());

	}

	/**
	 * Using the urlStream get the next JPEG image as a byte[]
	 *
	 * @return byte[] of the JPEG
	 * @throws IOException
	 */
	public byte[] retrieveNextImage(InputStream urlStream) throws IOException {

		int currByte = -1;

		boolean captureContentLength = false;
		StringWriter contentLengthStringWriter = new StringWriter(128);
		StringWriter headerWriter = new StringWriter(128);

		int contentLength = 0;

		while ((currByte = urlStream.read()) > -1) {
			if (captureContentLength) {
				if (currByte == 10 || currByte == 13) {
					contentLength = Integer.parseInt(contentLengthStringWriter.toString().replace(" ", ""));
					break;
				}
				contentLengthStringWriter.write(currByte);

			} else {
				headerWriter.write(currByte);
				String tempString = headerWriter.toString();
				int indexOf = tempString.indexOf(CONTENT_LENGTH);
				if (indexOf > 0) {
					captureContentLength = true;
				}
			}
		}

		// 255 indicates the start of the jpeg image
		while (urlStream.read() != 255) {

		}

		// && urlStream.read()!=-1
		// if(urlStream.read()==-1) {
		// throw new RuntimeException("No stream available");
		// }

		// rest is the buffer
		byte[] imageBytes = new byte[contentLength + 1];
		// since we ate the original 255 , shove it back in
		imageBytes[0] = (byte) 255;
		int offset = 1;
		int numRead = 0;
		while (offset < imageBytes.length
				&& (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
			offset += numRead;
		}

		return imageBytes;
	}

	/**
	 * Stop the loop, and allow it to clean up
	 */
	public void startCameraCapture(String modality, ImageView streamImageView, ImageView scanImageView) {
		System.out.println("[TRACE] Entering startCameraCapture()");

		if (camera == null) {
			System.out.println("[TRACE] Creating new VideoCapture instance");
			camera = new VideoCapture();
		}

		if (streamImageView == null || scanImageView == null) {
			System.out.println("[ERROR] ImageView references are null");
			LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "ImageView references are null");
			return;
		}

		System.out.println("[TRACE] Starting OpenCV camera capture...");
		LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Starting OpenCV camera capture...");

		if (camera.isOpened()) {
			System.out.println("[TRACE] Camera was already open - releasing first");
			camera.release();
		}

		if (!camera.open(0, Videoio.CAP_DSHOW)) {
			System.out.println("[ERROR] Failed to open camera device");
			return;
		}

		System.out.println("[TRACE] Setting camera properties (800x640)");
		camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 800);
		camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 640);

		System.out.println("[TRACE] Setting cameraActive to true");
		cameraActive.set(true);

		System.out.println("[TRACE] Starting frame capture thread");
		startFrameCapture(modality, streamImageView, scanImageView);
	}

	private void startFrameCapture(String modality, ImageView streamImageView, ImageView scanImageView) {
		System.out.println("[TRACE] Entering startFrameCapture()");

		System.out.println("[TRACE] Shutting down previous timer if exists");
		shutdownTimer(cameraTimer);

		if (streamImageView == null || scanImageView == null) {
			System.out.println("[ERROR] ImageView references are null in startFrameCapture");
			LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "ImageView references are null");
			return;
		}

		System.out.println("[TRACE] Creating new single thread executor");
		cameraTimer = Executors.newSingleThreadScheduledExecutor();

		captureTask = cameraTimer.scheduleWithFixedDelay(() -> {

			if (cameraActive.get() && camera != null && camera.isOpened()) {
				try {
					System.out.println("[TRACE] Calling captureAndProcessFrameSafe");
					captureAndProcessFrameSafe(modality, streamImageView, scanImageView);
				} catch (Exception e) {
					System.out.println("[ERROR] Exception in frame capture task: " + e.getMessage());
					LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Error capturing frame");
				}
			} else {
				System.out.println("[WARN] Camera not active or not opened");
			}
		}, 0, FRAME_RATE_MS, TimeUnit.MILLISECONDS);

		System.out.println("[TRACE] Scheduling timeout task");
		timeoutTask = cameraTimer.schedule(() -> {
			System.out.println("[TIMEOUT] Camera timeout after " + CAMERA_TIMEOUT_SECONDS + " seconds");
			LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID,
					"Camera timeout after " + CAMERA_TIMEOUT_SECONDS + " seconds");
			stop();
		}, CAMERA_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	private void captureAndProcessFrameSafe(String modality, ImageView streamImageView, ImageView scanImageView) {
		System.out.println("[TRACE] Entering captureAndProcessFrameSafe()");

		if (!cameraActive.get() || shouldStop.get() || streamImageView == null || scanImageView == null) {
			System.out.println("[WARN] Early exit - camera not active or views null");
			return;
		}

		BufferedImage originalImage = null;
		BufferedImage displayImage = null;
		Mat frame = new Mat();

		try {
			if (!camera.read(frame) || frame.empty()) {
				LOGGER.warn(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Cannot grab frame from camera");
				return;
			}

			originalImage = OpenCVUtils.matToBufferedImage(frame);

			if (originalImage != null) {
				displayImage = OpenCVUtils.copyBufferedImage(originalImage);

				boolean blinkDetected;

				if (!modality.equalsIgnoreCase("Exception_Photo") && !modality.equalsIgnoreCase("Exception")) {
					faceDetection.addUiElements(displayImage);

					blinkDetected = faceDetection.detectBlink(frame);
				} else {
					blinkDetected = false;
				}

				Image fxImage = SwingFXUtils.toFXImage(displayImage, null);

				Platform.runLater(() -> {
					updateStreamImageView(streamImageView, scanImageView, fxImage);

					if (faceStreamer != null && blinkDetected) {
						System.out.println("[FX] Setting blink detected flag");
						faceStreamer.setBlinkDetected(true);
					}
				});

				if (faceStreamer != null) {
					System.out.println("[TRACE] Setting face cam frame");
					faceStreamer.setFaceCamFrame(originalImage);
				}
			}

		} catch (Exception e) {
			System.out.println("[TRACE] Cleaning up resources");
			if (displayImage != null) displayImage.flush();
			if (originalImage != null) originalImage.flush();
			frame.release();
			System.out.println("[ERROR] Exception in captureAndProcessFrameSafe: " + e.getMessage());
			LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Error in captureAndProcessFrame");
		} finally {
			System.out.println("[TRACE] Cleaning up resources");
			if (displayImage != null) displayImage.flush();
			if (originalImage != null) originalImage.flush();
			frame.release();
		}
	}


	private void updateStreamImageView(ImageView
											   streamImageView, ImageView scanImageView, Image image) {
		if (image == null) {
			return;
		}

		try {
			streamImageView.setImage(image);

			if (scanImageView == null) {
				streamImageView.setFitHeight(450.0);
				streamImageView.setFitWidth(350.0);
				streamImageView.setX(-45.0);

				if (image.getHeight() < 720.0d) {
					streamImageView.setX(25.0);
					streamImageView.setFitHeight(250.0);
					streamImageView.setFitWidth(250.0);
				}
			}

			if (scanImageView != null) {
				setImageView(scanImageView);
				setStreamImage(image);
			}
		} catch (Exception exception) {
			LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Image update error: " + exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			faceStreamer.stopCam();
		}
	}

	public synchronized void stopCameraCapture() {
		if (!cameraActive.getAndSet(false)) {
			return;
		}

		shouldStop.set(true);

		try {
			if (captureTask != null) {
				captureTask.cancel(true);
				captureTask = null;
			}

			if (timeoutTask != null) {
				timeoutTask.cancel(true);
				timeoutTask = null;
			}

			shutdownTimer(cameraTimer);

			if (camera != null && camera.isOpened()) {
				camera.release();
			}

			faceStreamer.stopCam();
			LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Camera stopped successfully");
		} catch (Exception e) {
			LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Error during camera shutdown: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
		} finally {
			cameraTimer = null;
			camera = null;
			shouldStop.set(false);
			blinkCount.set(0);
		}
	}

	private void shutdownTimer(ScheduledExecutorService timer) {
		if (timer != null) {
			try {
				timer.shutdownNow();
				if (!timer.awaitTermination(100, TimeUnit.MILLISECONDS)) {
					LOGGER.warn(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Timer didn't terminate properly");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Interrupted while shutting down timer: " + e.getMessage());
			}
		}
	}

	private void handleCameraError(String message) {
		LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, message);
		stopCameraCapture();
	}

	public synchronized void stop() {
		System.out.println(" =stop " );
		SessionContext.setAutoLogout(true);
		stopCameraCapture();

		if (streamerThread != null) {
			streamerThread.interrupt();
			try {
				streamerThread.join(SHUTDOWN_TIMEOUT_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.error(STREAMER, APPLICATION_NAME, APPLICATION_ID, "Thread interruption error: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
			}
			streamerThread = null;
		}

		closeInputStream();
	}

	private synchronized void closeInputStream() {
		if (urlStream != null) {
			try {
				urlStream.close();
			} catch (IOException exception) {
				LOGGER.error(STREAMER, RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, "Error closing input stream: " + exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			}
			urlStream = null;
		}
	}
}
