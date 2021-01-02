package io.mosip.registration.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * The DTO Class PacketStatusReaderDTO.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
public class PacketStatusReaderDTO {

	private String id;
	private String version;
	private String requesttime;
	private List<RegistrationIdDTO> request;
	
	
}
