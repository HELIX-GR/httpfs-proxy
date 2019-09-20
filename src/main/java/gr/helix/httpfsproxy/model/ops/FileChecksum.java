package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Value
public class FileChecksum
{
    /**
     * The name of the checksum algorithm
     */
    @JsonProperty("algorithm")
    @NotEmpty
    String algorithmName;
    
    @JsonIgnore
    @NotEmpty
    byte[] checksum;
    
    /**
     * The length of the bytes (not the length of the string)
     */
    @JsonProperty("length")
    @NotNull
    @Min(1)
    Integer length;
    
    @JsonProperty("bytes")
    @JsonGetter
    public String getChecksumAsHexString()
    {
        return Hex.encodeHexString(checksum);
    }
    
    @JsonCreator
    public FileChecksum(
        @JsonProperty("algorithm") String algorithmName, 
        @JsonProperty("bytes") String checksumAsHexString, 
        @JsonProperty("length") Integer length) 
            throws DecoderException
    {
        this.algorithmName = algorithmName;
        this.checksum = Hex.decodeHex(checksumAsHexString.substring(0, 2 * length));
        this.length = length;
    }
    
    public FileChecksum(String algorithmName, byte[] checksum)
    {
        this.algorithmName = algorithmName;
        this.checksum = checksum;
        this.length = checksum.length;
    }
}
