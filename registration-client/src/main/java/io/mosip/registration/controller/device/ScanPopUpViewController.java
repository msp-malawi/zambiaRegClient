package io.mosip.registration.controller.device;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.device.webcam.impl.WebcamSarxosServiceImpl;
import io.mosip.registration.util.common.RubberBandSelection;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Timer;
import java.util.TimerTask;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Controller
public class ScanPopUpViewController extends BaseController {
    private static final Logger LOGGER = AppConfig.getLogger(ScanPopUpViewController.class);
    static int interval;
    static Timer timer;

    @Autowired
    private BaseController baseController;

    @Autowired
    private DocumentScanController documentScanController;

    @Autowired
    private WebcamSarxosServiceImpl webcamSarxosServiceImpl;

    @FXML
    private Label popupTitle;

    @FXML
    private Text totalScannedPages;

    @FXML
    private Button saveBtn;

    @FXML
    private Text scanningMsg;

    private boolean isDocumentScan;

    @Autowired
    private Streamer streamer;

    @FXML
    private Hyperlink closeButton;

    public TextField streamerValue;

    @Value("${mosip.doc.stage.width}")
    private int width;

    @Value("${mosip.doc.stage.height}")
    private int height;

    @FXML
    protected Label docPreviewNext;

    @FXML
    protected Label docPreviewPrev;

    @FXML
    protected Text docCurrentPageNumber;

    @FXML
    protected GridPane previewOption;

    private Stage popupStage;

    @FXML
    private Button captureBtn;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button cropButton;

    @FXML
    private Button streamBtn;
    @FXML
    private Button previewBtn;

    @FXML
    private GridPane imageViewGridPane;

    @FXML
    private ImageView scanImage;

    @FXML
    private Group imageGroup;

    @FXML
    private Button faceCaptureBtn;

    @Autowired
    private BiometricsController biometricsController;

    @FXML
    private Text countDown;

