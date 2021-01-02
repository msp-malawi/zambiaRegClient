package io.mosip.registration.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * The DTO Class Pre Registration Exception JSON Info.
 *
 * @author M1046129 - Jagadishwari
 */
@Getter
@Setter
public class PreRegistrationExceptionJSONInfoDTO implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 3999014525078508265L;

	private String errorCode;
	private String message;

}
