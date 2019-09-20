package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppendToFileRequestParameters extends BaseRequestParameters
{
    @JsonProperty("data")
    private final boolean hasData = true;
    
    @JsonProperty("buffersize")
    @Min(4096)
    private Integer bufferSize;
}
