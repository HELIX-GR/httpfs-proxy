package gr.helix.httpfsproxy.model.ops;

import org.apache.http.StatusLine;

public class FileNotExistsException extends OperationFailedException
{
    private static final long serialVersionUID = 1L;
    
    FileNotExistsException(String message, StatusLine statusLine)
    {
        super(message, statusLine);
    }
}
