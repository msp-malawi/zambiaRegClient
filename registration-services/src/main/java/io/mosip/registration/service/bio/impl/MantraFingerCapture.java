package io.mosip.registration.service.bio.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.device.fp.*;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Service
public class MantraFingerCapture {
    private static final Logger LOGGER = AppConfig.getLogger(MantraFingerCapture.class);

    private static final String INFO_API = "http://127.0.0.1:8032/morfinenroll/info";
    private static final String CAPTURE_API = "http://127.0.0.1:8032/morfinenroll/capture";
    private static final String GETIMAGE_API = "http://127.0.0.1:8032/morfinenroll/getimage";
    static Map<String, Boolean> fingerPositionMap = new HashMap<>();
    static Map<String, String> uiAttibuteMap = new HashMap<>();

    static {

        uiAttibuteMap.put("leftLittle", "LEFT_LITTLE");
        uiAttibuteMap.put("leftRing", "LEFT_RING");
        uiAttibuteMap.put("leftMiddle", "LEFT_MIDDLE");
        uiAttibuteMap.put("leftIndex", "LEFT_INDEX");
        uiAttibuteMap.put("rightIndex", "RIGHT_INDEX");
        uiAttibuteMap.put("rightMiddle", "RIGHT_MIDDLE");
        uiAttibuteMap.put("rightRing", "RIGHT_RING");
        uiAttibuteMap.put("rightLittle", "RIGHT_LITTLE");
        uiAttibuteMap.put("leftThumb", "LEFT_THUMB");
        uiAttibuteMap.put("rightThumb", "RIGHT_THUMB");
    }

    @Autowired
    RestTemplate restTemplate;
    Map<String, String> fingerSlab = Map.of(RegistrationConstants.FINGERPRINT_SLAB_LEFT, "0", RegistrationConstants.FINGERPRINT_SLAB_RIGHT, "1", RegistrationConstants.FINGERPRINT_SLAB_THUMBS, "2");

    public void setFingerPos() {

        fingerPositionMap.put("LEFT_LITTLE", false);
        fingerPositionMap.put("LEFT_RING", false);
        fingerPositionMap.put("LEFT_MIDDLE", false);
        fingerPositionMap.put("LEFT_INDEX", false);
        fingerPositionMap.put("RIGHT_INDEX", false);
        fingerPositionMap.put("RIGHT_MIDDLE", false);
        fingerPositionMap.put("RIGHT_RING", false);
        fingerPositionMap.put("RIGHT_LITTLE", false);
        fingerPositionMap.put("LEFT_THUMB", false);
        fingerPositionMap.put("RIGHT_THUMB", false);
    }

