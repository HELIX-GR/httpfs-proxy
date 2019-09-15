package gr.helix.httpfsproxy.model.backend.ops;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter 
@lombok.Setter
@lombok.ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class GetHomeDirectoryResponse
{
    @JsonProperty("Path")
    private String path;
}
