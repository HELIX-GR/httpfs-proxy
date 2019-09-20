package gr.helix.httpfsproxy.model;

import java.net.URI;
import java.time.Instant;

@lombok.Getter
@lombok.ToString
@lombok.AllArgsConstructor(staticName = "of")
public class ServiceStatusReport
{
    @lombok.NonNull
    private final URI baseUri;
    
    @lombok.NonNull
    private final ServiceStatus status;
    
    private final Instant timestamp;
    
    private final String errorMessage;

    public static ServiceStatusReport of(URI baseUri, ServiceStatus status, Instant now)
    {
        return of(baseUri, status, now, null);
    }
}
