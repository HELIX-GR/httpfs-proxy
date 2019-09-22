package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class ContentSummary
{
    /**
     * The number of directories
     */
    @JsonProperty("directoryCount")
    Integer directoryCount;
    
    /**
     * The number of files
     */
    @JsonProperty("fileCount")
    Integer fileCount;
    
    /**
     * The number of bytes used by the content
     */
    @JsonProperty("length")
    Long length;
    
    /**
     * The namespace quota of this directory
     */
    @JsonProperty("quota")
    Long quota;
    
    /**
     * The disk space consumed by the content.
     * <p>Note that: <tt>spaceConsumed = length * replicationFactor</tt>
     */
    @JsonProperty("spaceConsumed")
    Long spaceConsumed;
    
    /**
     * The disk space quota
     */
    @JsonProperty("spaceQuota")
    Long spaceQuota;

}
