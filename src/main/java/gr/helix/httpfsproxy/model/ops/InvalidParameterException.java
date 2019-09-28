package gr.helix.httpfsproxy.model.ops;

import org.apache.http.StatusLine;

public class InvalidParameterException extends OperationFailedException
{
    private static final long serialVersionUID = 1L;

    public InvalidParameterException(String message, StatusLine statusLine)
    {
        super(message, statusLine);
    }
}
