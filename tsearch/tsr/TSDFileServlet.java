package ro.cst.tsearch.tsr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.ReplaceStringInFile;
import ro.cst.tsearch.utils.StringUtils;


public class TSDFileServlet extends HttpServlet
//public class TSDFileServlet extends GenericServlet
{
	protected static final Category logger = Category.getInstance(TSDFileServlet.class.getName());

//  public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException
    {
    	if (logger.isDebugEnabled())    	
    		logger.debug("!START " + this.getClass().getName());

        ServletInputStream  sis = req.getInputStream ();
        ServletOutputStream sos = res.getOutputStream();

        //read client's command from input stream
		BufferedReader in = new BufferedReader(new InputStreamReader( sis ));

		int BUFFER_SIZE_KB = 100;
		int maxLen = BUFFER_SIZE_KB * 1024;
		char cb[] = new char[maxLen];

		StringBuffer allContent = new StringBuffer("");
		String tempContent = "";
		String inputLine   = "";
		int nc      = 0;
		int totalnc = 0;
		nc = in.read(cb, 0, maxLen);
		while (nc >= 0) {
			tempContent = new String(cb, 0, nc);
			allContent.append(tempContent);
			totalnc = totalnc + nc;
			nc = in.read(cb, 0, maxLen);
		}
		
		if(in!=null){
			in.close();
		}
		//in.close();

		//dispatch the client's command:
		// 1. first character is the the command (R - read file, W - write file)
		// 2. from second character to the '|' character, we have the file name
		// 3. from the '|' character to the end of the file we have the file
		//    if command is W - write file.
		String clientReq = allContent.toString().trim();
		if ( clientReq.equals("") ) {
		    logger.debug("!DO NOTHING (empty command string) " + this.getClass().getName());
		} else {
		    String command = clientReq.substring(0, 1);
		    int    posBar  = clientReq.indexOf  ("|") ;
		    if ( posBar < 0 ) {
		        logger.debug("!DO NOTHING (invalid command string) " + this.getClass().getName());
		    } else {
		        String fileName = clientReq.substring(1, posBar).trim();
   		        logger.debug("!REQUESTED FILE (" + fileName + ") " + this.getClass().getName());
		        if ( fileName.equals("") ) {
		            logger.debug("!DO NOTHING (invalid requested file) (" + fileName + ") " + this.getClass().getName());
		        } else {
    		        String REAL_PATH   = getServletConfig().getServletContext().getRealPath("/");
    		        if ( REAL_PATH.endsWith( File.separator ) )
    		            REAL_PATH = REAL_PATH.substring(0, REAL_PATH.length()-1);
                    if ( !fileName.startsWith( File.separator ) )
                        fileName = File.separator + fileName;
    		        String fullFileName = BaseServlet.FILES_PATH + File.separator + fileName;
    		        String fileContent = null;
    		        if ( posBar < clientReq.length()-1 ) {
    		            //in case of read, we have the file comming in the request
    		            fileContent = clientReq.substring(posBar+1);
    		        }
        		    if ( command.equalsIgnoreCase("R") ) {
    		            logger.debug("!REQUESTED OPERATION (read) (" + fileName + ") " + this.getClass().getName());
        		        //the client requests reading a file so
        		        //we get the file and we write it on the response
                        StringBuffer fileContentBuffer = new StringBuffer("");
                        BufferedReader in2 = new BufferedReader(new FileReader( fullFileName ));
                        while ( (inputLine = in2.readLine()) != null )
                            fileContentBuffer.append( inputLine + "\r\n");
                        in2.close();

        		        byte[] requestedFile = fileContentBuffer.toString().getBytes();
        				sos.write(requestedFile, 0, requestedFile.length);
        				sos.flush();
        				//sos.close();
        		    } else {
        		        if ( command.equalsIgnoreCase("W") ) {
    		                logger.debug("!REQUESTED OPERATION (write) (" + fileName + ") " + this.getClass().getName());
        		            if ( fileContent == null || fileContent.trim().equals("") ) {
    		                    logger.debug("!DO NOTHING (received empty file to be written) (" + fileName + ") " + this.getClass().getName());
    		                } else {
    		                    //the client requests writing a file, so we save it
                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullFileName, false)));
/*
                                logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                                logger.info(fullFileName);
                                logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                                logger.info(fileContent);
                                logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
*/
                                out.println( fileContent );
                                out.flush();
                                out.close();
								
                                ReplaceStringInFile.replaceInFile(new File(fullFileName), StringUtils.regex , "<!-- NEW PAGE -->");
								
    		                }
    		            } else {
    	                    logger.debug("!DO NOTHING (invalid requested operation) (" + fileName + ") " + this.getClass().getName());
    		            }
        		    }
        		}
    		}
		}

		logger.debug("!END " + this.getClass().getName());
    }

}