    public boolean info() {
        MantraDeviceInfo infoRequest = new MantraDeviceInfo();
        infoRequest.setFingerType("0");
        infoRequest.setClientKey("");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MantraDeviceInfo> req = new HttpEntity<>(infoRequest, headers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(infoRequest);
            System.out.println("Request JSON: " + jsonPayload);
            ResponseEntity<MantraDeviceInfoResponse> response = restTemplate.exchange(INFO_API, HttpMethod.POST, req, MantraDeviceInfoResponse.class);
            System.out.println("Response received: " + response.getBody());
            return response.getBody().getDeviceInfo().getSerialNo() != null;

        } catch (HttpStatusCodeException ex) {
            System.err.println("HTTP Status: " + ex.getStatusCode());
            System.err.println("Error Response Body: " + ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            System.err.println("Failed to fetch  serialNumber: " + ex.getMessage());
            throw new RuntimeException("Failed to fetch  serialNumber", ex);
        }


    }


    public MantraCaptureResponse.CaptureData capture(String slab, Map<String, Boolean> fingerPositionMap) {
        MantraDeviceCapture mantraDeviceCapture = new MantraDeviceCapture();


        mantraDeviceCapture.setSlap(slab);
        mantraDeviceCapture.setNFIQ_Quality("50");
        mantraDeviceCapture.setTimeOut("20");
        mantraDeviceCapture.setFingerPosition(fingerPositionMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MantraDeviceCapture> req = new HttpEntity<>(mantraDeviceCapture, headers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(mantraDeviceCapture);
            System.out.println("Request JSON: capture " + jsonPayload);
            ResponseEntity<MantraCaptureResponse> response = restTemplate.exchange(CAPTURE_API, HttpMethod.POST, req, MantraCaptureResponse.class);
//            System.out.println("Response received: " + response.getBody());
            return response.getBody().getCaptureData();

        } catch (HttpStatusCodeException ex) {
            System.err.println("HTTP Status: " + ex.getStatusCode());
            System.err.println("Error Response Body: " + ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            System.err.println("Failed to fetch  serialNumber: " + ex.getMessage());
            throw new RuntimeException("Failed to fetch  serialNumber", ex);
        }
    }


    public List<BiometricsDto> rCapture(MDMRequestDto mdmRequestDto) {
        System.out.println("mdmRequestDto = " + mdmRequestDto);
        String currentModality = mdmRequestDto.getModality().toUpperCase();
        List<BiometricsDto> biometricDTOs = new ArrayList<>();
        String slab = fingerSlab.get(currentModality);
        if (info()) {
            setFingerPos();
            setFingerPositions(mdmRequestDto.getExceptions());

            MantraCaptureResponse.CaptureData res = capture(slab, MantraFingerCapture.fingerPositionMap);

            List< String> jp2Image = getImageByte();
            jp2Image.remove(0);

//            for (MantraCaptureResponse.CapturedFinger s : res.getCapturedFingers()) {
            List<MantraCaptureResponse.CapturedFinger> captureFingerList = res.getCapturedFingers();
            for(int i=0; i<captureFingerList.size(); i++){
//                String uiAttribute = io.mosip.registration.mdm.dto.Biometric.getUiSchemaAttributeName("face", "0.9.2");
                String uiAttribute = getUiAttribute(slab);
                LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
                        "  slab = " + slab +uiAttribute );


                BiometricsDto biometricDTO = new BiometricsDto(uiAttribute, Base64.getDecoder().decode(jp2Image.get(i)), Double.parseDouble(captureFingerList.get(i).getNFIQ()));
                biometricDTO.setCaptured(true);
                biometricDTO.setModalityName(mdmRequestDto.getModality());
                biometricDTOs.add(biometricDTO);
            }
            LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
                    "  currentModality_MANTRA capture " + slab +currentModality );
            System.out.println("currentModality_MANTRA = capture " + currentModality + "_MANTRA");
            SessionContext.map().put(currentModality + "_MANTRA", getSlabImage(res.getSlapBoxedBmpImage()));
        }
        MantraFingerCapture.fingerPositionMap.clear();
//        setFingerPos();
        return biometricDTOs;
    }

    public byte[] getSlabImage(String fingerSlabImage) {
        // Decode base64 to byte[]
        byte[] bmpBytes = Base64.getDecoder().decode(fingerSlabImage);
        ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
        try {
            // Convert BMP byte[] to BufferedImage
            ByteArrayInputStream bais = new ByteArrayInputStream(bmpBytes);
            BufferedImage bufferedImage = ImageIO.read(bais); // auto detects BMP

            if (bufferedImage == null) {
                throw new IllegalArgumentException("Could not decode image from Base64 BPM string.");
            }

            // Convert BufferedImage to JPEG byte[]

            ImageIO.write(bufferedImage, "jpg", jpegOutput);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return jpegOutput.toByteArray(); // ready to use with JavaFX Image

    }

    public String getUiAttribute(String slab) {
        switch (slab) {
            case "0":
                if (!fingerPositionMap.get("LEFT_LITTLE")) {
                    fingerPositionMap.put("LEFT_LITTLE", true);
                    return "leftLittle";
                }
                if (!fingerPositionMap.get("LEFT_RING")) {
                    fingerPositionMap.put("LEFT_RING", true);
                    return "leftRing";
                }
                if (!fingerPositionMap.get("LEFT_MIDDLE")) {
                    fingerPositionMap.put("LEFT_MIDDLE", true);
                    return "leftMiddle";
                }
                if (!fingerPositionMap.get("LEFT_INDEX")) {
                    fingerPositionMap.put("LEFT_INDEX", true);
                    return "leftIndex";
                }
                break;
            case "1":
                if (!fingerPositionMap.get("RIGHT_INDEX")) {
                    fingerPositionMap.put("RIGHT_INDEX", true);
                    return "rightIndex";
                }
                if (!fingerPositionMap.get("RIGHT_MIDDLE")) {
                    fingerPositionMap.put("RIGHT_MIDDLE", true);
                    return "rightMiddle";
                }
                if (!fingerPositionMap.get("RIGHT_RING")) {
                    fingerPositionMap.put("RIGHT_RING", true);
                    return "rightRing";
                }
                if (!fingerPositionMap.get("RIGHT_LITTLE")) {
                    fingerPositionMap.put("RIGHT_LITTLE", true);
                    return "rightLittle";
                }
                break;
            case "2":
                if (!fingerPositionMap.get("LEFT_THUMB")) {
                    fingerPositionMap.put("LEFT_THUMB", true);
                    return "leftThumb";
                }
                if (!fingerPositionMap.get("RIGHT_THUMB")) {
                    fingerPositionMap.put("RIGHT_THUMB", true);
                    return "rightThumb";
                }
                break;
            default:
                return null;
        }
        return null;
    }

    public void setFingerPositions(String[] exceptions) {

        for (String exception : exceptions) {
            fingerPositionMap.put(uiAttibuteMap.get(exception), true);
        }
    }




    public List<String> getImageByte() {
        FingerGetImageRequestDto infoRequest = new FingerGetImageRequestDto();
        infoRequest.setCompressionRatio("10");
        infoRequest.setImgFormat("6");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FingerGetImageRequestDto> req = new HttpEntity<>(infoRequest, headers);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(infoRequest);
            System.out.println("Request JSON: " + jsonPayload);
            ResponseEntity<FingerGetImageResponseDto> response = restTemplate.exchange(GETIMAGE_API, HttpMethod.POST, req, FingerGetImageResponseDto.class);
            System.out.println("Response received: " + response.getBody());
            return response.getBody().getFingers().getFingerbase64();

        } catch (HttpStatusCodeException ex) {
            System.err.println("HTTP Status: " + ex.getStatusCode());
            System.err.println("Error Response Body: " + ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
                    "Failed to fetch  serialNumber  " + ex );
            System.err.println("Failed to fetch  serialNumber: " + ex.getMessage());
            throw new RuntimeException("Failed to fetch  serialNumber", ex);
        }


    }


}
