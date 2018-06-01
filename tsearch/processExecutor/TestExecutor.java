package ro.cst.tsearch.processExecutor;

import java.io.File;
import java.util.Map;

public class TestExecutor {
	public static void main(String[] args) {
		if(args.length == 0)
			System.err.println("Must enter at least one argument");
		
		for (int i = 0; i < args.length; i++) {
			System.out.println("arg[" + i + "]=[" + args[i] + "]");
		}
		
		
		ProcessBuilder pb = new ProcessBuilder(args);
		if(args.length > 0) {
			if(args[0].contains("/dip")) {
				String temp = args[0].substring(0, args[0].lastIndexOf("/"));
				if(!temp.isEmpty()) {
					pb.directory(new File(temp));
				}
			}
		}
		try {
			Process process = pb.start();
			
			Map<String, String> envs = System.getenv();
			for (String key : envs.keySet()) {
				System.out.println("[" + key + "]=[" + envs.get(key) + "]");
			}
			long processStartTime = System.currentTimeMillis();
			boolean processFinished = false;
            boolean mustKill = false;
            int k;
            //must kill executed process if it's execution time exceeds WAIT_TIME seconds
            
            while( !processFinished ){
            	
            	processFinished = false;
            	
	            try{
	            	//try to get process exit value
	            	k = process.exitValue();
	            	System.out.println("k = " + k);
	            	//will reach only if the above line does not issue an IllegalThreadStateException exception
	            	//here the process has exited with code k
	            	processFinished = true;
	            }
	            catch( IllegalThreadStateException itse ){
	            	//thread not yet terminated
	            	
	            	//compute the wait time until now
	            	long processExecTime = System.currentTimeMillis();
	            	
	            	//if WAIT_TIME second exceeded, terminate wayting
	            	if( processExecTime - processStartTime > 100000 ){
	            		processFinished = true;
	            		
	            		//must kill process after while loop
	            		mustKill = true;
	            	}
	            }
	            
	            if( !processFinished ){
	            	//if not finished, wait one second then try again
	            	synchronized( process ){
	            		try{
	            			process.wait( 1000 );
	            		}
	            		catch( Exception we ){
	            			we.printStackTrace();
	            		}
	            	}
	            }
            }
            
            if( mustKill ){
				process.destroy();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
