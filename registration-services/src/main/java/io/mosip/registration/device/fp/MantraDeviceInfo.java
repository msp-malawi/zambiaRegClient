package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MantraDeviceInfo {
    @JsonProperty("FingerType")
    private String  FingerType;
    @JsonProperty("ClientKey")
    private  String ClientKey;

}
