package ro.cst.tsearch.threads;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessStreamReader extends Thread
{
    private StringBuffer outputString;
    private BufferedReader br = null;
    
    public ProcessStreamReader( InputStream streamToRead )
    {
        outputString = new StringBuffer("");
        br = new BufferedReader(new InputStreamReader( streamToRead ));
    }
    
    public void run()
    {
        try
        {
            String l=null;
            while ((l=br.readLine())!=null)
            {
                outputString.append(l);
                outputString.append("\n");
            }
        }
        catch (Exception e) {
        }
        finally{
        	try{
        		br.close( );
        	}
        	catch(Exception e){
        		//nothing to do
        	}
        }
    }
    
    public String getOutput()
    {
        return outputString.toString();
    }
}