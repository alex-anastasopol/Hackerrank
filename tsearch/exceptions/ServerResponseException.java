/*
 * Created on May 27, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.exceptions;

import org.apache.log4j.Category;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ServerResponseException extends BaseException {
	
	protected static final Category logger= Category.getInstance(ServerResponseException.class.getName());

	private ServerResponse sr;
	
	/*
	 * Text here
	 */
	public ServerResponseException(ServerResponse sr) {
		super();
		this.sr = sr;
		logError(sr);
	}

	public ServerResponseException(ParsedResponse pr) {
		super();
		this.sr = new ServerResponse();
		this.sr.setParsedResponse(pr);
		logError(sr);
	}

	private void logError(ServerResponse sr) {
		if(sr.isError()){
			logger.error (sr.getError());
			logger.error ("ServerResponse =[" 
				+ sr.getResult() + "]");
		}
		if (sr.getParsedResponse().isError()){
			logger.error (sr.getParsedResponse().getError());
		}
		if (sr.getParsedResponse().isWarning()){
			logger.warn (sr.getParsedResponse().getWarning());
		}
	}


	/**
	 * @return
	 */
	public ServerResponse getServerResponse() {
		return sr;
	}

    public static String getExceptionStackTrace( Exception e )
    {
        String retStackTrace = "";
        
        StackTraceElement[] exceptionStackTrace = e.getStackTrace();
        
        for( int i = 0 ; i < exceptionStackTrace.length ; i++ )
        {
            retStackTrace += exceptionStackTrace[i].toString() + "\n<BR>\n";
        }
        
        return retStackTrace;
    }
    
    public static String getExceptionStackTrace( Throwable e , String lineSeparator)
    {
        String retStackTrace = "";
        
        StackTraceElement[] exceptionStackTrace = e.getStackTrace();
        
        for( int i = 0 ; i < exceptionStackTrace.length ; i++ )
        {
            retStackTrace += exceptionStackTrace[i].toString() + lineSeparator;
        }
        
        return retStackTrace;
    }

}
