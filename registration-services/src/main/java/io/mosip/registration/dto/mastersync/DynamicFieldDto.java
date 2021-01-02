package io.mosip.registration.dto.mastersync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicFieldDto {

	private String id;
	private String name;
	private String dataType;
	private List<DynamicFieldValueDto> fieldVal;
	private String langCode;	
}
