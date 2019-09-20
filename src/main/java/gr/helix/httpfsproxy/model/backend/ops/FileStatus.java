package gr.helix.httpfsproxy.model.backend.ops;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see https://hadoop.apache.org/docs/r2.9.2/hadoop-project-dist/hadoop-hdfs/WebHDFS.html#FileStatus_Properties
 */
@lombok.Data
public class FileStatus
{
    enum EnumType {
        FILE, 
        DIRECTORY, 
        SYMLINK
    }; 
    
    @NotNull
    @JsonProperty("type")
    EnumType type;
    
    @NotEmpty
    @JsonProperty("pathSuffix")
    String path;
    
    /**
     * The octal permission string
     */
    @NotEmpty
    @JsonProperty("permission")
    String permission;
    
    /**
     * The file size (in bytes)
     */
    @NotNull
    @Min(0)
    @JsonProperty("length")
    Long length;
    
    /**
     * The block size of a file (in bytes)
     */
    @NotNull
    @Min(0)
    @JsonProperty("blockSize")
    Integer blockSize;
    
    /**
     * The access time (milliseconds since Epoch)
     */
    @NotNull
    @Min(0)
    @JsonProperty("accessTime")
    Long accessTime;
    
    /**
     * The modification time (milliseconds since Epoch)
     */
    @JsonProperty("modificationTime")
    Long modificationTime;
    
    /**
     * The owning user
     */
    @NotEmpty
    @JsonProperty("owner")
    String ownerName;
    
    /**
     * The owning group
     */
    @NotEmpty
    @JsonProperty("group")
    String groupName;
    
    /**
     * The replication factor
     */
    @NotNull
    @Min(0)
    @JsonProperty("replication")
    Short replication;
    
    /**
     * The link target of a symlink (if current file is a symlink)
     */
    @JsonProperty("symlink")
    String linkTarget;
}
