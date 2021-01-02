package io.mosip.registration.dto;

import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuthenticationValidatorDTO {
	private String userId;
	private String password;
	private String otp;
	private List<FingerprintDetailsDTO> fingerPrintDetails;
	private String authValidationType;
	private List<IrisDetailsDTO> irisDetails;
	private FaceDetailsDTO faceDetail;
	private boolean authValidationFlag;
	
}