    private Boolean flag;


    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }

    private boolean isStreamPaused;

    public boolean isStreamPaused() {
        return isStreamPaused;
    }

    private boolean isWebCamStream;

    public boolean isWebCamStream() {
        return isWebCamStream;
    }

    public void setWebCamStream(boolean isWebCamStream) {
        this.isWebCamStream = isWebCamStream;
    }

    /**
     * @return the popupStage
     */
    public Stage getPopupStage() {
        return popupStage;
    }

    /**
     * @return the scanImage
     */
    public ImageView getScanImage() {
        return scanImage;
    }

    /**
     * @param popupStage the popupStage to set
     */
    public void setPopupStage(Stage popupStage) {
        this.popupStage = popupStage;
    }

    /**
     * This method will open popup to scan
     *
     * @param parentControllerObj
     * @param title
     */
    public void init(BaseController parentControllerObj, String title) {

        try {

            LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                    "Opening pop-up screen to scan for user registration");

            streamerValue = new TextField();
            baseController = parentControllerObj;
            popupStage = new Stage();
            popupStage.initStyle(StageStyle.UNDECORATED);

            LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "loading scan.fxml");
            Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SCAN_PAGE));

            scanImage.fitWidthProperty().bind(imageViewGridPane.widthProperty());
            scanImage.fitHeightProperty().bind(imageViewGridPane.heightProperty());

            setDefaultImageGridPaneVisibility();
            popupStage.setResizable(false);
            popupTitle.setText(title);

            cropButton.setDisable(true);
            cancelBtn.setDisable(true);
            previewOption.setVisible(false);
            Scene scene = null;

            if (title.equalsIgnoreCase("face"))
                faceCaptureBtn.setVisible(true);

            if (!isDocumentScan) {
                scene = new Scene(scanPopup);
                captureBtn.setVisible(false);
                saveBtn.setVisible(false);
                cancelBtn.setVisible(false);
                cropButton.setVisible(false);
                previewBtn.setVisible(false);
                streamBtn.setVisible(false);
            } else {
                LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                        "Setting doc screen width : " + width);

                LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                        "Setting doc screen height : " + height);

                scene = new Scene(scanPopup, width, height);

                if (documentScanController.getScannedPages() != null
                        && !documentScanController.getScannedPages().isEmpty()) {

                    initializeDocPages(1, documentScanController.getScannedPages().size());

                    previewBtn.setDisable(false);
                } else {
                    saveBtn.setDisable(true);
                    cropButton.setDisable(true);
                    cancelBtn.setDisable(true);
                    previewBtn.setDisable(true);
                }
            }
            scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
            popupStage.setScene(scene);
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(fXComponents.getStage());
            popupStage.show();

            LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "scan screen launched");

            scanningMsg.textProperty().addListener((observable, oldValue, newValue) -> {

                Platform.runLater(() -> {
                    if (RegistrationUIConstants.NO_DEVICE_FOUND.contains(newValue)) {

                        // captureBtn.setDisable(false);

                        generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
                        popupStage.close();

                    }
                });

            });

            LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                    "Opening pop-up screen to scan for user registration");

        } catch (IOException ioException) {
            LOGGER.error(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                    String.format(
                            "%s -> Exception while Opening pop-up screen to capture in user registration  %s -> %s",
                            RegistrationConstants.USER_REG_SCAN_EXP, ioException.getMessage(),
                            ExceptionUtils.getStackTrace(ioException)));

            generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP);
        }

    }

    /**
     * This method will allow to scan
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    @FXML
    public void scan() throws MalformedURLException, IOException {
        scanningMsg.setVisible(true);
        LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                "Invoke scan method for the passed controller");
        baseController.scan(popupStage);

        if (isDocumentScan) {

            String docNumber = docCurrentPageNumber.getText();

            totalScannedPages.setText(String.valueOf(documentScanController.getScannedPages().size()));
            if (docNumber == null || docNumber.isEmpty() || docCurrentPageNumber.getText().isEmpty()
                    || docCurrentPageNumber.getText().equals("0")) {
                docCurrentPageNumber.setText("1");
                docPreviewPrev.setDisable(true);
                docPreviewNext.setDisable(true);

            } else {
                docCurrentPageNumber.setText(String.valueOf(documentScanController.getScannedPages().size()));
                docPreviewPrev.setDisable(false);
                docPreviewNext.setDisable(true);
            }

//			previewOption.setVisible(true);

            if (!documentScanController.getScannedPages().isEmpty()) {
                previewBtn.setDisable(false);
                saveBtn.setDisable(false);
            } else {
                previewBtn.setDisable(true);
            }
        }

        generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.DOC_CAPTURE_SUCCESS);
    }

    /**
     * event class to exit from present pop up window.
     *
     * @param event
     */
    public void exitWindow(ActionEvent event) {

        LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                "Calling exit window to close the popup");

        biometricsController.stopRCaptureService();
        biometricsController.stopDeviceSearchService();
        isStreamPaused = false;
        streamer.stop();
        if (webcamSarxosServiceImpl.isWebcamConnected()) {
            webcamSarxosServiceImpl.close();
        }
        popupStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
        popupStage.close();

        if (documentScanController.getScannedPages() != null) {
            documentScanController.getScannedPages().clear();
        }

        LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Popup is closed");

    }

    public void enableCloseButton() {
        if (null != closeButton)
            closeButton.setDisable(false);
    }

    public void disableCloseButton() {
        if (null != closeButton)
            closeButton.setDisable(true);
    }

    @FXML
    private void save() {
        webcamSarxosServiceImpl.close();
        isStreamPaused = false;
        flag = false;
        setDefaultImageGridPaneVisibility();
        // Enable Auto-Logout
        SessionContext.setAutoLogout(true);
        if (baseController instanceof DocumentScanController) {
            DocumentScanController documentScanController = (DocumentScanController) baseController;
            try {
                documentScanController.attachScannedDocument(popupStage);

                documentScanController.getScannedPages().clear();
                popupStage.close();
            } catch (IOException | RuntimeException ioException) {
                LOGGER.error(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                        ExceptionUtils.getStackTrace(ioException));
                generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
            }
        }

    }

    public boolean isDocumentScan() {
        return isDocumentScan;
    }

    public void setDocumentScan(boolean isDocumentScan) {
        this.isDocumentScan = isDocumentScan;
    }

    public Text getScanningMsg() {
        return scanningMsg;
    }

    public void setScanningMsg(String msg) {
        if (scanningMsg != null) {
            scanningMsg.setText(msg);
            scanningMsg.getStyleClass().add("scanButton");
        }
    }

    public void setDefaultImageGridPaneVisibility() {

        LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
                "Setting default visibilities for webCamParent and imageParent");
