package ro.cst.tsearch.utils;

import java.io.*;

import org.apache.commons.io.IOUtils;

public class StrUtil
{
	
	private ByteArrayOutputStream _copy = new ByteArrayOutputStream();
	private int size;
	
    public StrUtil( InputStream in) throws IOException	
    {
	 size = 0;
	byte[] buf = new byte[ 8 * 1024 ];

	for ( ; ; )
	    {
		int numRead = in.read( buf );

		if ( numRead == -1 )
		    break;

		_copy.write( buf, 0, numRead );
		size += numRead;
	    }

	//out.flush();
	
    }

    /**
     * Get Stream bytes 
     * @param in
     * @return
     * @throws IOException
     */
    public byte[] getStreamBytes() throws IOException {	
    	    
	 return _copy.toByteArray();
    }
    
    
    /**
     * Get a copy of the input Stream
     * @param in
     * @return
     * @throws IOException
     */
    public  ByteArrayInputStream getStreamCopy() throws IOException {        
     return new ByteArrayInputStream(_copy.toByteArray());
    }
    /**
     * Converts a stream into a String representation
     * @param Stream in
     * @return String content
     */
    public static String getStreamAsString(InputStream in){
    	
    	try {
			return IOUtils.toString(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return "";
   }
    

}



