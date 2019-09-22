package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.AssertTrue;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Value
@lombok.EqualsAndHashCode(callSuper = false)
public class SetOwnerRequestParameters extends BaseRequestParameters
{
    @JsonProperty("owner")
    String ownerName;
    
    @JsonProperty("group")
    String groupName;
    
    @JsonCreator
    public static SetOwnerRequestParameters of(
        @JsonProperty("owner") String userName, @JsonProperty("group") String groupName)
    {
        return new SetOwnerRequestParameters(userName, groupName);
    }
    
    @JsonIgnore
    @AssertTrue
    public boolean hasOwnerOrGroup()
    {
        return !StringUtils.isEmpty(ownerName) || !StringUtils.isEmpty(groupName);
    }
}