//		webcamParent.setVisible(false);
//		imageParent.setVisible(true);
    }

    /**
     * This method will preview the next document
     */
    public void previewNextPage() {

        Integer docNumber = getDocPreviewNumber();

        if (docNumber != null && docNumber > 0) {

            int previousDocNumber = docNumber;

            BufferedImage bufferedImage = null;
            if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

                bufferedImage = documentScanController.getScannedImage(previousDocNumber);

            }

            if (bufferedImage != null) {

                scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                docCurrentPageNumber.setText(String.valueOf(previousDocNumber + 1));

                docPreviewPrev.setDisable(false);

                if (documentScanController.getScannedPages().size() == (previousDocNumber + 1)) {

                    docPreviewNext.setDisable(true);
                }

            }

        }
    }

    /**
     * This method will preview the previous document
     */
    public void previewPrevPage() {

        Integer docNumber = getDocPreviewNumber();

        if (docNumber != null && docNumber > 0) {

            int previousDocNumber = docNumber - 2;

            BufferedImage bufferedImage = null;

            if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

                bufferedImage = documentScanController.getScannedImage(previousDocNumber);

            }

            if (bufferedImage != null) {

                scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                docCurrentPageNumber.setText(String.valueOf(docNumber - 1));

                docPreviewNext.setDisable(false);

                if ((previousDocNumber) == 0) {

                    docPreviewPrev.setDisable(true);
                }
            }

        }
    }

    private Integer getDocPreviewNumber() {

        Integer docNumber = null;
        String docPreviewNumber = docCurrentPageNumber.getText();

        if (docPreviewNumber != null && !docPreviewNumber.isEmpty()) {

            docNumber = Integer.valueOf(docPreviewNumber);

        }

        return docNumber;
    }

//	@FXML
//	public void crop() {
//
//		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//				"crop has been selected");
//		isStreamPaused = true;
//		scanImage.setVisible(true);
//		RubberBandSelection rubberBandSelection = new RubberBandSelection(imageGroup);
//
//		rubberBandSelection.setscanPopUpViewController(this);
//
//		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//				"Shown stage for crop");
//
//	}

    public void save(Bounds bounds) throws IOException {

        LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
                "Saving cropped image");

        if (bounds.getHeight() == 1.0 || bounds.getWidth() == 1.0)
            return;

        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), width, height));

        WritableImage wi = new WritableImage(width, height);
        scanImage.snapshot(parameters, wi);

        BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
        BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(),
                BufferedImage.OPAQUE);

        Graphics2D graphics = bufImageRGB.createGraphics();
        graphics.drawImage(bufImageARGB, 0, 0, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufImageRGB, "jpg", baos);

        LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
                "Saving cropped image into session");

        int pageNumber = Integer
                .valueOf(docCurrentPageNumber.getText().isEmpty() ? "1" : docCurrentPageNumber.getText());

        documentScanController.getScannedPages().remove(pageNumber - 1);
        documentScanController.getScannedPages().add(pageNumber - 1,
                ImageIO.read(new ByteArrayInputStream(baos.toByteArray())));

        scanImage.setImage(SwingFXUtils.toFXImage(documentScanController.getScannedPages().get(pageNumber - 1), null));

        graphics.dispose();
        generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.CROP_DOC_SUCCESS);
        primaryStage.close();
//		if (webcamSarxosServiceImpl.isWebcamConnected()) {
//			scanImage.setVisible(false);
//			webcamNode.setVisible(true);
//		}

        showPreview(true);

