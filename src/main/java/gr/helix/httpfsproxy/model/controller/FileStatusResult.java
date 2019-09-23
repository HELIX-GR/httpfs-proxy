package gr.helix.httpfsproxy.model.controller;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.ops.FileStatus;

@lombok.Data
@lombok.AllArgsConstructor(staticName = "of")
public class FileStatusResult
{
    @JsonProperty("status")
    @NotNull
    @lombok.NonNull
    final FileStatus status;
}
