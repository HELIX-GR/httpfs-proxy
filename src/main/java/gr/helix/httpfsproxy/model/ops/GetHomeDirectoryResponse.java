package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class GetHomeDirectoryResponse
{
    @JsonProperty("Path")
    String path;
}
