package gr.helix.httpfsproxy.integration_test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

@lombok.Value(staticConstructor = "of")
public class TextAsResourceFixture
{
    public final static Charset CHARSET = Charset.forName("UTF-8");
    
    final Resource resource;
    
    final String checksum;
    
    public String readText() throws IOException
    {
        String data = null;
        try (InputStream in = resource.getInputStream()) {
            data = IOUtils.toString(in, CHARSET);
        }
        return data;
    }
    
    public static TextAsResourceFixture from(Resource textResource, Resource checksumResource)
        throws IOException
    {
        String checksum = null;
        try (InputStream in = checksumResource.getInputStream()) {
            checksum = IOUtils.toString(in, CHARSET);
        }
        
        return new TextAsResourceFixture(textResource, checksum);
    }
}
