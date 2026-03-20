package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MantraCaptureResponse {
    @JsonProperty("CaptureData")
    private CaptureData CaptureData;
    @JsonProperty("ErrorCode")
    private String ErrorCode;
    @JsonProperty("ErrorDescription")
    private String ErrorDescription;

    @Data
    public static class CaptureData {
        @JsonProperty("CapturedFingers")
        private List<CapturedFinger> CapturedFingers;
        @JsonProperty("FingerCount")
        private int FingerCount;
        @JsonProperty("SlapBoxedBmpImage")
        private String SlapBoxedBmpImage;
    }

    @Data
    public static class CapturedFinger {
        @JsonProperty("FingerBitmapStr")
        private String FingerBitmapStr;
        @JsonProperty("NFIQ")
        private String NFIQ;
        @JsonProperty("Quality")
        private String Quality;
    }
}
