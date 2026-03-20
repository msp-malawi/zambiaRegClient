package io.mosip.registration.device.fp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FingerGetImageRequestDto {
    @JsonProperty("ImgFormat")
    private String ImgFormat;

    @JsonProperty("CompressionRatio")
    private String CompressionRatio;
}
