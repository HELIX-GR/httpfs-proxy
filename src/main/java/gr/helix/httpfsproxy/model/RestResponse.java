package gr.helix.httpfsproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RestResponse <Result> 
{
    public enum EnumResponseStatus
    {
        SUCCESS, FAILURE;
    };
    
    private EnumResponseStatus status;
    
    private Result result;
    
    private String errorMessage;
    
    @JsonProperty("status")
    public EnumResponseStatus getStatus()
    {
        return this.status;
    }
  
    @JsonProperty("status")
    public void setStatus(EnumResponseStatus status)
    {
        this.status = status;
    }
    
    @JsonProperty("error")
    public String getErrorMessage()
    {
        return errorMessage;
    }
    
    @JsonProperty("error")
    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }
    
    @JsonProperty("result")
    public Result getResult()
    {
        return result;
    }
    
    @JsonProperty("result")
    public void setResult(Result result)
    {
        this.result = result;
    }
    
    public RestResponse()
    {}
    
    public static <R> RestResponse<R> result(R result)
    {
        RestResponse<R> y = new RestResponse<R>();
        y.status = EnumResponseStatus.SUCCESS;
        y.result = result;
        y.errorMessage = null;
        return y;
    }
    
    public static RestResponse<Void> error(String errorMessage)
    {
        RestResponse<Void> y = new RestResponse<Void>();
        y.status = EnumResponseStatus.FAILURE;
        y.result = null;
        y.errorMessage = errorMessage;
        return y;
    }
}