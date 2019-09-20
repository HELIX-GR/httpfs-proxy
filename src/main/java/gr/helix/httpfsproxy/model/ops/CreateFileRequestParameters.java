package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.validation.FileBlockSize;
import gr.helix.httpfsproxy.validation.FileReplication;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFileRequestParameters extends BaseRequestParameters
{
    @JsonProperty("data")
    private final boolean hasData = true;
    
    @JsonProperty("overwrite")
    private boolean overwrite = false;
    
    @JsonProperty("blocksize")
    @FileBlockSize
    @Min(4096)
    private Integer blockSize;
    
    @JsonProperty("buffersize")
    @Min(4096)
    private Integer bufferSize;
    
    @JsonProperty("replication")
    @FileReplication
    @Min(1)
    @Max(10)
    private Integer replication;
    
    @JsonProperty("permission")
    @Pattern(regexp = "^[0-7][0-7][0-7]$")
    private String permission = "644";
}
