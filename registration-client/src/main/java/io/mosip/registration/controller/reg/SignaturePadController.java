package io.mosip.registration.controller.reg;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.BiometricsController;
//import io.mosip.registration.device.fp.MantraCaptureResponse;
//import io.mosip.registration.device.fp.MantraDeviceCapture;
import io.mosip.registration.device.scanner.IMosipDocumentScannerService;
//import io.mosip.registration.dto.FingerType;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
//import io.mosip.registration.service.bio.impl.MantraFingerCapture;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachingConfigurationSelector;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Queue;
import java.util.*;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Controller
public class SignaturePadController extends BaseController {
    private Boolean signatureException=false;

    private static final String CAPTURE_API = "http://127.0.0.1:8032/morfinenroll/capture";
    private static final Logger LOGGER = AppConfig.getLogger(SignaturePadController.class);
    static Map<String, Boolean> fingerPositionMap = new HashMap<>();
    static Map<String, String> uiAttibuteMap = new HashMap<>();
    private final Map<String, Queue<String>> slabToFingerOrder = Map.of("0", new ArrayDeque<>(List.of("leftLittle", "leftRing", "leftMiddle", "leftIndex")), "1", new ArrayDeque<>(List.of("rightIndex", "rightMiddle", "rightRing", "rightLittle")), "2", new ArrayDeque<>(List.of("leftThumb", "rightThumb")));
    private final boolean isDrawing = false;
    public Button captureFinger;
    @FXML
    public ComboBox thumbsComboBox;
    @FXML
    public Hyperlink closeButton;
    public Label popupTitle;
    @FXML
    public HBox thumbsHbox;
    @Autowired
    RestTemplate restTemplate;
    @FXML
    CheckBox exceptionCheckBox;
    @Autowired
    private BiometricsController biometricsController;
    @FXML
    private ImageView signatureImageView;

    private Stage stage;
    @Qualifier("documentScannerServiceImpl")
    @Autowired
    private IMosipDocumentScannerService documentScannerService;



    public String getUiAttribute(String slab) {
        return slabToFingerOrder.getOrDefault(slab, new ArrayDeque<>()).poll();
    }


    @FXML
    public void initialize() {

//        thumbsComboBox.setItems(FXCollections.observableArrayList(FingerType.values()));
//        thumbsComboBox.getSelectionModel().selectFirst();
//
//        // Optional: print when changed
//        thumbsComboBox.setOnAction(event -> {
//            FingerType selected = (FingerType) thumbsComboBox.getSelectionModel().getSelectedItem();
//            if (selected != null) {
//                System.out.println("Selected from dropdown: " + selected);
//            }
//        });

//

    }



