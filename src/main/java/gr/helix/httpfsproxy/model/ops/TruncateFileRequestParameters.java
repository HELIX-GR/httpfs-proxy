package gr.helix.httpfsproxy.model.ops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TruncateFileRequestParameters extends BaseRequestParameters
{
    @JsonProperty("newlength")
    Integer length;
}
