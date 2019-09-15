package gr.helix.httpfsproxy.model.backend.ops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class BooleanResponse
{
    @JsonProperty("boolean")
    Boolean flag;

}
