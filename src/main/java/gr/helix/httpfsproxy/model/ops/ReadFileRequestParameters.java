package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadFileRequestParameters extends BaseRequestParameters
{
    @JsonProperty("length")
    @Min(1028)
    private Long length;
 
    @JsonProperty("offset")
    @Min(0)
    private Long offset;
    
    @JsonProperty("buffersize")
    @Min(4096)
    private Integer bufferSize;
}
