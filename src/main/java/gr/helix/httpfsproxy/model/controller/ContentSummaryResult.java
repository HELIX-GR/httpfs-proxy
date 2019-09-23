package gr.helix.httpfsproxy.model.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.ops.ContentSummary;

@lombok.Data
@lombok.AllArgsConstructor(staticName = "of")
public class ContentSummaryResult
{
    @JsonProperty("summary")
    ContentSummary summary;
}
