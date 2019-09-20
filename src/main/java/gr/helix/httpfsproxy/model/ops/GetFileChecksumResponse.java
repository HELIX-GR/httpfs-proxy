package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class GetFileChecksumResponse
{
    @JsonProperty("FileChecksum")
    FileChecksum checksum;
}
