package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class GetFileStatusResponse
{
    @JsonProperty("FileStatus")
    FileStatus fileStatus;
}
