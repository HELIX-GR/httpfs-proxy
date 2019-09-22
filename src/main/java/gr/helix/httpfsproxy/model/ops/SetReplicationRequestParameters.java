package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Set the replication factor for a file/directory
 */
@lombok.Value
@lombok.EqualsAndHashCode(callSuper = false)
public class SetReplicationRequestParameters extends BaseRequestParameters
{
    @JsonProperty("replication")
    @NotNull
    @Min(1)
    Integer replication;
    
    @JsonCreator
    public static SetReplicationRequestParameters of(@JsonProperty("replication") Integer replication)
    {
        return new SetReplicationRequestParameters(replication);
    }
}
