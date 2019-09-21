package gr.helix.httpfsproxy.model.ops;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Lists;

import gr.helix.httpfsproxy.validation.FilePath;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class ConcatenateFilesRequestParameters extends BaseRequestParameters
{
    @JsonIgnore
    @NotEmpty
    List<@NotEmpty @FilePath String> sources;
    
    @JsonProperty("sources")
    @JsonGetter
    public String getSourcesAsString()
    {
        return String.join(",", sources);
    }
    
    @JsonProperty("sources")
    @JsonSetter
    public void setSourcesFromString(String s)
    {
        this.sources = Arrays.asList(s.split(","));
    }
    
    @JsonIgnore
    @Size(min = 1, max = 1, message = "source files must be in same directory")
    public Collection<String> getSourceDirectoryPaths()
    {
        if (sources == null)
            return null;
        return new HashSet<>(Lists.transform(sources, FilenameUtils::getFullPathNoEndSeparator));
    }
}
