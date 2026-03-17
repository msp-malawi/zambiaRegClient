package io.mosip.registration.dto.iso;

import lombok.Data;

@Data
public class ImageDetail {

    private int width;
    private int height;
    private byte[] data;
    private String format;
    private String imageType;
    private String subtype;

}
