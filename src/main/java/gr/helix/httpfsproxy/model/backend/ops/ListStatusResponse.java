package gr.helix.httpfsproxy.model.backend.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class ListStatusResponse
{
    @JsonProperty("FileStatuses")
    FileStatusList statuses;
}
