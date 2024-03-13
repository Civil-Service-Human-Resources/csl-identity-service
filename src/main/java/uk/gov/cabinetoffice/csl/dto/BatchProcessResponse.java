package uk.gov.cabinetoffice.csl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchProcessResponse {
    private List<String> successfulIds = emptyList();
    private List<String> failedIds = emptyList();
}
