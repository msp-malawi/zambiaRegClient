package io.mosip.registration.dto;

import lombok.Data;

import java.util.List;

/**
 * The Class RegCenterMachineUserReqDto.
 * 
 * @author Brahmananda reddy
 *
 *
 * @param <T> the generic type
 */
@Data
public class RegCenterMachineUserReqDto<T> {
	private String id;
	private String version;
	private String requesttime;
	private List<T> request;

}
