package gr.helix.httpfsproxy.model.controller;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.ops.FileStatus;

@lombok.Data
@lombok.AllArgsConstructor(staticName = "of")
public class ListStatusResult
{
    @JsonProperty("statuses")
    @NotNull
    @lombok.NonNull
    List<FileStatus> statuses;
}
