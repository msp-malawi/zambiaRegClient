package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RCaptureResponseDTO {

	List<RCaptureResponseBiometricsDTO> biometrics;
}
