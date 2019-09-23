package gr.helix.httpfsproxy.model.controller;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.ops.FileChecksum;

@lombok.Data
@lombok.AllArgsConstructor(staticName = "of")
public class FileChecksumResult
{
    @JsonProperty("checksum")
    @NotNull
    @lombok.NonNull
    FileChecksum checksum;
}
