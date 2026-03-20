package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MantraDeviceInfoResponse {
    @JsonProperty("DeviceInfo")
    private DeviceInfo DeviceInfo;
    @JsonProperty("ErrorCode")
    private String ErrorCode;
    @JsonProperty("ErrorDescription")
    private String ErrorDescription;

    @Data
    public static class DeviceInfo {
        @JsonProperty("Firmware")
        private String Firmware;
        @JsonProperty("Height")
        private int Height;
        @JsonProperty("Make")
        private String Make;
        @JsonProperty("Model")
        private String Model;
        @JsonProperty("SerialNo")
        private String SerialNo;
        @JsonProperty("Width")
        private int Width;
    }
}
