package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.validation.FilePath;

@lombok.Value
@lombok.EqualsAndHashCode(callSuper = false)
public class RenameRequestParameters extends BaseRequestParameters
{
    @NotEmpty
    @FilePath
    @JsonProperty("destination")
    @lombok.NonNull
    String destinationPath;
    
    @JsonCreator
    public static RenameRequestParameters of(@JsonProperty("destination") String destinationPath)
    {
        return new RenameRequestParameters(destinationPath);
    }
}
