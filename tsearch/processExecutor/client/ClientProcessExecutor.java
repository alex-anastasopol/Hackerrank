package ro.cst.tsearch.processExecutor.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.processExecutor.Packet;
import ro.cst.tsearch.processExecutor.server.ServerExecutor;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;

public class ClientProcessExecutor
{
    protected static final Category logger = Logger.getLogger(ClientProcessExecutor.class);
	private static boolean			triedAutoStart		= false;
	private static Process			process				= null;
    
    public static final int EXEC_TIMEOUT = -1000;
    
    public static final long TIME_BETWEEN_EMAILS = 10 * 90 * 1000;
    public static final int WAIT_TIMEOUT = 1000000;	//500 seconds
    
    //command to execute
    private String[] cmd;
    
    //succes or failure
    private int executionResult = Packet.INVALID_VALUE;

    private boolean captureOutput = false;
    private boolean captureError = false;
    private int returnValue = -1;
    
    private String commandOutput = "";
    private String commandError = "";
    
    private long searchId = -1;
    
    private static long lastEmailSentMillis = -1;
    
    private String workingDirectory = "";
    
    
    public ClientProcessExecutor( String[] cmd, boolean captureOutput, boolean captureError )
    {
        this.cmd = cmd;
        
        this.captureOutput = captureOutput;
        this.captureError = captureError;
        
        
    }
    
    public void setSearchId( long searchId ){
    	this.searchId = searchId;
    }
    
    public void start() throws IOException {
    	start(true);
    }
    
    
    public void start(boolean retryStartServerExecutor) throws IOException
    {
        Socket executionRequestSocket = null;
        try
        {
            //create a socket to send the execution request
            logger.debug( "Creating execution socket" );
            executionRequestSocket = new Socket( InetAddress.getByName( ServerExecutor.BIND_IP ), ServerExecutor.BIND_PORT );
            
            if(searchId<0){
	            //WAIT_TIMEOUT/1000 second timeout;
	            executionRequestSocket.setSoTimeout( WAIT_TIMEOUT );
            }
            else{
	            executionRequestSocket.setSoTimeout( 1000000  );
            }
            ObjectOutputStream socketWriter = new ObjectOutputStream( executionRequestSocket.getOutputStream() );
            
            //create the execution request package
            logger.debug( "Create request packet" );
            Packet requestExecution = new Packet( Packet.EXEC_REQUEST, cmd, (char)0 );
            
            requestExecution.setWorkingDirectory( workingDirectory );
            
            //set the output capture flag
            if( captureOutput )
            {
                requestExecution.setMask( Packet.CAPTURE_OUTPUT_MASK );
            }
            
            //set the error capture flag
            if( captureError )
            {
                requestExecution.setMask( Packet.CAPTURE_ERROR_MASK );
            }
            
            //sends the request to the socket
            logger.debug( "write packet to the socket" );
            socketWriter.writeUnshared( requestExecution );
            socketWriter.flush();
            
            ObjectInputStream socketReader = new ObjectInputStream( executionRequestSocket.getInputStream());
            
            //wait the response
            Object responseObject = socketReader.readUnshared();
            if( !( responseObject instanceof Packet ) )
            {
                //error
                logger.info( "Invalid object read from stream, execution error" );
                executionResult = Packet.ERROR_RESPONSE;
                
            }
            else
            {
                Packet response = (Packet) responseObject;
                
                executionResult = response.getPacketType();
                
                commandOutput = response.getOutput();
                
                commandError = response.getError();
                
                logger.debug( " Command Output " + commandOutput );

                logger.debug( " Command Error " + commandError );
                
                
                /*if(cmd.length > 0 && cmd[0].equals(TSServer.FULL_OCR_EXECUTABLE_PATH)) {
                	
                }*/
                
                
                returnValue = response.getReturnValue();
            }
            
            logger.debug( "Execution result:" + executionResult );

            executionRequestSocket.close();
        }
		catch( SocketTimeoutException ste ){
			//OCR timeout
			
			//mark execution result to terminate dip retry in TSServer
			returnValue = EXEC_TIMEOUT;
			
			ste.printStackTrace(System.err);
			if( searchId > 0 ){
	            IndividualLogger.info( "DIP: Image processing takes more than " + (600000/1000) + " seconds, DIP aborted!" ,searchId);
	        	SearchLogger.info("<br>DIP: Image processing takes more than " + (600000/1000) + " seconds, DIP aborted!", searchId);
			}
		}
        catch( Exception e)
        {
        	boolean retryWasSuccesful = false;
        	if(retryStartServerExecutor && executionRequestSocket == null) {
        		
        		String restartTokens = ServerConfig.getServerExecutorRestartTokens();
        		
        		if(StringUtils.isNotBlank(restartTokens)) {
        		
	        		synchronized(ClientProcessExecutor.class) {
						if(!triedAutoStart) {
							triedAutoStart = true;
						
							if(process != null) {
								try { process.destroy(); } catch (Exception exception){}
							}
						
							String[] commands = restartTokens.split("\\s*;\\s*");
							for (String command : commands) {
								try {
									String[] tokens = command.split("\\s*,\\s*");
									int tokensLength = tokens.length;
									if(tokensLength > 1) {
										long sleep = Long.parseLong(tokens[tokensLength - 1]);
							            ProcessBuilder pb = new ProcessBuilder(Arrays.copyOf(tokens, tokensLength - 1 ));
						        		process = pb.start();
						        		Thread.sleep(sleep);
							            
									}
								} catch (Exception exception) {
									logger.error("Cannot run tokens " + command, exception);
								}
							}
							
							
			        		
			        		try {
			        			start(false);
								triedAutoStart = false;
								retryWasSuccesful = true;
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
			        		
						}
	        		}
        		}
        		
        	}
        	
        	if(!retryWasSuccesful) {
	            logger.info( "Error while receiving result!!!" );
	            returnValue = -22222;
	            //log exceptions once every 10 minutes
	            if( System.currentTimeMillis() - lastEmailSentMillis >= TIME_BETWEEN_EMAILS || lastEmailSentMillis == -1 ) {           
	            	Log.sendExceptionViaEmail( MailConfig.getExceptionEmail(), "Error while receiving result!!!", e );
	            	
	            	lastEmailSentMillis = System.currentTimeMillis();
	            }
	            
	            e.printStackTrace();
	            
	            if( executionRequestSocket != null )
	            {
	                try
	                {
	                    if( Packet.testMask( executionResult, Packet.INVALID_VALUE ) )
	                    {
	                        //if exception occurred before success, error response
	                        executionResult = Packet.ERROR_RESPONSE;
	                    }
	                    
	                    executionRequestSocket.close();
	                }catch( Exception e2 ) {}
	            }
        	}
        }
        
        if( !Packet.testMask(executionResult, Packet.SUCCESS_RESPONSE) )
        {
            if( cmd[0].indexOf( "netstat" ) < 0 )
            {
                throw new IOException( "Error executing " + cmd[0] );
            }
        }
    }
    
    public String getCommandOutput()
    {
        return commandOutput;
    }
    
    public String getErrorOutput()
    {
        return commandError;
    }
    
    public int getReturnValue()
    {
        return returnValue;
    }

	public  String getWorkingDirectory() {
		return workingDirectory;
	}

	public  void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
}