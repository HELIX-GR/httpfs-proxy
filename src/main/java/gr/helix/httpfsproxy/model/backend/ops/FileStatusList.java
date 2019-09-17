package gr.helix.httpfsproxy.model.backend.ops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
class FileStatusList
{
    @JsonProperty("FileStatus")
    List<FileStatus> statusList;
}
