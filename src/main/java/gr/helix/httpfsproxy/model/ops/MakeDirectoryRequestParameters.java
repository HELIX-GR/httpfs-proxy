package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MakeDirectoryRequestParameters extends BaseRequestParameters
{
    @JsonProperty("permission")
    @Pattern(regexp = "^[0-7][0-7][0-7]$")
    String permission;
}