    @FXML
    public void signatureMethode() throws IOException, InterruptedException {
        System.out.println("=== Starting getSignatureImage ===");

        File signatureFile = new File("signature.jpg");
        File pngFile = new File("signature_converted.png");

        Files.deleteIfExists(signatureFile.toPath());
        Files.deleteIfExists(pngFile.toPath());
        System.out.println("Deleted old signature files if present.");

        // Start signature capture
        try {
            Process process = new ProcessBuilder("java", "-cp", "Wacom-0.0.1-SNAPSHOT.jar;wgssSTU.jar", "com.exampleDemoButtons.DemoButtons").inheritIO().start();
            int exitCode = process.waitFor();
            System.out.println("Capture process exited with code: " + exitCode);

            if (exitCode != 0 || !signatureFile.exists()) {
                System.out.println("exitCode = " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Load the image
        BufferedImage jpgImage = ImageIO.read(signatureFile);
        int originalWidth = jpgImage.getWidth();
        int originalHeight = jpgImage.getHeight();

        // Make white background transparent
        BufferedImage cleanedImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int pixel = jpgImage.getRGB(x, y);
                int alpha = (pixel == java.awt.Color.WHITE.getRGB()) ? 0 : 255;
                int rgb = pixel & 0x00FFFFFF;
                cleanedImage.setRGB(x, y, (alpha << 24) | rgb);
            }
        }

        // Detect bounds of signature (non-transparent area)
        int minX = originalWidth, minY = originalHeight, maxX = 0, maxY = 0;
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int alpha = (cleanedImage.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // No signature detected
        if (minX >= maxX || minY >= maxY) {
            System.out.println("minX = " + minX);
        }

        // Crop the signature
        int sigWidth = maxX - minX + 1;
        int sigHeight = maxY - minY + 1;
        BufferedImage croppedSignature = cleanedImage.getSubimage(minX, minY, sigWidth, sigHeight);

        // Create a new transparent image same size as original
        BufferedImage centeredImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = centeredImage.createGraphics();

        // Calculate center offset
        int xOffset = (originalWidth - sigWidth) / 2;
        int yOffset = (originalHeight - sigHeight) / 2;

        // Draw cropped signature in center
        g2d.drawImage(croppedSignature, xOffset, yOffset, null);
        g2d.dispose();

        // Save as transparent PNG
        ImageIO.write(centeredImage, "png", pngFile);

        // Convert to Base64
        byte[] pngBytes = Files.readAllBytes(pngFile.toPath());
        String base64Png = Base64.getEncoder().encodeToString(pngBytes);
        System.out.println("base64Png = " + base64Png);

        // Display in JavaFX
        javafx.scene.image.Image fxImage = new Image(new ByteArrayInputStream(pngBytes));
        signatureImageView.setImage(fxImage);

        System.out.println("Signature centered dynamically.");

        List<BufferedImage> signImage = new ArrayList<>();
        signImage.add(centeredImage);
        byte[] signPDF = documentScannerService.asPDF(signImage);
        String base64Encoded = Base64.getEncoder().encodeToString(signPDF);

        System.out.println("PDF (Base64):" + base64Encoded);


        // Save raw PNG image (with transparency)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(centeredImage, "png", baos);
        byte[] signImageBytes = baos.toByteArray();
        System.out.println("png  :" + Base64.getEncoder().encodeToString(signImageBytes));

        // Get or create document DTO
        DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get("signature");
        if (documentDto == null) {
            documentDto = new DocumentDto();
            documentDto.setType("DOCSIGN01");
            documentDto.setCategory("POS");
            documentDto.setFormat("PDF");
            documentDto.setOwner("Applicant");
            documentDto.setValue("POS_DOCSIGN01");
        }

        Random r = new Random();
        documentDto.setRefNumber("SIGN" + r.nextLong());
        documentDto.setDocument(signImageBytes);
        getRegistrationDTOFromSession().addDocument("signature", documentDto);
        signatureImageView.setImage(fxImage);

//        getRegistrationDTOFromSession().setSignatureLines(drawnLines);
        Platform.runLater(() -> {
            biometricsController.refreshContinueButton();
        });
    }


    @FXML
    public void cancelSignature(ActionEvent actionEvent) {
        stage = (Stage) ((Node) actionEvent.getSource()).getParent().getScene().getWindow();
        stage.close();
    }


    @FXML
    public void handleExceptionCheckBox(ActionEvent event) {
        signatureException = exceptionCheckBox.isSelected();

        System.out.println("signException : " + signatureException);
        SessionContext.map().put("signException", signatureException);
        RegistrationDTO registrationDTO = new RegistrationDTO();
        registrationDTO.setSignatureException(true);
//        thumbsHbox.setVisible(true);
//        thumbsComboBox.setVisible(true);
//        captureFinger.setVisible(true);


        Platform.runLater(() -> {
            biometricsController.refreshContinueButton();
        });
//        generateAlert(RegistrationConstants.ALERT_INFORMATION, "Exception Signature Selected#TYPE#SUCCESS");
//        stage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
//        stage.close();
    }


}
