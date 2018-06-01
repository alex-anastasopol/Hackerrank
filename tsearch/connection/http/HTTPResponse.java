package ro.cst.tsearch.connection.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang.StringUtils;

public class HTTPResponse extends HttpStatus
{
	public HTTPResponse() {};
	
	///////////////////
	public int returnCode;
	public InputStream is;
	public String contentType = "unknown";
	public long contentLenght = -1;
	public HashMap<String,String> headers = new HashMap<String,String>();
	public String body = null;
	/**
	 * Contains last URI used to get this response
	 */
	private URI lastURI = null;
	
	public int getReturnCode()
	{
		return returnCode;
	}
	
	public InputStream getResponseAsStream()
	{
		return is;
	}

	public String getResponseAsString()
	{
		try
		{
			if ( body != null )
				return body;
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			byte[] buff = new byte[1024];
			
			int length;
			while ( ((is!=null) && (length = is.read(buff)) != -1) )
            {
				baos.write(buff, 0, length);
            }
			
			body = new String(baos.toByteArray());
			
			return body;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			return null;
		}
	}
	
	public byte[] getResponseAsByte()
	{
		try
		{
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			byte[] buff = new byte[1024];
			
			int length;
			while ( (length = is.read(buff)) != -1 )
            {
				baos.write(buff, 0, length);
            }

			
			return baos.toByteArray();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			return null;
		}
	}
	
	public String getGZipResponseAsString() throws IOException{
		String contentEncoding = this.getHeader("Content-Encoding");
	    
	    if (StringUtils.isNotBlank(contentEncoding)){
	    	if (contentEncoding.indexOf("gzip") != -1){
	    		StringWriter responseBody = new StringWriter();
	    		PrintWriter responseWriter = new PrintWriter(responseBody);
	    		GZIPInputStream zippedInputStream =  new GZIPInputStream(getResponseAsStream());
	    		BufferedReader r = new BufferedReader(new InputStreamReader(zippedInputStream));
	    		String line = null;
	    		while ((line = r.readLine()) != null){
	    			responseWriter.println(line);
	    		}
	    		return responseBody.toString();
	    	}
	    }
	    return getResponseAsString();
	}

	public HashMap<String, String> getHeaders()
	{
		return headers;
	}
	
	public void setHeader(String name, String value)
	{
		headers.put(name, value);		
	}
	
	public String getHeader(String name)
	{
		return (String) headers.get(name);
	}
	
	
	public String getContentType()
	{
		return contentType;
	}
	
	public long getContentLenght()
	{
		return contentLenght;
	}

	/**
	 * Gets the last URI followed to get this page (even after redirect)
	 * @return the last URI followed
	 */
	public URI getLastURI() {
		return lastURI;
	}

	public void setLastURI(URI uri) {
		this.lastURI = uri;
	}
}
