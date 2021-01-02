package io.mosip.registration.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncDataResponseDto {
	private String lastSyncTime;
	private List<SyncDataBaseDto> dataToSync;
}
