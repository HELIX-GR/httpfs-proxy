package gr.helix.httpfsproxy.model.backend.ops;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class RenameRequestParameters extends BaseRequestParameters
{
    // Todo validate destination as a path?
    @NotEmpty
    @JsonProperty("destination")
    String destinationPath;
}
