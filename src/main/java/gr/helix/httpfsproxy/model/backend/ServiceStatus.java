package gr.helix.httpfsproxy.model.backend;

public enum ServiceStatus
{
    OK("OK", true),
    
    /**
     * The service is unreachable. 
     * An attempt to communicate to the service results to some kind of I/O exception.
     */
    UNREACHABLE("UNREACHABLE", false),
    
    /**
     * The service responds in a unsuccessful manner (e.g with a non-2xx HTTP status or with
     * an unexpected content-type).
     */
    FAILED("FAILED", false),
    
    UNKNOWN("?", false);
    
    private final String displayText;
    
    private final boolean successful;
    
    private ServiceStatus(String displayText, boolean successful)
    {
        this.displayText = displayText;
        this.successful = successful;
    }
    
    public String getDisplayText()
    {
        return displayText;
    }
    
    public boolean isSuccessful()
    {
        return successful;
    }
}
