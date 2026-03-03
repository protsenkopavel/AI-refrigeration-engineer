package net.protsenko.refrigeration.engineer.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.protsenko.refrigeration.engineer.domain.model.ChamberData;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtractionResponse {
    @JsonProperty("project_name")
    private String projectName;

    private List<ChamberData> chambers;

    @JsonProperty("missing_parameters")
    private List<String> missingParameters;

    private List<String> warnings;
}
