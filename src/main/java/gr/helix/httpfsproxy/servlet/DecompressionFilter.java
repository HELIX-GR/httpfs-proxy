package gr.helix.httpfsproxy.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

/**
 * A filter that examines <tt>Content-Encoding</tt> request header and decompresses 
 * input stream.
 */
public class DecompressionFilter implements Filter
{
    static class DecompressionRequestWrapper extends HttpServletRequestWrapper
    {
        public DecompressionRequestWrapper(HttpServletRequest request)
        {
            super(request);
        }
        
        @Override
        public ServletInputStream getInputStream() throws IOException
        {
            final GZIPInputStream zin = new GZIPInputStream(super.getInputStream());
            
            // Forward read/close methods to gzip-ed input stream
            
            return new ServletInputStream()
            {
                @Override
                public boolean isReady()
                {
                    return true;
                }
                
                @Override
                public int read() throws IOException
                {
                    return zin.read();
                }
                
                @Override
                public int read(byte[] b) throws IOException
                {
                    return zin.read(b);
                }
                
                @Override
                public int read(byte[] b, int off, int len) throws IOException
                {
                    return zin.read(b, off, len);
                }
                
                @Override
                public void close() throws IOException
                {
                    zin.close();
                }
                
                @Override
                public synchronized void reset() throws IOException
                {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public void setReadListener(ReadListener listener)
                {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public boolean isFinished()
                {
                    return false;
                }
                
                @Override
                public boolean markSupported()
                {
                    return false;
                }
            };
        }
    }
    
    protected boolean shouldFilter(HttpServletRequest request)
    {
        String httpMethod = request.getMethod();
        if (!httpMethod.equalsIgnoreCase("PUT") && !httpMethod.contentEquals("POST"))
            return false;
        return "gzip".equalsIgnoreCase(request.getHeader(HttpHeaders.CONTENT_ENCODING));
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // no-op
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        final HttpServletRequest request1 = (HttpServletRequest) request;
        final HttpServletResponse response1 = (HttpServletResponse) response;
        if (shouldFilter(request1)) {
            chain.doFilter(new DecompressionRequestWrapper(request1), response1);
        } else {
            chain.doFilter(request1, response1);
        }
    }
}