//		cropStage.close();

        LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
                "Saving cropped image completed");

    }

    @FXML
    public void cancel() {

        int currentDocPageNumber = Integer.valueOf(docCurrentPageNumber.getText());
        int pageNumberIndex = currentDocPageNumber - 1;

        // Remove current page
        documentScanController.getScannedPages().remove(pageNumberIndex);

        generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.DOC_DELETE_SUCCESS);
        // If first page
        if (currentDocPageNumber == 1) {

            // Remove current doc
            if (documentScanController.getScannedPages().size() > 0) {

                BufferedImage bufferedImage = documentScanController.getScannedImage(0);

                if (bufferedImage != null) {

                    scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                }

                initializeDocPages(1, documentScanController.getScannedPages().size());

            } else {

                scanImage.setImage(null);

                initializeDocPages(0, 0);

                previewOption.setVisible(false);

            }
        }

        // If last page
        else if (currentDocPageNumber == documentScanController.getScannedPages().size() + 1) {

            BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex - 1);

            if (bufferedImage != null) {

                scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                initializeDocPages(currentDocPageNumber - 1, documentScanController.getScannedPages().size());

            }
        }

        // If middle page
        else {
            BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex);

            if (bufferedImage != null) {

                scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                initializeDocPages(currentDocPageNumber, documentScanController.getScannedPages().size());

            }
        }

        if (!documentScanController.getScannedPages().isEmpty()) {
            previewBtn.setDisable(false);
            saveBtn.setDisable(false);
        } else {
            previewBtn.setDisable(true);
            saveBtn.setDisable(true);
            cancelBtn.setDisable(true);
            cropButton.setDisable(true);
        }
    }

    private void initializeDocPages(int currentPage, int totalPages) {
        docCurrentPageNumber.setText(String.valueOf(currentPage));

        totalScannedPages.setText(String.valueOf(totalPages));

        boolean prevPageDisable = currentPage > 1 ? false : true;

        docPreviewPrev.setDisable(prevPageDisable);

        boolean nextPageDisable = currentPage < totalPages ? false : true;

        docPreviewNext.setDisable(nextPageDisable);

    }

    @FXML
    public void stream() {
        showPreview(false);
        showStream(false);

        cancelBtn.setDisable(true);
        cropButton.setDisable(true);
        flag = false;
    }

    @FXML
    public void preview() {
        flag = true;
        showStream(true);
        showPreview(true);
        if (documentScanController.getScannedPages().size() > 1 && docCurrentPageNumber.getText().equals("1"))
            scanImage.setImage(SwingFXUtils.toFXImage(
                    documentScanController.getScannedImage(documentScanController.getScannedPages().size() - 2), null));
        else
            scanImage.setImage(SwingFXUtils.toFXImage(
                    documentScanController.getScannedImage(documentScanController.getScannedPages().size() - 1), null));

    }

    private void showPreview(boolean isVisible) {
        previewOption.setVisible(isVisible);
        scanImage.setVisible(true);
        cancelBtn.setDisable(false);
        cropButton.setDisable(false);

    }

    private void showStream(boolean isVisible) {
        isStreamPaused = isVisible;
    }

    @FXML
    public void faceCapture(ActionEvent actionEvent) {
        faceCaptureBtn.setVisible(false);
        closeButton.setVisible(false);
        String secs = "15";
        int delay = 1000;
        int period = 1000;
        timer = new Timer();
        interval = Integer.parseInt(secs);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    countDown.setText(Integer.toString(setInterval()));
                    countDown.setFont(Font.font("Verdana", 20));
                    countDown.setFill(Color.BLUE);
                    if (Integer.parseInt(countDown.getText()) <= 5)
                        countDown.setFill(Color.RED);
                });
            }
        }, delay, period);
        biometricsController.rCaptureTaskService();
    }

    private static final int setInterval() {
        if (interval == 1)
            timer.cancel();
        return --interval;
    }

    RubberBandSelection rubberBandSelection;
    ImageView imageView;
    Stage primaryStage;

    @FXML
    public void crop() throws IOException {
        Stage primaryStage = new Stage();
        BorderPane root = new BorderPane();
        isStreamPaused = true;
        scanImage.setVisible(true);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();

        Group imageLayer = new Group();

        Image image = scanImage.getImage();

//		BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
//		ByteArrayOutputStream s = new ByteArrayOutputStream();
//		ImageIO.write(bImage, "png", s);
//		byte[] res  = s.toByteArray();
//		InputStream in = new ByteArrayInputStream(res);
//		BufferedImage bImageFromConvert = ImageIO.read(in);
//
//		ImageIO.write(bImageFromConvert, "png", new File(
//				"D:\\test.png"));

        imageView = new ImageView(image);
//		imageView.setPreserveRatio(true);
//		imageView.setFitHeight(720);
//		imageView.setFitWidth(1200);
        //imageView.fitHeightProperty().bind(primaryStage.heightProperty());
        //imageView.fitWidthProperty().bind(primaryStage.widthProperty());

        imageLayer.getChildren().add(imageView);

        scrollPane.setContent(imageLayer);

        scrollPane.setPrefSize(image.getWidth(), image.getHeight());
       // scrollPane.setPannable(true);
       scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scrollPane);

        rubberBandSelection = new RubberBandSelection(imageLayer);

