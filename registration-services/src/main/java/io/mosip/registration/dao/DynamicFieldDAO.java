package io.mosip.registration.dao;

import io.mosip.registration.dto.mastersync.DynamicFieldValueDto;
import io.mosip.registration.entity.DynamicField;

import java.util.List;

public interface DynamicFieldDAO {

	DynamicField getDynamicField(String fieldName, String Langcode);
	
	List<DynamicFieldValueDto> getDynamicFieldValues(String fieldName, String Langcode);
}
