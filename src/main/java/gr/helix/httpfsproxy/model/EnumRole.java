package gr.helix.httpfsproxy.model;

public enum EnumRole
{
    USER("User"),
    DEVELOPER("Developer"),
    ADMIN("Administrator");
    
    private String friendlyName;
    
    private EnumRole(String friendlyName)
    {
        this.friendlyName = friendlyName;
    }
    
    public String getFriendlyName()
    {
        return friendlyName;
    }
}
