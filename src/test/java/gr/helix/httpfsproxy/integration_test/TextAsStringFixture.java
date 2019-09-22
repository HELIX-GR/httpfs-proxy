package gr.helix.httpfsproxy.integration_test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

@lombok.Value(staticConstructor = "of")
public class TextAsStringFixture
{
    public final static Charset CHARSET = Charset.forName("UTF-8");
    
    final String text;
    
    final String checksum;
    
    public static TextAsStringFixture from(Resource textResource, Resource checksumResource)
        throws IOException
    {
        String data = null;
        try (InputStream in = textResource.getInputStream()) {
            data = IOUtils.toString(in, CHARSET);
        }
        
        String checksum = null;
        try (InputStream in = checksumResource.getInputStream()) {
            checksum = IOUtils.toString(in, CHARSET);
        }
        
        return new TextAsStringFixture(data, checksum);
    }
}
