package gr.helix.httpfsproxy.model.backend.ops;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class ListStatusResponse
{
    @JsonProperty("FileStatuses")
    @lombok.Setter(lombok.AccessLevel.PACKAGE)
    @lombok.Getter(lombok.AccessLevel.PACKAGE)
    FileStatusList fileStatuses;
    
    @JsonIgnore
    public List<FileStatus> getStatusList()
    {
        return fileStatuses == null? Collections.emptyList() : fileStatuses.statusList;
    }
}
