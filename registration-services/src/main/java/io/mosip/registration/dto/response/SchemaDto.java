package io.mosip.registration.dto.response;


import io.mosip.registration.dto.UiSchemaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemaDto {

	private String id;
	private double idVersion;
	private List<UiSchemaDTO> schema;
	private String schemaJson;
	private LocalDateTime effectiveFrom;
}
