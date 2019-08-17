package gr.helix.httpfsproxy.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo
{
    private Long id;
    
    private String name;
    
    private String fullname;
    
    private ZonedDateTime registeredAt;
    
    private String email;
    
    public UserInfo() {}
    
    public UserInfo(Long id, String name, String fullname, String email)
    {
        this.id = id;
        this.name = name;
        this.fullname = fullname;
        this.email = email;
    }
    
    @JsonProperty("id")
    public Long getId()
    {
        return id;
    }
    
    @JsonProperty("id")
    public void setId(Long id)
    {
        this.id = id;
    }
    
    @JsonProperty("name")
    public String getUserName()
    {
        return name;
    }
    
    @JsonProperty("name")
    public void setUserName(String name)
    {
        this.name = name;
    }
    
    @JsonProperty("fullname")
    public String getFullname()
    {
        return fullname;
    }
    
    @JsonProperty("fullname")
    public void setFullname(String fullname)
    {
        this.fullname = fullname;
    }

    @JsonProperty("registeredAt")
    public ZonedDateTime getRegisteredAt()
    {
        return registeredAt;
    }
    
    @JsonProperty("registeredAt")
    public void setRegisteredAt(ZonedDateTime registeredAt)
    {
        this.registeredAt = registeredAt;
    }
    
    @JsonProperty("email")
    public String getEmail()
    {
        return email;
    }
    
    @JsonProperty("email")
    public void setEmail(String email)
    {
        this.email = email;
    }
    
    @Override
    public String toString()
    {
        return String.format("UserInfo [id=%s, name=%s, fullname=%s]", id, name, fullname);
    }
}