package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Value
@lombok.EqualsAndHashCode(callSuper = false)
public class SetPermissionRequestParameters extends BaseRequestParameters
{
    @JsonProperty("permission")
    @NotNull
    @Pattern(regexp = "^[0-7][0-7][0-7]$")
    String permission;
    
    @JsonCreator
    public static SetPermissionRequestParameters of(@JsonProperty("permission") String permission)
    {
        return new SetPermissionRequestParameters(permission);
    }
}
