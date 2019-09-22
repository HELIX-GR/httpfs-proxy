package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class ContentSummaryResponse
{
    @JsonProperty("ContentSummary")
    ContentSummary summary;
}
