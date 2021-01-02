package io.mosip.registration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * The DTO Class PreRegistrationIds.
 *
 * @author M1046129
 */
@Getter
@Setter
@NoArgsConstructor
public class PreRegistrationIdsDTO implements Serializable {

	private static final long serialVersionUID = 6402670047109104959L;
	private String transactionId;
	private String countOfPreRegIds;
	private Map<String, String> preRegistrationIds;
}
