package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FingerGetImageResponseDto {

    @JsonProperty("ErrorCode")
    private String ErrorCode;

    @JsonProperty("ErrorDescription")
    private String ErrorDescription;

    @JsonProperty("Fingers")
    private Fingers Fingers;

    @Data
    public static class Fingers {
        @JsonProperty("FingerCount")
        private int FingerCount;

        @JsonProperty("fingerbase64")
        private List<String> fingerbase64;
    }
}
