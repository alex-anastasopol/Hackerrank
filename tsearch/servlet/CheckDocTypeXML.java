package ro.cst.tsearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.titledocument.abstracts.DocTypeReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.RequestParams;

/**
 * @author nae
 */
public class CheckDocTypeXML extends BaseServlet {
    /**
     *  
     */
	private static final Category logger = Category.getInstance(CheckDocTypeXML.class.getName());
    public static final String TSD_FILE = "tsd_file";

    public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException, BaseException {

        MultipartParameterParser mpp = null;
        mpp = new MultipartParameterParser(request);
        
        //MultipartParameterParser mpp = InstanceManager.getCurrentInstance().getMultipartParameterParser();
        
        //long searchId = mpp.getMultipartLongParameter(RequestParams.SEARCH_ID);
        File XMLFiles = null;
        XMLFiles = mpp.getFileParameter(TSD_FILE, null);
        if (XMLFiles != null && XMLFiles.length() == 0) {
            throw new BaseException("Please Enter a Valid File!");
        }
        PrintWriter out = response.getWriter();
        DocTypeReader.check(XMLFiles, out);
        //out.println("<br><br><br><input type=button value='Back' onclick='javascript:history.go(-1);'>");
        
        if (XMLFiles.delete() == true) {
            logger.info("File " + XMLFiles.getName() + " succesufully deleted from WEB SERVER!");
        } else {
            logger.info("Cannot delete File " + XMLFiles.getName() + " from WEB SERVER!");
        }
    }
}