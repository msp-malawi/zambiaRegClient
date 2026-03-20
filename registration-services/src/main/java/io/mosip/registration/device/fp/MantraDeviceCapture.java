package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;
@Data

public class MantraDeviceCapture {
    @JsonProperty("TimeOut")
    private String TimeOut;
    @JsonProperty("Slap")
    private String Slap;
    @JsonProperty("FingerPosition")
    private Map<String, Boolean> FingerPosition;
    @JsonProperty("NFIQ_Quality")
    private String NFIQ_Quality;
}