//        saveButton.setOnAction(e -> {
//
//            // get bounds for image crop
//            Bounds selectionBounds = rubberBandSelection.getBounds();
//
//            // show bounds info
//            System.out.println( "Selected area: " + selectionBounds);
//
//            // crop the image
//            save( selectionBounds);
//
//        });

        primaryStage.setScene(new Scene(root, image.getWidth(), 660));
        primaryStage.setMaximized(false);
        primaryStage.setTitle("Crop Document");
        primaryStage.setResizable(false);
        //primaryStage.initStyle(StageStyle.UNDECORATED);

        // primaryStage.getIcons().add(new Image("file:@../resources/images/PSAlogotransparent.png"));
        primaryStage.show();
        this.primaryStage = primaryStage;

    }

    private void saves(Bounds bounds) throws IOException {
        if (bounds.getHeight() < 50 || bounds.getWidth() < 50)
            return;

        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), width, height));


        WritableImage wi = new WritableImage(width, height);
        imageView.snapshot(parameters, wi);

        BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
        BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);

        Graphics2D graphics = bufImageRGB.createGraphics();
        graphics.drawImage(bufImageARGB, 0, 0, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufImageRGB, "jpg", baos);


        int pageNumber = Integer
                .valueOf(docCurrentPageNumber.getText().isEmpty() ? "1" : docCurrentPageNumber.getText());

        documentScanController.getScannedPages().remove(pageNumber - 1);
        documentScanController.getScannedPages().add(pageNumber - 1,
                ImageIO.read(new ByteArrayInputStream(baos.toByteArray())));
        scanImage.setImage(SwingFXUtils.toFXImage(documentScanController.getScannedPages().get(pageNumber - 1), null));

        graphics.dispose();
        generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.CROP_DOC_SUCCESS);
        primaryStage.close();

    }

    /**
     * Drag rectangle with mouse cursor in order to get selection bounds
     */
    public class RubberBandSelection {

        final DragContext dragContext = new DragContext();
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle();

        Group group;


        public Bounds getBounds() {
            return rect.getBoundsInParent();
        }

        public RubberBandSelection(Group group) {

            this.group = group;

            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setStrokeWidth(1);
            rect.setStrokeLineCap(StrokeLineCap.ROUND);
            rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

            group.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
            group.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
            group.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

        }

        EventHandler
                <MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.isSecondaryButtonDown())
                    return;

                // remove old rect
                rect.setX(0);
                rect.setY(0);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().remove(rect);


                // prepare new drag operation
                dragContext.mouseAnchorX = event.getX();
                dragContext.mouseAnchorY = event.getY();

                rect.setX(dragContext.mouseAnchorX);
                rect.setY(dragContext.mouseAnchorY);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().add(rect);

            }
        };

        EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (event.isSecondaryButtonDown())
                    return;

                double offsetX = event.getX() - dragContext.mouseAnchorX;
                double offsetY = event.getY() - dragContext.mouseAnchorY;

                if (offsetX > 0)
                    rect.setWidth(offsetX);
                else {
                    rect.setX(event.getX());
                    rect.setWidth(dragContext.mouseAnchorX - rect.getX());
                }

                if (offsetY > 0) {
                    rect.setHeight(offsetY);
                } else {
                    rect.setY(event.getY());
                    rect.setHeight(dragContext.mouseAnchorY - rect.getY());
                }
            }
        };


        EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {

            @SneakyThrows
            @Override
            public void handle(MouseEvent event) {

//                if( event.isSecondaryButtonDown())
//                    return;

                // get bounds for image crop
                Bounds selectionBounds = rubberBandSelection.getBounds();

                // crop the image
                saves(selectionBounds);
                // remove rectangle
                // note: we want to keep the ruuberband selection for the cropping => code is just commented out
                /*
                rect.setX(0);
                rect.setY(0);
                rect.setWidth(0);
                rect.setHeight(0);

                group.getChildren().remove( rect);
                */

            }
        };

        private final class DragContext {

            public double mouseAnchorX;
            public double mouseAnchorY;


        }
    }


}
