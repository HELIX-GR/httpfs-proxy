package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parameters for deleting a file/directory
 */
@lombok.Value
@lombok.EqualsAndHashCode(callSuper = false)
public class DeleteFileRequestParameters extends BaseRequestParameters
{
    @JsonProperty("recursive")
    final boolean recursive;
    
    private static final DeleteFileRequestParameters PARAMETERS_FOR_NON_RECURSIVE_DELETE = 
        new DeleteFileRequestParameters(false);
    
    private static final DeleteFileRequestParameters PARAMETERS_FOR_RECURSIVE_DELETE = 
        new DeleteFileRequestParameters(true);
    
    @JsonCreator
    public static DeleteFileRequestParameters of(@JsonProperty("recursive") boolean recursive)
    {
        return recursive? PARAMETERS_FOR_RECURSIVE_DELETE : PARAMETERS_FOR_NON_RECURSIVE_DELETE;
    }
}
