package gr.helix.httpfsproxy.model.backend.ops;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class MakeDirectoryRequestParameters extends BaseRequestParameters
{
    @JsonProperty("permission")
    @Pattern(regexp = "^[0-7][0-7][0-7]$")
    String permission;
}
