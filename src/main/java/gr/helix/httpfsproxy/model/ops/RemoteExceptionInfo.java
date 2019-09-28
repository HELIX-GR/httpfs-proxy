package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the exception response returned from a failed HttpFS/WebHDFS operation
 * 
 * @see https://hadoop.apache.org/docs/r2.9.2/hadoop-project-dist/hadoop-hdfs/WebHDFS.html#RemoteException_JSON_Schema 
 */
@lombok.Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteExceptionInfo
{
    @JsonProperty("exception")
    @NotEmpty
    @lombok.NonNull
    final String exceptionName;
    
    @JsonProperty("message")
    @NotEmpty
    @lombok.NonNull
    final String message;
    
    @JsonCreator
    public static RemoteExceptionInfo of(
        @JsonProperty("exception") String exceptionName, 
        @JsonProperty("message") String message)
    {
        return new RemoteExceptionInfo(exceptionName, message);
    }
}
