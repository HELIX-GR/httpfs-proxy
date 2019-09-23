package gr.helix.httpfsproxy.model.controller;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@lombok.AllArgsConstructor(staticName = "of")
@lombok.NoArgsConstructor
public class HomeDirectoryResult
{
    @JsonProperty("path")
    @NotEmpty
    @lombok.NonNull
    String path;
}
