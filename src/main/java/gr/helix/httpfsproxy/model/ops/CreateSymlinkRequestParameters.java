package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.validation.FilePath;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CreateSymlinkRequestParameters extends BaseRequestParameters
{
    @NotEmpty
    @FilePath
    @JsonProperty("destination")
    String destinationPath;
    
    @JsonProperty("createParent")
    boolean createParent = false;
}
