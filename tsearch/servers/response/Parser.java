package ro.cst.tsearch.servers.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.wrapper;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.monitor.ParserTime;
import ro.cst.tsearch.search.filter.NameFilterResponse;
import ro.cst.tsearch.search.filter.parser.address.AddressParser;
import ro.cst.tsearch.search.filter.parser.name.AssessorNameParser;
import ro.cst.tsearch.search.filter.parser.name.DesotoAssessorNameParser;
import ro.cst.tsearch.search.filter.parser.name.NameParser;
import ro.cst.tsearch.search.filter.parser.name.ShelbyRegisterNameParser;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.wrapper2.RuleTreeManipulator;
import ro.cst.tsearch.wrappers.HtmlDescriptor;
import ro.cst.tsearch.wrappers.HtmlNode;
import ro.cst.tsearch.wrappers.Landmark;
import ro.cst.tsearch.wrappers.Rule;
import ro.cst.tsearch.wrappers.Rules;

public class Parser implements Serializable {

    static final long serialVersionUID = 10000000;

    private static final Category logger = Category.getInstance(Parser.class.getName());

    private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + Parser.class.getName());

    private final int OIS = 1, ODS = 2, PAS = 3, PDS = 4, PIS = 5, SDS = 6,
            THS = 7, CDIS = 8;

    public static final int PAGE_DETAILS = 1;

    public static final int NO_PARSE = 2;

    public static final int CROSS_REF = 3;

    public static final int PAGE_DETAILS_NO_CROSS_REF = 4;

    protected static final int PAGE_ROWS_HEAD = 100;

    public static final int PAGE_ROWS = 105;

    public static final int PAGE_ROWS_NAME = 106;

    public static final int PAGE_ROWS_ADDRESS = 107;

    public static final int PAGE_ROWS_BOOK_PAGE = 108;
    
    public static final int PAGE_ROWS_BOOK_PAGE2 = 111;
    
    public static final int PAGE_ROWS_NAME2 = 112;
    
    public static final int PAGE_ROWS_SUBDIV_CODE = 109;

    public static final int PAGE_ROWS_DUMMY = 110;

    protected static final int PAGE_TO_ONE_ROW = 100;

    public static final int ONE_ROW = 205;

    public static final int ONE_ROW_NAME = 206;

    public static final int ONE_ROW_ADDRESS = 207;

    public static final int ONE_ROW_BOOK_PAGE = 208;
    
    public static final int ONE_ROW_SUBDIV_CODE = 209;

    public static final int ONE_ROW_DUMMY = 210;
    
    public static final int ONE_ROW_BOOK_PAGE2 = 211;  

    public static final int ONE_ROW_NAME2 = 212;
    
    protected static Map oneRowMap = new HashMap();

    
    static {
    	//in new version all is nedeed is a notnull value for the ids  coresponding to intermediar calls
    	//just for old compatibility we put all intermediary ID-s with an non nulll  value that now is ignored , but  make the parser to use the "...rule/inter"  directory
    	oneRowMap .put(PAGE_ROWS, "");
    	oneRowMap .put(PAGE_ROWS_NAME, "");
    	oneRowMap .put(PAGE_ROWS_ADDRESS, "");
    	oneRowMap .put(PAGE_ROWS_BOOK_PAGE, "");
    	oneRowMap .put(PAGE_ROWS_BOOK_PAGE2, "");
    	oneRowMap .put(PAGE_ROWS_NAME2, "");
    	oneRowMap .put(PAGE_ROWS_SUBDIV_CODE, "");
    	oneRowMap .put(PAGE_ROWS_DUMMY, "");
    	
    	// the value are still used for sites that has not yet moved to the new mechanism
    	oneRowMap .put(ONE_ROW, "");
    	oneRowMap .put(ONE_ROW_NAME, "Name");
    	oneRowMap .put(ONE_ROW_ADDRESS,"Address");
    	oneRowMap .put(ONE_ROW_BOOK_PAGE,"BookPage");
    	oneRowMap .put(ONE_ROW_SUBDIV_CODE,"SubdivCode");
    	oneRowMap .put(ONE_ROW_DUMMY,"Dummy");
    	oneRowMap .put(ONE_ROW_BOOK_PAGE2,"BookPage2");
    	oneRowMap .put(ONE_ROW_NAME2,"Name2");
        
    }

    private int miServerID;

    private String msRealPath;

    private Search search;
    
    private String header; // set by sites that need the header of the intermediate results table to be passed to parser
    
    protected TSServer tsserver;
    
    protected static String getFileName(String s) {
        s = s.substring(7).toLowerCase();
        for (int i = s.indexOf("_"); i != -1; i = s.indexOf("_"))
            s = s.substring(0, i) + Character.toUpperCase(s.charAt(i + 1))
                    + s.substring(i + 2);
        return s;
    }

    protected static String IDtoName(int id) throws Exception {
        if (!isOneRow(id))
            throw new Exception("ID " + id + " is not an 'One Row' id");
        String s = (String) oneRowMap.get(new Integer(id));
        if (s == null)
            throw new Exception("Not a valid id : " + id);
        return s;
    }
    protected long searchId =-1;
    public Parser(String sRealPath, int serverID, Search srch,long searchId) {
    	this.searchId = searchId;
        //logger.info("init parser for server " + serverID);
        //logger.info("RealPath: [" + sRealPath + "]");
        msRealPath = sRealPath;
        miServerID = serverID;
        search = srch;
    }

    public void Parse_old(ParsedResponse pr, String sHtml, int iHtmlID)
            throws IOException {
        //logger.info("starting parse for html page: " + iHtmlID);
        HtmlDescriptor tree;

        //logger.debug("Parser: Apelat cu parametrii SID="+miServerID+"
        // HID="+iHtmlID);

        if ((tree = GetHtmlDescriptor(miServerID, iHtmlID)) != null) {
            ArrayList al = new ArrayList();
            //al.add(new Integer(0));
            //logger.info("start parsing ");
            sHtml = Parse(pr, sHtml.replaceAll("\\n", ""), tree.getNode(al));

            //bug fix--- do not standardize Register Documents
            if (miServerID != (int) TSServersFactory.getSiteId("TN", "Shelby", "RO"))
                standardizePIS(pr);//this function is an work around for
                                   // Assessor site it should be removed
        }
    }

    /**
     * varianta 2
     */
    /**
     * Gets an int from ResoutceBundle for coresponding key
     */
    private static int getInt(ResourceBundle rb, String key) {
        return Integer.parseInt(rb.getString(key).trim());
    }

    protected static boolean isPageRows(int id) {
        return id > PAGE_ROWS_HEAD && id < PAGE_ROWS_HEAD + PAGE_TO_ONE_ROW;
    }

    protected static boolean isOneRow(int id) {
        return id > PAGE_ROWS_HEAD + PAGE_TO_ONE_ROW;
    }

    public void setHeader(String value){
    	header = value;
    }
    
    public void Parse(ParsedResponse pr, String sHtml, int iHtmlID)throws ServerResponseException {
    	Parse(pr, sHtml, iHtmlID, "", -1, "","");
    }
    
    public void Parse(ParsedResponse pr, String sHtml, int iHtmlID, String parserConfigurationFileName, String parserConfigurationFileNameOverWrite)
            throws ServerResponseException {
        Parse(pr, sHtml, iHtmlID, "", -1, parserConfigurationFileName, parserConfigurationFileNameOverWrite);
    }

    public void Parse(ParsedResponse pr, String sHtml, int iHtmlID,
            String linkStart, int action) throws ServerResponseException {
    		Parse(pr,sHtml,iHtmlID,linkStart,action,"","");
    }
    
    public void Parse(ParsedResponse pr, String sHtml, int iHtmlID,
            String linkStart, int action, String parserConfigurationFileName, String parserConfigurationFileNameOverWrite) throws ServerResponseException {

        long startTime = System.currentTimeMillis();

        pr.setResponse(sHtml);
        if (iHtmlID == NO_PARSE) {
            return;
        }
        
        DefaultTreeModel model;

        //Reading config to identify which parser to use
        ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.PARSER_CONFIG);
        int PType = getInt(rbc, "Parser.Type");

        
        if (miServerID == (int) TSServersFactory.getSiteId("TN", "Shelby", "TR")) { // Shelby TR (County Tax)
            if (iHtmlID == PAGE_ROWS_NAME) {
                splitResultRows(pr, sHtml, ONE_ROW, "<tr><td><a ",
                        "</table>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == PAGE_ROWS_ADDRESS) {
                splitResultRows(pr, sHtml, ONE_ROW,
                        "<tr valign=\"top\"><td><a href=", "</table>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == ONE_ROW) {
                parseShelbyOneRowTR(pr, sHtml, linkStart, action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
        
        } else if (miServerID == (int) TSServersFactory.getSiteId("TN", "Shelby", "CT")) { // Shelby TR (City Tax)
            if (iHtmlID == PAGE_ROWS) {
                splitResultRows(pr, sHtml, ONE_ROW,
                        "<tr bgcolor=", "<tr>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == ONE_ROW) {
                parseShelbyOneRowEP(pr, sHtml, linkStart, action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
        
        } else if (miServerID == (int) TSServersFactory.getSiteId("TN" , "Shelby", "RO")) { // Shelby RO
            if (iHtmlID == PAGE_ROWS) {
                splitResultRows(pr, sHtml, ONE_ROW, "<tr", "</table",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                
        		// remove table header
                Vector rows = pr.getResultRows();
                if (rows.size()>0){
                        ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
                        pr.setResultRows(rows);
                        pr.setHeader(pr.getHeader()+firstRow.getResponse());
                } 

                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == PAGE_ROWS_NAME) {
                splitResultRows(pr, sHtml, ONE_ROW_NAME, "<tr",
                        "</table", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == ONE_ROW) {
                defaultParseRow(pr, sHtml, linkStart, action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == ONE_ROW_NAME) {
                parseShelbyRegisterPageOneRowNameSearch(pr, sHtml, linkStart,
                        action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<tr><td><A HREF=\"",
                        "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        
        } else if (miServerID == TSServersFactory.getSiteId("TN", "Williamson", "RO")) { // Williamson RO
            if (iHtmlID == PAGE_DETAILS) {
                
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<tr><td><A HREF=\"",
                        "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        
        } else if (miServerID == TSServersFactory.getSiteId("MO", "Clay", "RO")) { // Clay RO
            if (iHtmlID == PAGE_DETAILS) {
                
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<A HREF='",
                        "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        
        } else if (miServerID == TSServersFactory.getSiteId("KS", "Johnson", "RO")) { // JohnsonRO
            if (iHtmlID == PAGE_DETAILS) {
                String ss = sHtml;
                int idxStart = ss.lastIndexOf("<a href=");
                int idxEnd=-1;
                	if (idxStart != -1)
                	{
                	    	idxEnd = ss.indexOf("</a>",idxStart);
                	    	if (idxEnd != -1)
                	    	{
                	    	    if (ss.substring(idxStart, idxEnd).indexOf("Show Detail For All Marginals") != -1)
                	    	        ss = ss.substring(0, idxStart) + ss.substring(idxEnd+4, ss.length());
                	    	}
                	}
                idxStart = ss.indexOf("<a href=");
                idxEnd = ss.indexOf("</a>", idxStart);
                try {
	                if (ss.substring(idxStart, idxEnd).indexOf("Display Doc") != -1)                
	                    ss = ss.replaceFirst("<a href=", "<A HREF=");
                } catch(StringIndexOutOfBoundsException e){}
                findSubResults(pr, ss, ONE_ROW_DUMMY, "<a href='", "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        
        } else if (miServerID == TSServersFactory.getSiteId("MO", "Jackson", "RO")) { // JacksonRO
            if (iHtmlID == PAGE_DETAILS) {
                
                String ss = sHtml;
                
                int index = ss.indexOf("Related Documents");
                if (index > 0) {
                    index = ss.lastIndexOf("<table ", index);
                    ss = ss.substring(index, ss.length());
                }
                //findSubResults(pr, ss, ONE_ROW_DUMMY, "<a href=", "</a>", linkStart, action);
                
                Vector parsedRows = new Vector();

                int startIdx = ss.indexOf("<a href=", 0);
                int endIdx = ss.indexOf("</a>", startIdx + 1);
              
                while ((startIdx != -1) && (endIdx != -1)) {
                    String row = ss.substring(startIdx, endIdx);
                    
                    int i = row.lastIndexOf(">");
                    int j = row.indexOf("<", i);
                    if (j == -1) j = row.length();
                    String instno = row.substring(i + 1, j);
                
                    ParsedResponse pp = parseOneRow(row, ONE_ROW_DUMMY, linkStart, action,
                    		parserConfigurationFileName, parserConfigurationFileNameOverWrite, pr.isParentSite());
                    
                    LinkInPage lip = pp.getPageLink();
                    
                    lip.setOnlyLink(lip.getLink() + "&insno=" + instno + "&isSubResult=true" );
                    lip.setOnlyOriginalLink(lip.getOriginalLink() + "&insno=" + instno + "&isSubResult=true");
                    
                    pp.setPageLink(lip);
                    
                    parsedRows.add(pp);

                    startIdx = ss.indexOf("<a href=", endIdx);
                    endIdx = ss.indexOf("</a>", startIdx + 1);
                }

                pr.setOnlyResultRows(parsedRows);
                
            }
            if (iHtmlID == ONE_ROW_DUMMY || iHtmlID == ONE_ROW) {
                defaultParseRow(pr, sHtml, linkStart, action, true);
                
                if (iHtmlID == ONE_ROW_DUMMY) return;
                
                try {

                    XMLExtractor xmle = new XMLExtractor(sHtml, new Object[] {
                            new Long(miServerID), IDtoName(iHtmlID) },
                            msRealPath,this.searchId, parserConfigurationFileName, pr.isParentSite());
                    xmle.process(); 
                    ResultMap resultMap = xmle.getDefinitions();
                    
                    Bridge bridge = new Bridge(pr, resultMap, searchId);
                    pr.setDocument(bridge.importData());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                return;
            }
        
        	}else if (miServerID == TSServersFactory.getSiteId("TN", "Wilson", "RO")) { // Wilson RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<tr><td><A HREF='",
                        "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        
        }

        else if (miServerID == TSServersFactory.getSiteId("MO", "Jackson", "OR") ||
                    miServerID == TSServersFactory.getSiteId("KS", "Johnson", "OR") ||
                    miServerID == TSServersFactory.getSiteId("MO", "Clay", "OR") || 
                    miServerID == TSServersFactory.getSiteId("MO", "Platte", "OR") ||
                    miServerID == TSServersFactory.getSiteId("KS", "Wyandotte", "OR")) {
        	
        	if (iHtmlID == PAGE_ROWS_BOOK_PAGE2) {
                splitResultRows(pr, sHtml, ONE_ROW_SUBDIV_CODE, "<a href=\"",
                        "</table", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
        	
        	if (iHtmlID == ONE_ROW_SUBDIV_CODE) {
                defaultParseRow(pr, sHtml, linkStart, action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
        	
        }
        else if (miServerID == TSServersFactory.getSiteId("TN", "Rutherford", "RO")) { //Rutherford RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a href=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
            
        } else if (miServerID == TSServersFactory.getSiteId("TN", "Montgomery", "RO")) { // Montgomery RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a href=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        } else if(miServerID == TSServersFactory.getSiteId("TN", "Sumner", "RO")) {
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a href=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        } else if (miServerID == TSServersFactory.getSiteId("MI", "Wayne", "RO")) { // Montgomery RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a href=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
        } else if (miServerID == TSServersFactory.getSiteId("KY", "Jefferson", "RO")) { // Jefferson RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a HREF=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
            
        } 
        else if (miServerID == TSServersFactory.getSiteId("TN", "Hamilton", "RO")) { // Hamilton RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<A HREF=", "</A>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }       
            if (iHtmlID == ONE_ROW_DUMMY) 
            {
             //   Vector parsedRows = new Vector();
                //ParsedResponse pp = new ParsedResponse();//parseOneRow(sHtml, ONE_ROW_DUMMY, linkStart, action);
                
                String lnk =  sHtml.substring(sHtml.indexOf("\""), sHtml.lastIndexOf("\""));
                if (lnk.length() > 1)
                {
                    LinkInPage lip = new LinkInPage(lnk, lnk);
                    lip.setActionType(TSServer.REQUEST_SAVE_TO_TSD);                                                                           
                    pr.setPageLink(lip);
                }
               
                 return;
            }
            
        } else if (miServerID == TSServersFactory.getSiteId("TN", "Knox", "RO")) { // Knoxville RO
            if (iHtmlID == ONE_ROW) {
                defaultParseRow(pr, sHtml, linkStart, action);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a href=", "</a>",
                        linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
            
        } else if (miServerID == TSServersFactory.getSiteId("MS", "DeSoto", "RO")) {
            
            if (iHtmlID == CROSS_REF) {
                defaultParseRow(pr, sHtml, linkStart, action, true);
                ParserTime.update(System.currentTimeMillis() - startTime);
                return;
            }
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, CROSS_REF, "<a href=\"/title",
                        "</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }
            if (iHtmlID == PAGE_DETAILS_NO_CROSS_REF) {
                iHtmlID = PAGE_DETAILS;
            }
        }        
        else if (miServerID == TSServersFactory.getSiteId("IL", "Cook", "RO")){
	     	
	    	if (iHtmlID == PAGE_ROWS_NAME2) {
	    		splitResultRows(pr, sHtml, ONE_ROW_SUBDIV_CODE, "<tr", "</tr>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
	            ParserTime.update(System.currentTimeMillis() - startTime);
	            return;
	        }
	    	
	    	if (iHtmlID == ONE_ROW_SUBDIV_CODE) {
	            defaultParseRow(pr, sHtml, linkStart, action);
	            ParserTime.update(System.currentTimeMillis() - startTime);
	            return;
	        }    	

	    	 if (iHtmlID == PAGE_DETAILS) {                
                 findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a HREF='","</a>", linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite);
             }            
        } else if (miServerID == TSServersFactory.getSiteId("MI", "Macomb", "RO") 
        		|| miServerID == TSServersFactory.getSiteId("MI", "Oakland", "RO")) { // MIMacomb, MIOakland RO
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a HREF='", "</a>",
                        linkStart, action, parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }            
        } else if(miServerID == TSServersFactory.getSiteId("FL", "Hillsborough", "RO")){
            if (iHtmlID == PAGE_DETAILS) {
                findSubResults(pr, sHtml, ONE_ROW_DUMMY, "<a HREF='", "</a>",
                        linkStart, action, parserConfigurationFileName, parserConfigurationFileNameOverWrite);
            }         	
        }
                
        if (isPageRows(iHtmlID)) {
            try {
            	if (miServerID == TSServersFactory.getSiteId("MO", "Jackson", "TR") ||
            			miServerID == TSServersFactory.getSiteId("MO", "Clay", "TR")){
            		 Class c = TSServersFactory.IdToClass(miServerID);
 	                Method m = c.getMethod("splitResultRows", new Class[] {
 	                        Parser.class, ParsedResponse.class, String.class,
 	                        int.class, String.class, int.class ,long.class});
 	                m.invoke(null, new Object[] { this, pr, sHtml,
 	                        new Integer(iHtmlID + PAGE_TO_ONE_ROW), linkStart,
 	                        new Integer(action) ,new Long(search.getID())});
            	}
            	else{
	                Class c = TSServersFactory.IdToClass(miServerID);
	                Method m = c.getMethod("splitResultRows", new Class[] {
	                        Parser.class, ParsedResponse.class, String.class,
	                        int.class, String.class, int.class });
	                m.invoke(null, new Object[] { this, pr, sHtml,
	                        new Integer(iHtmlID + PAGE_TO_ONE_ROW), linkStart,
	                        new Integer(action) });
            	}
            } catch (Exception e) {
                ParsedResponse pr2 = new ParsedResponse();
                pr2.setError( ServerResponseException.getExceptionStackTrace( e ) );
                throw new ServerResponseException(pr2);
            }
            ParserTime.update(System.currentTimeMillis() - startTime);
            return;
        }

        
        
        if (isOneRow(iHtmlID)) {
            if (iHtmlID != ONE_ROW_DUMMY) {
                long t0 = System.currentTimeMillis();

                try {
                	String str = null;
                	if (header != null){
                		str = header + sHtml;
                	} else {
                		str = sHtml;
                	}
                    XMLExtractor xmle = new XMLExtractor(str, new Object[] {
                            new Long(miServerID), IDtoName(iHtmlID) },
                            msRealPath,this.searchId, parserConfigurationFileName, pr.isParentSite());
                    xmle.process(); 
                    ResultMap resultMap = xmle.getDefinitions();
                    
                    Bridge bridge = new Bridge(pr, resultMap, searchId);
                    pr.setDocument(bridge.importData());

                } catch (Exception e) {
                    e.printStackTrace();
                    pr.setError("Parsing Error: " + e.getMessage());
                    throw new ServerResponseException(pr);
                }
                long t1 = System.currentTimeMillis();
            }
            defaultParseRow(pr, sHtml, linkStart, action);
            ParserTime.update(System.currentTimeMillis() - startTime);
            return;
        }

        
        
        try {

            long t0 = System.currentTimeMillis();
            XMLExtractor xmle = new XMLExtractor(sHtml,
                    new Object[] { new Long(miServerID) }, msRealPath,this.searchId, parserConfigurationFileName, pr.isParentSite());
            //System.out.println("msRealPath: " + msRealPath);
            xmle.process();
           ResultMap resultMap = xmle.getDefinitions();
           
           try {
	           	if(tsserver!=null) {
	           		tsserver.smartParseForOldSites(pr, resultMap, sHtml, iHtmlID);
	           	}
           }catch(Exception e) {
           	e.printStackTrace();
           }
           
            Bridge bridge = new Bridge(pr, resultMap, searchId);
            pr.setDocument(bridge.importData());
            pr = postprocess(pr);
            updateCrossRefWithSources(pr);
            long t1 = System.currentTimeMillis();
            logger.info("Parser operation lasted " + (t1-t0));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            PType = 1;
        } catch (Exception e) {
            e.printStackTrace();
            pr.setError("Parsing Error: " + e.getMessage());
            throw new ServerResponseException(pr);
        }

        switch (PType) {
        	case 0: //call Costin Version
            
	            try {
	                Parse_old(pr, sHtml, iHtmlID);
	            } catch (IOException e1) {
	                logger.error(e1);
	            }
	            break;
        	case 1:

	            model = GetHtmlDescriptor2(miServerID, iHtmlID);
	            wrapper wp = new wrapper();
	            if (model != null) {
	
	                String sir = new String(sHtml);
	
	                try {
	                    wp.Parse_HTML(pr, sir, model);
	                } catch (IOException e2) {
	                    //logger.error(e2);
	                }                
	            }

	            if (miServerID != (int) TSServersFactory.getSiteId("TN", "Shelby", "RO")
                    && miServerID != TSServersFactory.getSiteId("TN", "Williamson", "RO"))
	                standardizePIS(pr);
	            // this function is an work around for
                // Assessor site it should be removed

	            break;
        }

        ParserTime.update(System.currentTimeMillis() - startTime);
    }

    @SuppressWarnings("unchecked")
	private void updateCrossRefWithSources(ParsedResponse pr) {
    	Vector<SaleDataSet> sds = pr.getSaleDataSet();
    	String docType = "";
    	for (SaleDataSet set : sds) {
    		docType = set.getAtribute("DocumentType");
			if(!StringUtils.isEmpty(docType)){
				break;
			}
		}
    	if(StringUtils.isEmpty(docType))
    		return;
    	
    	String crossRefSource = DocumentTypes.getDocumentCategory(docType, searchId);
		Vector<ParsedResponse> rows = pr.getResultRows();
		boolean anyChange = false;
		for (ParsedResponse response : rows) {
			if(response.getPageLink().getLink().contains("isSubResult=true")){
				response.getPageLink().setOnlyLink(
						response.getPageLink().getLink() + "&crossRefSource=" + crossRefSource);
				anyChange = true;
			}
			if(response.getPageLink().getOriginalLink().contains("isSubResult=true")){
				response.getPageLink().setOnlyOriginalLink(
						response.getPageLink().getOriginalLink() + "&crossRefSource=" + crossRefSource);
				anyChange = true;
			}
		}
		if(anyChange)
			pr.setOnlyResultRows(rows);
		Vector<CrossRefSet> crossRefs = pr.getCrossRefSets();
		for (CrossRefSet set : crossRefs) {
			set.setAtribute("CrossRefSource", crossRefSource);
		}
	}

	private ParsedResponse postprocess(ParsedResponse pdata) {
        int c = pdata.getPropertyIdentificationSetCount();
        if (c > 0) {
            PropertyIdentificationSet pis = pdata
                    .getPropertyIdentificationSet(0);
            String bk = (String) pis.getAtribute("PlatBook");
            if (bk != null) {
                String pg = (String) pis.getAtribute("PlatNo");
                if (pg != null) {
                    pg = pg.replaceAll("&", "," + bk + "-");
                    pis.setAtribute("PlatNo", pg);
                }

            }
        }
        return pdata;
    }

    private Vector parseNameSet(String name, NameParser np) {
        return NameFilterResponse.nameTokenListList2NameSetVector(np
                .parseNames(name));
    }

    /////////////////////////////////////////////////////////////////////////
    /// Parsare pagini intermediare :-)
    /////////////////////////////////////////////////////////////////////////

    /// SHELBY ///////////////////////////////////////////////////////////////

    // Shelby TR (County Tax) - Name & Address parsing
    private void parseShelbyOneRowTR(ParsedResponse pr, String sHtml,
            String linkStart, int action) {
        loggerDetails.debug("parse one row Shelby TR (County Tax)");

        // Name
        PropertyIdentificationSet pis = new PropertyIdentificationSet();
        String name = StringUtils.getTextBetweenDelimiters(1, "<td>",
                "</td>", sHtml).trim();
        pis.setAtribute("OwnerLastName", name);
        parseOwnerNames(pis, new DesotoAssessorNameParser());

        // Address
        String add = StringUtils.getTextBetweenDelimiters(2, "<td>",
                "</td>", sHtml).trim();
        pr.setAddressString(AddressParser.parseAddress(add));

        // ParcelID
        String parcelID = StringUtils.getTextBetweenDelimiters(0,
                "\">", "</a>", sHtml).trim();
        pis.setAtribute("ParcelID", parcelID.trim());

        pr.addPropertyIdentificationSet(pis);

        loggerDetails.debug("parsed name=[" + name + "]");
        loggerDetails.debug("parsed addr=[" + add + "]");

        defaultParseRow(pr, sHtml, linkStart, action);
    }

    // Shelby TR (City Tax) - Name & Address parsing
    private void parseShelbyOneRowEP(ParsedResponse pr, String sHtml,
            String linkStart, int action) {
        loggerDetails.debug("parse one row Shelby TR (City Tax)");

        // Name
        PropertyIdentificationSet pis = new PropertyIdentificationSet();
        String name = StringUtils.getTextBetweenDelimiters(1,
                "<td class='table-body'>", "</td>", sHtml);
        pis.setAtribute("OwnerLastName", name);
        parseOwnerNames(pis, new DesotoAssessorNameParser());

        // Address
        String add = StringUtils.getTextBetweenDelimiters(2,
                "<td class='table-body'>", "</td>", sHtml);
        pr.setAddressString(AddressParser.parseAddress(add));

        pr.addPropertyIdentificationSet(pis);

        loggerDetails.debug("parsed name=[" + name + "]");
        loggerDetails.debug("parsed addr=[" + add + "]");

        defaultParseRow(pr, sHtml, linkStart, action);
    }

    // Shelby RO - Name parsing
    private void parseShelbyRegisterPageOneRowNameSearch(ParsedResponse pr,
            String sHtml, String linkStart, int action) {
        loggerDetails.debug("parse one row Shelby RO - name search");
        defaultParseRow(pr, sHtml, linkStart, action);

        String link = pr.getPageLink().getLink();
        pr.setGrantorNameSet(parseNameSet(getLastName(link) + " "
                + getFirstName(link), new ShelbyRegisterNameParser()));
        GenericFunctions1.parseLegalFromGrantorShelbyRO(pr, getLastName(link) + getFirstName(link));
        //standardizePIS(pr);//this function is an work around for Assessor
        // site it should be removed

    }

    /**
     * when a server page resulted after a search may return one or more but at
     * list one result then you may use this function to split that page in a
     * vector of strings, each string containing one and only result
     * 
     * @param vsHtml -
     *            the server page resulted after the search
     * @param vsRowSeparator -
     *            a string the unique separate a result of other inside the
     *            vsHtml. If there is not any vsRowSeparator in vsHtml then the
     *            function return a vector with only one item that store the
     *            entire page
     * @param bAnyRowBeginWith
     *            -if it is true then each result start with vsRowSeparator (but
     *            don't end with vsRowSeparator) else each result end with
     *            vsRowSeparator (but don't start with vsRowSeparator)
     * @return Vector
     */
    
    public void splitResultRows(ParsedResponse pr, String htmlString,
            int pageId, String vsRowSeparator, String rowEndSeparator,
            String linkStart, int action)throws ServerResponseException{
    	splitResultRows(pr, htmlString,
                pageId, vsRowSeparator, rowEndSeparator,
                linkStart, action,"","");
    } 
    
    public void splitResultRows(ParsedResponse pr, String htmlString,
            int pageId, String vsRowSeparator, String rowEndSeparator,
            String linkStart, int action,String parserConfigurationFileName, String parserConfigurationFileNameOverWrite) throws ServerResponseException {
        loggerDetails.debug("splitResultRows in parser");
        loggerDetails.debug("htmlString=" + htmlString);
        loggerDetails.debug("vsRowSeparator=" + vsRowSeparator);
   
        Vector parsedRows = new Vector();
        int startIdx = 0, endIdx = 0;

        startIdx = htmlString.indexOf(vsRowSeparator, 0);
        if (startIdx != -1) {
            pr.setHeader(htmlString.substring(0, startIdx));//the first element
                                                            // is the page
                                                            // header

            endIdx = htmlString.indexOf(vsRowSeparator, startIdx
                    + vsRowSeparator.length());
            while (endIdx != -1) {
                String row = htmlString.substring(startIdx, endIdx);
                parsedRows.add(parseOneRow(row, pageId, linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite, pr.isParentSite()));
                startIdx = endIdx;

                endIdx = htmlString.indexOf(vsRowSeparator, startIdx
                        + vsRowSeparator.length());
            }

            endIdx = htmlString.indexOf(rowEndSeparator, startIdx);
            if (endIdx == -1)
                endIdx = htmlString.length();

            String row = htmlString.substring(startIdx, endIdx);
           
            parsedRows.add(parseOneRow(row, pageId, linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite, pr.isParentSite()));

            pr.setFooter(htmlString.substring(endIdx, htmlString.length())); //the
                                                                             // last
                                                                             // element
                                                                             // will
                                                                             // be
                                                                             // the
                                                                             // footer
        } else {
            endIdx = htmlString.indexOf(rowEndSeparator, 0);
            if (endIdx == -1)
                endIdx = htmlString.length();

            pr.setHeader(htmlString.substring(0, endIdx));
            pr.setFooter(htmlString.substring(endIdx, htmlString.length())); //the
                                                                             // last
                                                                             // element
                                                                             // will
                                                                             // be
                                                                             // the
                                                                             // footer
        }

        loggerDetails.debug("found rows = " + parsedRows.size());
        pr.setResultRows(parsedRows);

        //logger.debug("header = " + header);
        //logger.debug("footer = " + footer);
    }

    public void findSubResults(ParsedResponse pr, String htmlString,
            int pageId, String subresultStart, String subresultEnd,
            String linkStart, int action,String parserConfigurationFileName, String parserConfigurationFileNameOverWrite) throws ServerResponseException {
        loggerDetails.debug("findSubResults in parser ");
        loggerDetails.debug("htmlString=" + htmlString);

        Vector parsedRows = new Vector();

        int startIdx = htmlString.indexOf(subresultStart, 0);
      /*  if (startIdx == -1)
        {
            startIdx = htmlString.indexOf(subresultStart.toUpperCase(), 0);
            if (startIdx != -1)
                subresultStart = subresultStart.toUpperCase();
        }*/
        
        int endIdx = htmlString.indexOf(subresultEnd, startIdx + 1);
      /*  if (endIdx == -1)
        {
        	endIdx = htmlString.indexOf(subresultEnd.toUpperCase(), startIdx + 1);
        	if (endIdx != -1)
                subresultEnd = subresultEnd.toUpperCase();
        }*/
        while ((startIdx != -1) && (endIdx != -1)) {
            String row = htmlString.substring(startIdx, endIdx);
            ParsedResponse parsedR = parseOneRow(row, pageId, linkStart, action,parserConfigurationFileName, parserConfigurationFileNameOverWrite, pr.isParentSite());
            if (parsedR != null && parsedR.getPageLink()!=null)
            {                
                // marchez ca este o crossreferinta de tip "link" pt ca sa nu se valideze documentul in TSServer...
                parsedR.getPageLink().setOnlyLink(parsedR.getPageLink().getLink() + "&isSubResult=true");
                parsedR.getPageLink().setOnlyOriginalLink(parsedR.getPageLink().getOriginalLink() + "&isSubResult=true");
                
                parsedRows.add(parsedR);
            }               
            

            startIdx = htmlString.indexOf(subresultStart, endIdx);
            endIdx = htmlString.indexOf(subresultEnd, startIdx + 1);
        }

        loggerDetails.debug("found subresults = " + parsedRows.size());
        pr.setOnlyResultRows(parsedRows);
        //logger.debug("header = " + header);
        //logger.debug("footer = " + footer);
    }

    private ParsedResponse parseOneRow(String row, int pageId,
            String linkStart, int action,String parserConfigurationFileName, String parserConfigurationFileNameOverWrite, boolean isParentSite) throws ServerResponseException {
        loggerDetails.debug("parse one row " + row);
        ParsedResponse pr = new ParsedResponse();
        pr.setParentSite(isParentSite);
        this.Parse(pr, row, pageId, linkStart, action, parserConfigurationFileName, parserConfigurationFileNameOverWrite);
        return pr;
    }

    private void defaultParseRow(ParsedResponse pr, String sHtml,
            String linkStart, int action) {
        defaultParseRow(pr, sHtml, linkStart, action, false);
    }

    private void defaultParseRow(ParsedResponse pr, String sHtml,
            String linkStart, int action, boolean needDecoding) {
        loggerDetails.debug("defaultParseRow");
        pr.setPageLink(getLink(sHtml, linkStart, action, needDecoding));
    }

    private static LinkInPage getLink(String html, String sLinkStart,
            int action, boolean needDecoding) {
        loggerDetails.debug("sLinkStart = " + sLinkStart);
        loggerDetails.debug("html =" + html);
        LinkInPage link = null;

        int iStart = html.indexOf(sLinkStart);
        if (iStart > -1) {
            int idxGT = html.indexOf(">", iStart);
            if (idxGT != -1) {
                String linkCand = html.substring(iStart, idxGT);
                if(linkCand.contains("\"") && 
                		linkCand.contains("\'") && 
                		linkCand.contains(" "))
                	linkCand = linkCand.substring(0, linkCand.indexOf(" "));
                int idxDQuote = linkCand.indexOf("\"");
                int idxQuote = linkCand.indexOf("\'");

                int iEnd = (idxDQuote > idxQuote) ? idxDQuote : idxQuote;

                if (iEnd != -1) {
                    linkCand = linkCand.substring(0, iEnd);
                }

                linkCand = linkCand.replaceFirst("\\?", "&");

                String sLink = linkCand;
                String originalLink = sLink.substring(sLinkStart.length());

                if (needDecoding) {
                    try {
                        sLink = URLDecoder.decode(sLink, "UTF-8");
                        originalLink = URLDecoder.decode(originalLink, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                }
                link = new LinkInPage(sLink, originalLink, action);
            }
        }
        loggerDetails.debug("link=" + link);
        return link;
    }

    /**
     * @param miServerID
     * @param iHtmlID
     * @return
     */
    public DefaultTreeModel GetHtmlDescriptor2(int miServerID, int iHtmlID) {
        DefaultTreeModel result = null;
        File inFS;

        //get resource bundle and establsih rule source
        //ResourceBundle rbc = ResourceBundle.getBundle("conf.Parser");
        //int rSource = getInt(rbc, "Parser.Rules");
        int rSource;

        rSource = 1; //always from text files
        if (rSource == 1) { //from text file
            try {
                //logger.debug("Getting file..[" + GetFilePath2(miServerID, iHtmlID) + "]");
                inFS = new File(GetFilePath2(miServerID, iHtmlID));
                //logger.debug("Got file!");
                //logger.debug("reading rules...");

                RuleTreeManipulator rtm = new RuleTreeManipulator();
                rtm.setF(inFS);

                result = rtm.ReadFromFile();

                return result;

            } catch (Exception e) {
                //logger.debug("Html Descriptor not found : " + e);

                return null;
            }

        } else {

            //connect to database
            String stm;
            //build select String
            Object tree_id = null;
            //logger.debug("getting Blob ID");
            stm = "SELECT " + "TREE_RULES_ID" + " FROM " + " TS_EXTRULES "
                    + " WHERE " + "SERVER_ID=" + miServerID + " AND "
                    + "PAGE_ID=" + iHtmlID;

            DBConnection conn = null;
            try {

                conn = ConnectionPool.getInstance().requestConnection();
                //					get ID for model
                DatabaseData data = conn.executeSQL(stm);
                tree_id = data.getValue(1, 0);
                //logger.debug("done blob id " + tree_id);
                //					build select blob string
                
                PreparedStatement pStmt = null;
                try {
                	pStmt = conn.prepareStatement("SELECT DATA FROM TS_EXTRULES_ATTACH WHERE ID=?");
                	pStmt.setLong(1, Long.parseLong(tree_id.toString()));
                	ResultSet resultBlob = pStmt.executeQuery();
                    //stmt = conn.createStatement();
                    //ResultSet resultBlob = stmt.executeQuery("SELECT "
                    //       + " DATA " + " FROM " + " TS_EXTRULES_ATTACH "
                    //        + " WHERE " + "ID" + "=" + tree_id);
                    //get treemodel from blob
                    
                    if (resultBlob.next()) {
                        Blob blobtree = resultBlob.getBlob(1);
                        //logger.debug("Parser: read blob from db");
                        try {
                            InputStream IStream = ((com.mysql.jdbc.Blob) blobtree).getBinaryStream();
                            //logger.debug("Parser: IStream from blob");
                            ObjectInputStream Objin = new ObjectInputStream(IStream);
                            //logger.debug("Parser: ObjectStream from istream");
                            result = (DefaultTreeModel) Objin.readObject();
                            //logger.debug("Parser: treemodel extracted OK");
                            Objin.close();
                            IStream.close();
                            resultBlob.close();
                            pStmt.close();

                        } catch (Exception eio) {
                            //logger.debug("Parser: Exceptie la citire din Blob");
                        }
                    }

                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    //logger.debug("Parser: Exceptii la axtragere model");
                    e1.printStackTrace();
                }

            } catch (BaseException e) {
                // TODO Auto-generated catch block
                //logger.debug("Parser: Exceptie la extragere ID model");
                //e.printStackTrace();
            } finally {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }

            //return the model
            return result;

        }
    }

    /*
     * @param pr @throws IOException
     */
    /**
     * Method SnatdardizePIS.
     * 
     * @param pr
     */
    private void standardizePIS(ParsedResponse pr) {
        PropertyIdentificationSet pis = null;
        try {
            pis = pr.getPropertyIdentificationSet(0);
        } catch (Exception e) {
            //logger.debug("EXCETION !!!! Parser.standardizePIS \n" + e);
        }
        if (pis == null) {
            //logger.debug("Null Pis");
            return;
        }

        if (ro.cst.tsearch.search.name.NameUtils.isCompany(pis
                .getAtribute("OwnerLastName"))) {
            return;
        }
        parseOwnerNames(pis, new AssessorNameParser());

    }

    private void parseOwnerNames(PropertyIdentificationSet pis, NameParser np) {
        String owner = pis.getAtribute("OwnerLastName");
        //logger.debug("owner =" + owner);

        NameTokenList[] owners = np.parseName(owner);
        setName(pis, owners[0], "Owner");
        setName(pis, owners[1], "Spouse");
    }

    private void setName(PropertyIdentificationSet pis, NameTokenList ntl,
            String prefix) {
        pis.setAtribute(prefix + "LastName", TokenList.getString(ntl
                .getLastName()));
        pis.setAtribute(prefix + "FirstName", TokenList.getString(ntl
                .getFirstName()));
        pis.setAtribute(prefix + "MiddleName", TokenList.getString(ntl
                .getMiddleName()));
    }

    private String Parse(ParsedResponse pr, String sHtml, HtmlNode hn)
            throws IOException {

        //logger.info("Current node " + hn.getName() + " in position: " + hn.getPosition());
        if (hn.getChildrenCount() != 0) {
            int i;
            
            String sIterationRow;
            while (!(sIterationRow = AplyIteration(hn.getData().getIteration(),
                    sHtml)).equals("")) {
                String sCurrentSectionAndRestOfHtml = sIterationRow;
                String sCurrentSection = sIterationRow;
                if (hn.getData().getIteration() == null) {
                    //aply extraction
                    sCurrentSectionAndRestOfHtml = AplyExtraction(hn.getData()
                            .getStartExtraction(),
                            sCurrentSectionAndRestOfHtml, true);
                    sCurrentSection = AplyExtraction(hn.getData()
                            .getEndExtraction(), sCurrentSectionAndRestOfHtml,
                            false);
                } else
                    AddIterationObject(pr, hn);
                for (i = 0; i < hn.getChildrenCount(); i++) {
                    String sCurrentSectionWithoutFields = Parse(pr,
                            sCurrentSection, hn.findChild(i));
                    sCurrentSectionAndRestOfHtml = sCurrentSectionAndRestOfHtml
                            .substring(sCurrentSection.length()
                                    - sCurrentSectionWithoutFields.length());
                    sCurrentSection = sCurrentSectionWithoutFields;
                }
                if ((sHtml.length() - sCurrentSection.length()) == 0) //no
                                                                      // iteration
                                                                      // rule
                                                                      // and no
                                                                      // childrens
                    sHtml = "";
                else
                    sHtml = sHtml.substring(sHtml.length()
                            - sCurrentSectionAndRestOfHtml.length());
                if (hn.getData().getIteration() == null)
                    break;
            }
        } else {
            InfSet inf = getSet(pr, hn);
            sHtml = AplyExtraction(hn.getData().getStartExtraction(), sHtml,
                    true);
            String sField = AplyExtraction(hn.getData().getEndExtraction(),
                    sHtml, false);
            if (inf != null) {
                //logger.debug("key=[" + hn.getName() + "] value=[" + sField + "]");
                inf.setAtribute(hn.getName(), sField);
            }
            sHtml = sHtml.substring(sField.length());
        }
        return sHtml;
    }

    /**
     * Method AddIterationObject.
     * 
     * @param pr
     * @param hn
     */
    private void AddIterationObject(ParsedResponse pr, HtmlNode hn) {
        switch (hn.getInfSet()) {
        case SDS:
            pr.addSaleDataSet(new SaleDataSet());
        case PIS:
            pr.addPropertyIdentificationSet(new PropertyIdentificationSet());
        }
    }

    /**
     * Method getSet.
     * 
     * @param pr
     * @param hn
     * @return InfSet
     */
    private InfSet getSet(ParsedResponse pr, HtmlNode hn) {
        switch (hn.getInfSet()) {
        
        case PAS:
            return pr.getPropertyAppraisalSet();
       
        case PIS:
            if (pr.getPropertyIdentificationSetCount() == 0) {
                pr
                        .addPropertyIdentificationSet(new PropertyIdentificationSet());
                //logger.debug("Creating new PIS");
            }

            return pr.getPropertyIdentificationSet(pr
                    .getPropertyIdentificationSetCount() - 1);
        case SDS:
            if (pr.getSaleDataSetsCount() == 0) {
                pr.addSaleDataSet(new SaleDataSet());
                //logger.debug("Creating new SDS");
            }
            return pr.getSaleDataSet(pr.getSaleDataSetsCount() - 1);
        case THS:
        	if (pr.getTaxHistorySetsCount() == 0) {
                pr.addTaxHistorySet(new TaxHistorySet());
                //logger.debug("Creating new THS");
            }
            return pr.getTaxHistorySet(pr.getTaxHistorySetsCount() - 1);        
        
        case CDIS:
            if (pr.getCourtDocumentIdentificationSetCount()==0) {
                pr.addCourtDocumentIdentificationSet(new CourtDocumentIdentificationSet());
            }
            return pr.getCourtDocumentIdentificationSet(pr.getCourtDocumentIdentificationSetCount() - 1);
        }    
        return null;
        
    }

    private String AplyExtraction(Rule rule, String sHtml, boolean bGetStart) {
        //logger.debug("Aply Extraction on: " + sHtml);
        String rtrn = "";
        int iMark = 0;
        if (rule != null) {
            for (int i = 0; i < rule.getLandmarksCount(); i++) {
                if (rule.getLandmark(i).getLandMarkType() == Landmark.TYPE_SKIP_TO) {
                    //logger.debug("with:" + rule.getLandmark(i).getBody());
                    iMark = sHtml.indexOf(rule.getLandmark(i).getBody(), iMark);
                    if (iMark == -1) {
                        //logger.debug("Not found: ");
                        return "";
                    }
                    if (bGetStart)
                        iMark += rule.getLandmark(i).getBody().length();
                }

            }
        } else {
            if (!bGetStart)
                iMark = sHtml.length();
        }
        if (bGetStart) {
            //logger.debug("found at: " + iMark);
            rtrn = sHtml.substring(iMark);
        } else {
            rtrn = sHtml.substring(0, iMark);
            //logger.debug("extracted " + rtrn);
        }
        return rtrn;
    }

    /**
     * Method AplyIteration.
     * 
     * @param rule
     * @param sHtml
     * @return String
     */
    private String AplyIteration(Rule rule, String sHtml) {
        if (rule != null) {
            //logger.debug("Iterating");
            return AplyExtraction(rule, sHtml, true);
        } else {
            //logger.debug("one Iteration only or stop");
            return sHtml;
        }
    }

    /**
     * Method GetHtmlDescriptor.
     * 
     * @param miServerID
     * @param iHtmlID
     * @return HtmlDescriptor
     */
    private HtmlDescriptor GetHtmlDescriptor(int miServerID, int iHtmlID)
            throws IOException {
        //logger.debug("Geting Html Descriptor for server: " + miServerID + "and page: " + iHtmlID);
        InputStream inFS;
        try {
            inFS = new FileInputStream(GetFilePath(miServerID, iHtmlID));
        } catch (FileNotFoundException e) {
            //logger.debug("Html Descriptor not found : ");
            return null;
        }
        StreamTokenizer tokens = new StreamTokenizer(inFS);
        int tokenType = StreamTokenizer.TT_EOF;
        Rules rules = new Rules();
        rules.setIteration(null);
        rules.setStartExtraction(null);
        rules.setEndExtraction(null);
        HtmlDescriptor tree = new HtmlDescriptor(rules);
        ArrayList aiFullPath2Parent = new ArrayList();
        rules = new Rules();
        String sFieldName = null;
        int iInfSet = 0;
        int state = 1;
        int depth = 0;
        tokens.quoteChar('\t');
        tokens.eolIsSignificant(true);
        while ((tokenType = tokens.nextToken()) != StreamTokenizer.TT_EOF) {
            if (tokenType == StreamTokenizer.TT_EOL) {
                //logger.debug("Adding rule for : " + sFieldName);
                HtmlNode node = tree.addNode(aiFullPath2Parent, rules,
                        sFieldName, iInfSet);
                aiFullPath2Parent.add(new Integer(node.getPosition()));
                depth = 0;
                state = 1;
                iInfSet = 0;
                rules = new Rules();
            } else if (tokenType == StreamTokenizer.TT_NUMBER && state == 1) {
                while (depth < aiFullPath2Parent.size()) {
                    aiFullPath2Parent.remove(aiFullPath2Parent.size() - 1);
                }
                //logger.debug("Node parent: " + aiFullPath2Parent.toString());
                iInfSet = (int) tokens.nval;
                //logger.debug("inf set: " + iInfSet);
                state++;
            } else if (tokenType == 124 && state == 1) {
                //logger.debug("current node depth: " + depth);
                depth = depth + 1;
            } else if (tokenType == StreamTokenizer.TT_WORD && state == 2) {
                //logger.debug("current node name: " + sFieldName);
                sFieldName = tokens.sval;
                state++;
            } else if (tokenType == '\t') {
                //logger.debug("Adding rule: ");
                Rule rule;
                if (!tokens.sval.equals("")) {
                    rule = new Rule();
                    Landmark landmark = new Landmark();
                    //logger.debug("SkipTo: " + tokens.sval);
                    landmark.addToken(tokens.sval);
                    landmark.setLandMarkType(Landmark.TYPE_SKIP_TO);
                    rule.addLandmark(landmark);
                } else
                    rule = null;
                if (state == 3) {
                    //logger.debug("for iteration");
                    rules.setIteration(rule);
                    state++;
                } else if (state == 4) {
                    //logger.debug("for start extraction");
                    rules.setStartExtraction(rule);
                    state++;
                } else if (state == 5) {
                    //logger.debug("for end extraction");
                    rules.setEndExtraction(rule);
                    state++;
                }
            }
        }
        return tree;
    }

    /**
     * Method GetFilePath.
     * @param miServerID
     * @param iHtmlID
     */
    private String GetFilePath(int miServerID, int iHtmlID) {
        String rtrn = msRealPath + "Wrappers" + File.separator;
        switch (miServerID) {
        /*case TSServersFactory.idShelbyAO :
         rtrn += "ShelbyAO" + File.separator;
         switch (iHtmlID)
         {
         case PAGE_DETAILS :
         rtrn += "details.dsc";
         break;
         default :
         rtrn= null;
         }
         break;*/
        /*case TSServersFactory.idShelbyTR :
         rtrn += "ShelbyTR" + File.separator;
         switch (iHtmlID)
         {
         case PAGE_DETAILS :
         rtrn += "details.dsc";
         break;
         default :
         rtrn= null;
         }
         break;*/
        /*case TSServersFactory.idShelbyRO  :
         rtrn += "ShelbyRO" + File.separator;
         switch (iHtmlID)
         {
         case PAGE_DETAILS :
         rtrn += "details.dsc";
         break;
         default :
         rtrn= null;
         }
         break;*/
        default:
            if (TSServersFactory.getSiteId("TN", "Shelby", "AO") == miServerID) {
                rtrn += "ShelbyAO" + File.separator;
                switch (iHtmlID) {
                case PAGE_DETAILS:
                    rtrn += "details.dsc";
                    break;
                default:
                    rtrn = null;
                }
            } else if ((int) TSServersFactory.getSiteId("TN", "Shelby", "RO") == miServerID) {
                rtrn += "ShelbyRO" + File.separator;
                switch (iHtmlID) {
                case PAGE_DETAILS:
                    rtrn += "details.dsc";
                    break;
                default:
                    rtrn = null;
                }
            } else if ((int) TSServersFactory.getSiteId("TN", "Shelby", "TR") == miServerID) {
                rtrn += "ShelbyTR" + File.separator;
                switch (iHtmlID) {
                case PAGE_DETAILS:
                    rtrn += "details.dsc";
                    break;
                default:
                    rtrn = null;
                }
            } else
                rtrn = null;
        }
        //logger.debug("File Path:" + rtrn);
        return rtrn;
    }

    private String GetFilePath2(int miServerID, int iHtmlID) {
        String rtrn = msRealPath + "Wrappers" + File.separator;
    
        if (TSServersFactory.getSiteId("TN", "Shelby", "AO") == miServerID
                || TSServersFactory.getSiteId("TN", "Shelby", "RO") == miServerID
                || TSServersFactory.getSiteId("TN", "Shelby", "TR") == miServerID
                || TSServersFactory.getSiteId("TN", "Williamson", "AO") == miServerID) {
            rtrn += TSServersFactory.getSiteName(miServerID).substring(2)
                    + File.separator;
            switch (iHtmlID) {
            case PAGE_DETAILS:
                rtrn += TSServersFactory.getSiteName(miServerID).substring(2);
                break;
            default:
                rtrn = null;
            }
        } else{
            rtrn = null;
        }
        //}

        String suffix = new String();

        //		if(use_corrector==1) suffix=new String(".crt");
        //		else
        suffix = new String(".txt");

        rtrn += suffix;
        //logger.debug("File Path:" + rtrn);
        return rtrn;
    }

    //	///////////////////////////////////////////////

    public static String getFirstName(String link) {
        return getParamValue(link, "firstname");
    }

    public static String getLastName(String link) {
        return getParamValue(link, "lastname");
    }

    static String getParamValue(String link, String param) {
        String rez = "";
        int idx1 = link.indexOf(param);
        if (idx1 > -1) {
            idx1 = idx1 + param.length() + ("=".length());
            int idx2 = link.indexOf("&", idx1);
            if (idx2 > -1)
                rez = link.substring(idx1, idx2);
        }
        
        try
        {
            rez = URLDecoder.decode( rez, "UTF-8" );
        }catch( Exception e ) {}
        
        return rez;
    }

	public TSServer getTsserver() {
		return tsserver;
	}

	public void setTsserver(TSServer tsserver) {
		this.tsserver = tsserver;
	}
    
    

}
