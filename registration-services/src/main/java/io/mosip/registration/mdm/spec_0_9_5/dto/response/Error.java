package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Error {

	// According to spec errorcode,errorinfo
	private String errorCode;
	private String errorInfo;
}
