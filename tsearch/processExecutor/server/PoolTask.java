
package ro.cst.tsearch.processExecutor.server;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import ro.cst.tsearch.processExecutor.Packet;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.threads.ProcessStreamReader;


class PoolTask implements Runnable{
		Socket sok=null;

		PoolTask(Socket sok){
			this.sok = sok;
	    }
	        
	    public void run(){	
	    	
	    	//ServerExecutor.log(" ==============  Start PoolTask client : "+sok);
	    	
	    	if(sok == null){
	    		return;
	    	}
	    	
	    	ObjectInputStream 	inputStream = null;
	    	ObjectOutputStream 	outputStream = null;
	    	
	    	//procesare sarcina
	    	try{
	    		int packetType=Packet.SUCCESS_RESPONSE;
	    		
	    		inputStream = new ObjectInputStream (sok.getInputStream());
				outputStream = new ObjectOutputStream (sok.getOutputStream());
				
				//ServerExecutor.log(" -------- Obtin stream-uri cu succes "+sok);
				
				Packet pack=(Packet)inputStream.readUnshared();
				
				//ServerExecutor.log(" -------- Packet citit cu succes "+ sok);
				
				ProcessStreamReader errStreamReader = null;
			    ProcessStreamReader outStreamReader = null;
	    		
			    try{
			    	String []command=pack.getCommand();
					ProcessBuilder pb = new ProcessBuilder(command);  
					
					//un mic artificiu pana modificam in toata aplicatia sa i se trimita si working directory
					try{
						String workingDir = pack.getWorkingDirectory();
						if(workingDir!=null && !workingDir.isEmpty()){
						//if(command.length>0&&command[0].indexOf("dip")>=0){
							//ServerExecutor.log("changing working directory to " + workingDir);
							pb.directory(new File(workingDir));
						}
					}
					catch(Exception e){
						//ServerExecutor.log(" === ERROR" + e.getMessage());
					}
				    //final de mic artificiu :)

					
					final Process process = pb.start();
				    long processStartTime = System.currentTimeMillis();
					
				    //ServerExecutor.log(" -------- Process pornit cu success "+ sok);
				    
				    errStreamReader = new ProcessStreamReader( process.getErrorStream() );
		            outStreamReader = new ProcessStreamReader( process.getInputStream() );
		            
		           // ServerExecutor.log(" -------- Obtinere stream-uri process cu success "+ sok);
		            
		            if( errStreamReader != null ){
		                errStreamReader.start();
		            }
		            if(outStreamReader != null ){
		            	outStreamReader.start();
		            }
		            
		            //ServerExecutor.log(" -------- Astept terminare process "+ sok);
		            int k = 0;
		            boolean processFinished = false;
		            boolean mustKill = false;
		            //must kill executed process if it's execution time exceeds WAIT_TIME seconds
		            
		            while( !processFinished ){
		            	
		            	processFinished = false;
		            	
			            try{
			            	//try to get process exit value
			            	k = process.exitValue();
			            	
			            	//will reach only if the above line does not issue an IllegalThreadStateException exception
			            	//here the process has exited with code k
			            	processFinished = true;
			            }
			            catch( IllegalThreadStateException itse ){
			            	//thread not yet terminated
			            	
			            	//compute the wait time until now
			            	long processExecTime = System.currentTimeMillis();
			            	
			            	//if WAIT_TIME second exceeded, terminate wayting
			            	if( processExecTime - processStartTime > ClientProcessExecutor.WAIT_TIMEOUT ){
			            		processFinished = true;
			            		
			            		//must kill process after while loop
			            		mustKill = true;
			            	}
			            }
			            
			            if( !processFinished ){
			            	//if not finished, wait one second then try again
			            	synchronized( this ){
			            		try{
			            			wait( 1000 );
			            		}
			            		catch( Exception we ){
			            			we.printStackTrace();
			            		}
			            	}
			            }
		            }
					
					//ServerExecutor.log(" -------- Terminare process cu success "+ sok);
					
					pack.setReturnValue(k);
		            
					if( mustKill ){
						process.destroy();
					}
					
		            if(pack.testMask(Packet.CAPTURE_OUTPUT_MASK)){
		            	if(outStreamReader!=null){
		            		outStreamReader.join(60000);
		            	}
		            	if(outStreamReader!=null && outStreamReader.isAlive()){
		            		outStreamReader.interrupt();
		            	}
		            	pack.setOutput(outStreamReader.getOutput());
		            }
		            if(pack.testMask(Packet.CAPTURE_ERROR_MASK)){
		            	if(errStreamReader!=null){
		            		errStreamReader.join(60000);
		            	}
		            	if(errStreamReader!=null && errStreamReader.isAlive()){
		            		errStreamReader.interrupt();
		            	}
		            	pack.setError(errStreamReader.getOutput());
		            }
		           
					process.destroy();
					//ServerExecutor.log(" -------- Process distrus "+ sok);
	    		}
	    		catch(Exception e){
	    			//ServerExecutor.log(" -------- Eroare procesare packet mesaj: \n"+e.getMessage()+"\n");
	    			packetType = Packet.ERROR_RESPONSE;
//	    			e.printStackTrace(System.err);
	    		}
	    		
				pack.setPacketType(packetType);
				outputStream.writeUnshared(pack);
				outputStream.flush();
				//ServerExecutor.log(" -------- Am trimis packetul de raspuns "+ sok);
	    	}
	    	catch(Exception e){
	    		//ServerExecutor.log(" -------- Eroare la citirea din socket sok  "+ sok);
	    		e.printStackTrace();
	    	}
	    	finally{
	    		try{
	    			inputStream.close();
	    		}
	    		catch(Exception e){
	    			//ServerExecutor.log(" =========== Nu am reusit sa inchid inputStream : "+ inputStream);
	    		}
	    		
	    		try{
	    			outputStream.close();
	    		}
	    		catch(Exception e){
	    			//ServerExecutor.log(" =========== Nu am reusit sa inchid inputStream : "+ outputStream);
	    		}
	    		
	    		try{
	    			this.sok.close();
	    			//ServerExecutor.log(" =========== Inchid soketul orice s-ar intampla "+ sok);
	    		}
	    		catch(Exception e){
	    			//ServerExecutor.log(" =========== Nu am reusit sa inchid soketul : "+ sok);
//	    			e.printStackTrace(System.err);
	    		}
	    	}
	    }
	    
	}