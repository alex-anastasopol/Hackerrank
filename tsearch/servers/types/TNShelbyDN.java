package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.CookieManager;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parentsitedescribe.ServerInfoDSMMap;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

/**
 * @author costin
	 */
public class TNShelbyDN extends TSServer {
	
	protected static final Category logger= Category.getInstance(TNShelbyDN.class.getName());
	
	static final long serialVersionUID = 10000000;
	private boolean downloadingForSave = false;
    
	/* 
	 * This version takes an additional parameter "companyNameParam"
	 * in order to suit the needs of DN site. Also added the following fields in the
	 * SearchAttributes:  OWNER_LNAME_DN, OWNER_FNAME_DN, OWNER_CNAME_DN
     */
/*    protected  TSServerInfoModule SetModuleSearchByName(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String lNameParam,
            String fNameParam, String companyNameParam) {
            
        TSServerInfoModule simTmp;
       
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Name"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_NAME);
        
        if( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isOwnerIndividual() ){
            simTmp.getFunction(0).setName("Last Name:"); //it will be displayed in jsp
            simTmp.getFunction(0).setParamName(lNameParam);
            simTmp.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            simTmp.getFunction(0).setSaKey(SearchAttributes.OWNER_LNAME);
            //FirstName
            simTmp.getFunction(1).setName("First Name:"); //it will be displayed in jsp
            simTmp.getFunction(1).setParamName(fNameParam);
            simTmp.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
            simTmp.getFunction(1).setSaKey(SearchAttributes.OWNER_FNAME);
            //CompanyName
            simTmp.getFunction(2).setName("Company Name:"); //it will be displayed in jsp
            simTmp.getFunction(2).setParamName(fNameParam);
            simTmp.getFunction(2).setDefaultValue("");
        }else{
        	//LastName
            simTmp.getFunction(0).setName("Last Name:"); //it will be displayed in jsp
            simTmp.getFunction(0).setParamName(lNameParam);
            simTmp.getFunction(0).setDefaultValue("");
            //FirstName
            simTmp.getFunction(1).setName("First Name:"); //it will be displayed in jsp
            simTmp.getFunction(1).setParamName(fNameParam);
            simTmp.getFunction(0).setDefaultValue("");
            //CompanyName
            simTmp.getFunction(2).setName("Company Name:"); //it will be displayed in jsp
            simTmp.getFunction(2).setParamName(fNameParam);
            simTmp.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            simTmp.getFunction(2).setSaKey(SearchAttributes.OWNER_LNAME);        	
        }
        simTmp.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        
        return simTmp;
    }
   */ 
	/**
	 * These parameters represent doctype filters for dn site
	 * If a certain doctype is needed, the query must contain "param=on" 
	 * !!! Be careful that the fields __EVENTVALIDATION and __VIEWSTATE will also need to be changed
	 * - use HTTPAnalyzer for this !!!
	 */
	public static String docTypeFilterParams[]= { 
		"ctl00$ContentPane$ctl25", 
		"ctl00$ContentPane$ctl41", 
		"ctl00$ContentPane$ctl42", 
		"ctl00$ContentPane$ctl43", 
		"ctl00$ContentPane$ctl48", 
		"ctl00$ContentPane$ctl49", 
		"ctl00$ContentPane$ctl50", 
		"ctl00$ContentPane$ctl51", 
		"ctl00$ContentPane$ctl53", 
		"ctl00$ContentPane$ctl55", 
		"ctl00$ContentPane$ctl57", 
		"ctl00$ContentPane$ctl58", 
		"ctl00$ContentPane$ctl60", 
		"ctl00$ContentPane$ctl64",
		"ctl00$ContentPane$ctl65"
	};
	
	/* 
	 * These fields are captured with HTTPAnalyzer from the dn site. They encode the selected doctypes
	 * If you change the doctype selectio then you also have to uodate these values 
	 */
	public static String EVENTVALIDATION_NAME_VALUE = "/wEWXwKJpb4RAseNuaoEAob+uYwCAsG63ekPAqiNopEHAtyku4cIAr7Lse8KAsS3/JcKAuXriYIJAqXSypQFApXX8a8FAqjKmcwJAv6Dia0EAqi5n4AJAsOivZUDAuz9/LYNAsPqtaQHAvTnxjoC7P/M8AsCr8e06gsCyrDS/wUC5ZnwFAKAg46qCgKb7Ku/BAKouZuACQLDormVAwLei9eqDQLHzvijAwL59PS/BwKU3pLVAQKvx7DqCwLKsM7/BQLlmewUAoCDiqoKApvsp78EAqi5p4AJAoW12IgPAsOixZUDAt6L46oNAvn0gMAHApTentUBApjq/7EBAq/HvOoLAsqw2v8FAuWZ+BQCgIOWqgoCm+yzvwQCqLmjgAkCw6LBlQMC3ovfqg0C+fT8vwcClN6a1QECr8e46gsCyrDW/wUC5Zn0FAKAg5KqCgKb7K+/BAKoua+ACQLDos2VAwKdls6bAQLei+uqDQL59IjABwKU3qbVAQKvx8TqCwLKsOL/BQLlmYAVAoCDnqoKApvsu78EAqi5q4AJAsOiyZUDAt6L56oNAvn0hMAHApTeotUBAq/HwOoLAsqw3v8FAuWZ/BQCgIOaqgoC5Jr8/goCm+y3vwQCqLm3gAkCw6LVlQMC3ovzqg0C+fSQwAcClN6u1QECr8fM6gsC2o3XyAQCpfXNBAKjqMLDCgLsueqKDgL+86OnAQL+87+nAQL+87unAQL+89emAQLVjM5aArq+0uIP7/SBJ/AXq6CRQgifR1ZFbPCvQdw=";
	public static String VIEWSTATE_NAME_VALUE = "/wEPDwUJMzAyNTcwNTU0D2QWAmYPZBYCAgMPZBYKAgEPDxYCHgRUZXh0BSRMb2dnZWQgaW4gYXM6IGhsb3dyYW5jZSZuYnNwO3wmbmJzcDtkZAIDDw8WBB8ABQZMb2dvdXQeB1Zpc2libGVnZGQCBQ9kFhACAQ8PFgIfAAUVT24gUHJlc3NpbmcgdGhlIEZsZXNoZGQCAw8PFgIeCEltYWdlVXJsBSFFZGl0b3JpYWxfSW1hZ2VzLzExMjcwNl9sZWFkMi5naWZkZAIFDw8WAh8ABRFOb3ZlbWJlciAyNywgMjAwNmRkAgcPDxYCHwAFmgE8cD5SZWRhIE1hbnNvdXIsIHRoZSA0MS15ZWFyLW9sZCBJc3JhZWxpIGRpcGxvbWF0IHdobyB3YXMgYXBwb2ludGVkIGVhcmxpZXIgdGhpcyBmYWxsIGFzIGNvbnN1bCBnZW5lcmFsIG9mIElzcmFlbCB0byB0aGUgc291dGhlYXN0ZXJuIFVuaXRlZCBTdGF0ZXMsIGkgLi4uZGQCCQ8PFgQfAAUHY29udCA+Ph4LTmF2aWdhdGVVcmwFIUVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9pZD05NTIxNmRkAg0PDxYEHwAFB2NvbnQgPj4fAwU5RWRpdG9yaWFsL1N0b3J5TGVhZC5hc3B4P3N0b3J5PWRpZ2VzdCZkYXRlPTExJTJmMjQlMmYyMDA2ZGQCDw8PFgIfAAU7VmVpblZpZXdlciBEZXZpY2UgR2V0cyBXYXJtIFJlY2VwdGlvbiBpbiBNZWRpY2FsIENvbW11bml0eSBkZAIRDw8WBB8ABQdjb250ID4+HwMFIUVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9pZD05NTIxN2RkAgcPZBYGAgEPZBYOAgEPDxYCHwAFAjEwZGQCAw8PFgYfAAULTmFtZSBTZWFyY2geCUZvbnRfQm9sZGceBF8hU0ICgBBkZAIFDw8WBh8ABQ9Qcm9wZXJ0eSBTZWFyY2gfBGgfBQKAEGRkAgkPDxYCHwFnZGQCCw8PFgIfAWhkZAIRD2QWBGYPEA8WBh8ABQZEZXNvdG8eB0NoZWNrZWRoHgxBdXRvUG9zdEJhY2tnZGRkZAIBDxAPFgYfAAUGU2hlbGJ5HwZnHwdnZGRkZAIZDw8WAh8BZ2QWAgIBD2QWBAIBD2QWBmYPZBYCZg9kFgJmD2QWAmYPZBYCZg8QDxYCHwZnZGRkZAIBD2QWAmYPZBYCZg9kFgJmD2QWAmYPEA8WAh8GZ2RkZGQCAg9kFgJmD2QWAmYPZBYCZg9kFgJmDxAPFgIfBmdkZGRkAgIPZBYGZg9kFgJmD2QWAmYPZBYCZg9kFgJmDxAPFgIfBmdkZGRkAgEPZBYCZg9kFgJmD2QWAmYPZBYCZg8QDxYCHwZnZGRkZAICD2QWAmYPZBYCZg9kFgJmD2QWAmYPEA8WAh8GZ2RkZGQCAw8PFgIfAWhkFgICBw8PFgIfAAUCMTBkZAIFDw8WAh8BaGRkAgkPZBYEAgEPDxYCHwFoZBYCAgsPDxYCHwAFAjEwZGQCAw9kFggCCQ8PFgIfAAUHKCQyLjk1KWRkAg0PDxYCHwAFBygkNi45NSlkZAIRDw8WAh8ABQcoJDYuOTUpZGQCFQ8PFgIfAAUHKCQ5Ljk1KWRkGAEFHl9fQ29udHJvbHNSZXF1aXJlUG9zdEJhY2tLZXlfXxZQBRtjdGwwMCRDb250ZW50UGFuZSRjYlByaW1hcnkFG2N0bDAwJENvbnRlbnRQYW5lJGNiVHJ1c3RlZQUcY3RsMDAkQ29udGVudFBhbmUkY2JBdHRvcm5leQUZY3RsMDAkQ29udGVudFBhbmUkY2JKdWRnZQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDAxBS9jdGwwMCRDb250ZW50UGFuZSRjYlJlYWxFc3RhdGVEZXZlbG9wbWVudERlc290bwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDA2BRdjdGwwMCRDb250ZW50UGFuZSRjdGwwNwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDgFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDA5BRdjdGwwMCRDb250ZW50UGFuZSRjdGwxMAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTEFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDEyBSdjdGwwMCRDb250ZW50UGFuZSRjYkxhd0dvdmVybm1lbnREZXNvdG8FF2N0bDAwJENvbnRlbnRQYW5lJGN0bDEzBRdjdGwwMCRDb250ZW50UGFuZSRjdGwxNAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDE2BRdjdGwwMCRDb250ZW50UGFuZSRjdGwxNwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTgFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDE5BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyMAUoY3RsMDAkQ29udGVudFBhbmUkY2JCdXNpbmVzc1Blb3BsZURlc290bwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjEFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDIyBRdjdGwwMCRDb250ZW50UGFuZSRjdGwyMwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjQFL2N0bDAwJENvbnRlbnRQYW5lJGNiUmVhbEVzdGF0ZURldmVsb3BtZW50U2hlbGJ5BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyNQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDI3BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyOAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDMwBRdjdGwwMCRDb250ZW50UGFuZSRjdGwzMQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzIFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDMzBRdjdGwwMCRDb250ZW50UGFuZSRjdGwzNAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM2BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzNwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzgFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM5BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0MAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDEFJ2N0bDAwJENvbnRlbnRQYW5lJGNiTGF3R292ZXJubWVudFNoZWxieQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDIFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQzBRdjdGwwMCRDb250ZW50UGFuZSRjdGw0NAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ2BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0NwUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDgFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ5BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1MAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTEFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDUyBRdjdGwwMCRDb250ZW50UGFuZSRjdGw1MwUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTQFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU1BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1NgUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTcFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU4BShjdGwwMCRDb250ZW50UGFuZSRjYkJ1c2luZXNzUGVvcGxlU2hlbGJ5BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1OQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDYxBRdjdGwwMCRDb250ZW50UGFuZSRjdGw2MgUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjMFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDY0BRdjdGwwMCRDb250ZW50UGFuZSRjdGw2NQUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMQUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMQUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMgUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMgUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMwUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwMwUWY3RsMDAkUmlnaHRQYW5lJHJiUmVwNIopOQItF/0mKRUB7eSTA/jTa42Q";
	
	public static String EVENTVALIDATION_ADDRESS_VALUE = "/wEWXQKOw+rkCwLHjbmqBAKG/rmMAgLBut3pDwLujK3wDwKLwquUDQKwl/GrDALWm9rmDQK1vLSxDQKoypnMCQL+g4mtBAKouZ+ACQLDor2VAwLs/fy2DQLD6rWkBwL058Y6Auz/zPALAq/HtOoLAsqw0v8FAuWZ8BQCgIOOqgoCm+yrvwQCqLmbgAkCw6K5lQMC3ovXqg0Cx874owMC+fT0vwcClN6S1QECr8ew6gsCyrDO/wUC5ZnsFAKAg4qqCgKb7Ke/BAKouaeACQKFtdiIDwLDosWVAwLei+OqDQL59IDABwKU3p7VAQKY6v+xAQKvx7zqCwLKsNr/BQLlmfgUAoCDlqoKApvss78EAqi5o4AJAsOiwZUDAt6L36oNAvn0/L8HApTemtUBAq/HuOoLAsqw1v8FAuWZ9BQCgIOSqgoCm+yvvwQCqLmvgAkCw6LNlQMCnZbOmwEC3ovrqg0C+fSIwAcClN6m1QECr8fE6gsCyrDi/wUC5ZmAFQKAg56qCgKb7Lu/BAKouauACQLDosmVAwLei+eqDQL59ITABwKU3qLVAQKvx8DqCwLKsN7/BQLlmfwUAoCDmqoKAuSa/P4KApvst78EAqi5t4AJAsOi1ZUDAt6L86oNAvn0kMAHApTertUBAq/HzOoLAtqN18gEAqX1zQQCo6jCwwoC7Lnqig4C/vOjpwEC/vO/pwEC/vO7pwEC/vPXpgEC1YzOWgK6vtLiD6xviOV57KMie8qjoPuAi35GOiqQ";
	public static String VIEWSTATE_ADDRESS_VALUE = "/wEPDwUJMzAyNTcwNTU0D2QWAmYPZBYCAgMPZBYKAgEPDxYCHgRUZXh0BSRMb2dnZWQgaW4gYXM6IGhsb3dyYW5jZSZuYnNwO3wmbmJzcDtkZAIDDw8WBB8ABQZMb2dvdXQeB1Zpc2libGVnZGQCBQ9kFhACAQ8PFgIfAAUVT24gUHJlc3NpbmcgdGhlIEZsZXNoZGQCAw8PFgIeCEltYWdlVXJsBSFFZGl0b3JpYWxfSW1hZ2VzLzExMjcwNl9sZWFkMi5naWZkZAIFDw8WAh8ABRFOb3ZlbWJlciAyNywgMjAwNmRkAgcPDxYCHwAFmgE8cD5SZWRhIE1hbnNvdXIsIHRoZSA0MS15ZWFyLW9sZCBJc3JhZWxpIGRpcGxvbWF0IHdobyB3YXMgYXBwb2ludGVkIGVhcmxpZXIgdGhpcyBmYWxsIGFzIGNvbnN1bCBnZW5lcmFsIG9mIElzcmFlbCB0byB0aGUgc291dGhlYXN0ZXJuIFVuaXRlZCBTdGF0ZXMsIGkgLi4uZGQCCQ8PFgQfAAUHY29udCA+Ph4LTmF2aWdhdGVVcmwFIUVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9pZD05NTIxNmRkAg0PDxYEHwAFB2NvbnQgPj4fAwU5RWRpdG9yaWFsL1N0b3J5TGVhZC5hc3B4P3N0b3J5PWRpZ2VzdCZkYXRlPTExJTJmMjQlMmYyMDA2ZGQCDw8PFgIfAAU7VmVpblZpZXdlciBEZXZpY2UgR2V0cyBXYXJtIFJlY2VwdGlvbiBpbiBNZWRpY2FsIENvbW11bml0eSBkZAIRDw8WBB8ABQdjb250ID4+HwMFIUVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9pZD05NTIxN2RkAgcPZBYGAgEPZBYOAgEPDxYCHwAFAjEwZGQCAw8PFgYfAAULTmFtZSBTZWFyY2geCUZvbnRfQm9sZGgeBF8hU0ICgBBkZAIFDw8WBh8ABQ9Qcm9wZXJ0eSBTZWFyY2gfBGcfBQKAEGRkAgkPDxYCHwFoZBYCAgcPZBYIAgEPEA8WAh4HQ2hlY2tlZGdkZGRkAgIPEA8WAh8GaGRkZGQCAw8QDxYCHwZoZGRkZAIEDxAPFgIfBmhkZGRkAgsPDxYCHwFnZGQCEQ9kFgRmDxAPFgYfAAUGRGVzb3RvHgxBdXRvUG9zdEJhY2tnHwZoZGRkZAIBDxAPFgYfAAUGU2hlbGJ5HwZnHwdnZGRkZAIZDw8WAh8BZ2QWAgIBD2QWBAIBD2QWBmYPZBYCZg9kFgJmD2QWAmYPZBYCZg8QDxYCHwZnZGRkZAIBD2QWAmYPZBYCZg9kFgJmD2QWAmYPEA8WAh8GZ2RkZGQCAg9kFgJmD2QWAmYPZBYCZg9kFgJmDxAPFgIfBmdkZGRkAgIPZBYGZg9kFgJmD2QWAmYPZBYCZg9kFgJmDxAPFgIfBmdkZGRkAgEPZBYCZg9kFgJmD2QWAmYPZBYCZg8QDxYCHwZnZGRkZAICD2QWAmYPZBYCZg9kFgJmD2QWAmYPEA8WAh8GZ2RkZGQCAw8PFgIfAWhkFgICBw8PFgIfAAUCMTBkZAIFDw8WAh8BaGRkAgkPZBYEAgEPDxYCHwFoZBYCAgsPDxYCHwAFAjEwZGQCAw9kFggCCQ8PFgIfAAUHKCQxLjk1KWRkAg0PDxYCHwAFBygkNC45NSlkZAIRDw8WAh8ABQcoJDUuOTUpZGQCFQ8PFgIfAAUHKCQ3Ljk1KWRkGAEFHl9fQ29udHJvbHNSZXF1aXJlUG9zdEJhY2tLZXlfXxZMBRdjdGwwMCRDb250ZW50UGFuZSRjdGwwMAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDEFL2N0bDAwJENvbnRlbnRQYW5lJGNiUmVhbEVzdGF0ZURldmVsb3BtZW50RGVzb3RvBRdjdGwwMCRDb250ZW50UGFuZSRjdGwwNQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDA3BRdjdGwwMCRDb250ZW50UGFuZSRjdGwwOAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDEwBRdjdGwwMCRDb250ZW50UGFuZSRjdGwxMQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTIFJ2N0bDAwJENvbnRlbnRQYW5lJGNiTGF3R292ZXJubWVudERlc290bwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTMFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDE0BRdjdGwwMCRDb250ZW50UGFuZSRjdGwxNQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDE3BRdjdGwwMCRDb250ZW50UGFuZSRjdGwxOAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMTkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDIwBShjdGwwMCRDb250ZW50UGFuZSRjYkJ1c2luZXNzUGVvcGxlRGVzb3RvBRdjdGwwMCRDb250ZW50UGFuZSRjdGwyMQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjIFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDIzBRdjdGwwMCRDb250ZW50UGFuZSRjdGwyNAUvY3RsMDAkQ29udGVudFBhbmUkY2JSZWFsRXN0YXRlRGV2ZWxvcG1lbnRTaGVsYnkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDI1BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyNgUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjcFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDI4BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyOQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDMxBRdjdGwwMCRDb250ZW50UGFuZSRjdGwzMgUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzMFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM0BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzNQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM3BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzOAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQwBRdjdGwwMCRDb250ZW50UGFuZSRjdGw0MQUnY3RsMDAkQ29udGVudFBhbmUkY2JMYXdHb3Zlcm5tZW50U2hlbGJ5BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0MgUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDMFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ0BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0NQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ3BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0OAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDUwBRdjdGwwMCRDb250ZW50UGFuZSRjdGw1MQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTIFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDUzBRdjdGwwMCRDb250ZW50UGFuZSRjdGw1NAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU2BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1NwUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTgFKGN0bDAwJENvbnRlbnRQYW5lJGNiQnVzaW5lc3NQZW9wbGVTaGVsYnkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU5BRdjdGwwMCRDb250ZW50UGFuZSRjdGw2MAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjEFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDYyBRdjdGwwMCRDb250ZW50UGFuZSRjdGw2MwUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjQFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDY1BRZjdGwwMCRSaWdodFBhbmUkcmJSZXAxBRZjdGwwMCRSaWdodFBhbmUkcmJSZXAxBRZjdGwwMCRSaWdodFBhbmUkcmJSZXAyBRZjdGwwMCRSaWdodFBhbmUkcmJSZXAyBRZjdGwwMCRSaWdodFBhbmUkcmJSZXAzBRZjdGwwMCRSaWdodFBhbmUkcmJSZXAzBRZjdGwwMCRSaWdodFBhbmUkcmJSZXA08Tig2eW+DkunovvHv+AQw8yGAoM=";
	
	public TSServerInfo getDefaultServerInfo() {
		        
        TSServerInfo msiServerInfoDefault = null;

		TSServerInfoModule simTmp = null;
		
		int crtFunctionNo;
		
		if (msiServerInfoDefault == null) {
			//SET SERVER
			//number of search modules
			msiServerInfoDefault = new TSServerInfo(2);
			//set Address
			msiServerInfoDefault.setServerAddress("www.memphisdailynews.com");
			//set link
			msiServerInfoDefault.setServerLink("http://www.memphisdailynews.com/");
			//set IP
			msiServerInfoDefault.setServerIP("66.194.101.16");
			{ //SET EACH SEARCH
/*
__EVENTTARGET=
__EVENTARGUMENT=
__LASTFOCUS=
__VIEWSTATE=%2FwEPDwULLTE2MTQzNTk5NzcPZBYCZg9kFgICAw9kFgoCAQ8PFgIeBFRleHQFJExvZ2dlZCBpbiBhczogaGxvd3JhbmNlJm5ic3A7fCZuYnNwO2RkAgMPDxYEHwAFBkxvZ291dB4HVmlzaWJsZWdkZAIFD2QWEAIBDw8WAh8ABUFNZW1waGlzLUJhc2VkIFRDQiBFbnRlcnRhaW5tZW50IEJyaW5ncyBNb3JlIENvbmNlcnRzIHRvIE1pZC1Tb3V0aGRkAgMPDxYCHghJbWFnZVVybAUsRWRpdG9yaWFsX0ltYWdlcy8wODIwMDdfdGhpcmRfc3Rvcnlfb3B0MS5naWZkZAIFDw8WAh8ABQ9BdWd1c3QgMjAsIDIwMDdkZAIHDw8WAh8ABZoBPHA%2BTWVtcGhpYW5zIG1pZ2h0IHRoaW5rIG9mIEVsdmlzIHdoZW4gdGhleSBzZWUgdGhlIGxldHRlcnMgIlRDQi4iIEFmdGVyIGFsbCwgIlRha2luZyBDYXJlIG9mIEJ1c2luZXNzIChpbiBhIEZsYXNoKSIgd2FzIHRoZSBLaW5nJ3MgdHJhZGVtYXJrIHBocmFzZSwgIC4uLmRkAgkPDxYEHwAFB2NvbnQgPj4eC05hdmlnYXRlVXJsBSFFZGl0b3JpYWwvU3RvcnlMZWFkLmFzcHg%2FaWQ9OTg1NDdkZAINDw8WBB8ABQdjb250ID4%2BHwMFOEVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9zdG9yeT1kaWdlc3QmZGF0ZT04JTJmMjAlMmYyMDA3ZGQCDw8PFgIfAAU1TmV3IFNsYXRlIG9mIFB1YmxpYyBPZmZpY2lhbHMgRmFjZXMgVW5wb3B1bGFyIENob2ljZSBkZAIRDw8WBB8ABQdjb250ID4%2BHwMFIUVkaXRvcmlhbC9TdG9yeUxlYWQuYXNweD9pZD05ODU0OGRkAgcPZBYGAgEPZBYOAgEPDxYCHwAFAjEzZGQCBQ8PFgYfAAULTmFtZSBTZWFyY2geCUZvbnRfQm9sZGceBF8hU0ICgBBkZAIHDw8WBh8ABQ9Qcm9wZXJ0eSBTZWFyY2gfBGgfBQKAEGRkAgkPDxYCHwAFKllvdSBtdXN0IHNwZWNpZnkgYXQgbGVhc3QgYSBmaXJzdCBpbml0aWFsLmRkAg0PDxYCHwFoZGQCEw9kFgRmDxAPFgYfAAUGRGVzb3RvHgxBdXRvUG9zdEJhY2tnHgdDaGVja2VkaGRkZGQCAQ8QDxYGHwAFBlNoZWxieR8HZx8GZ2RkZGQCHQ8PFgIfAWdkFgICAQ9kFgQCAQ8PFgIfAWhkFgZmD2QWAmYPZBYSZg9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCAQ9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCAg9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCAw9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCBA9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCBQ9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCBg9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCBw9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCCA9kFgJmD2QWAmYPEA8WAh8HaGRkZGQCAQ9kFgJmD2QWEmYPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgEPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgIPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgMPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgQPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgUPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgYPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgcPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAggPZBYCZg9kFgJmDxAPFgIfB2hkZGRkAgIPZBYCZg9kFgpmD2QWAmYPZBYCZg8QDxYCHwdoZGRkZAIBD2QWAmYPZBYCZg8QDxYCHwdoZGRkZAICD2QWAmYPZBYCZg8QDxYCHwdoZGRkZAIDD2QWAmYPZBYCZg8QDxYCHwdoZGRkZAIED2QWAmYPZBYCZg8QDxYCHwdoZGRkZAICD2QWBmYPZBYCZg9kFgJmD2QWAmYPZBYCZg8QDxYCHwdnZGRkZAIBD2QWAmYPZBYCZg9kFgJmD2QWAmYPEA8WAh8HZ2RkZGQCAg9kFgJmD2QWAmYPZBYCZg9kFgJmDxAPFgIfB2dkZGRkAgMPDxYCHwFoZBYCAgcPDxYCHwAFAjEzZGQCBQ8PFgIfAWhkZAIJD2QWBAIBDw8WAh8BaGQWAgILDw8WAh8ABQIxM2RkAgMPZBYCAgcPZBYIAgMPDxYCHwAFBihGUkVFKWRkAgcPDxYCHwAFBihGUkVFKWRkAgsPDxYCHwAFBihGUkVFKWRkAg8PDxYCHwAFBihGUkVFKWRkGAEFHl9fQ29udHJvbHNSZXF1aXJlUG9zdEJhY2tLZXlfXxY0BRtjdGwwMCRDb250ZW50UGFuZSRjYlByaW1hcnkFG2N0bDAwJENvbnRlbnRQYW5lJGNiVHJ1c3RlZQUcY3RsMDAkQ29udGVudFBhbmUkY2JBdHRvcm5leQUZY3RsMDAkQ29udGVudFBhbmUkY2JKdWRnZQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMDAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDAxBS9jdGwwMCRDb250ZW50UGFuZSRjYlJlYWxFc3RhdGVEZXZlbG9wbWVudFNoZWxieQUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDI2BRdjdGwwMCRDb250ZW50UGFuZSRjdGwyNwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMjgFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDI5BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzMAUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzEFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDMyBRdjdGwwMCRDb250ZW50UGFuZSRjdGwzMwUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzQFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM1BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzNgUXY3RsMDAkQ29udGVudFBhbmUkY3RsMzcFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDM4BRdjdGwwMCRDb250ZW50UGFuZSRjdGwzOQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQxBSdjdGwwMCRDb250ZW50UGFuZSRjYkxhd0dvdmVybm1lbnRTaGVsYnkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQyBRdjdGwwMCRDb250ZW50UGFuZSRjdGw0MwUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDQFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ1BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0NgUXY3RsMDAkQ29udGVudFBhbmUkY3RsNDcFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDQ4BRdjdGwwMCRDb250ZW50UGFuZSRjdGw0OQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTAFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDUxBRdjdGwwMCRDb250ZW50UGFuZSRjdGw1MgUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTMFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU0BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1NQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTYFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDU3BRdjdGwwMCRDb250ZW50UGFuZSRjdGw1OAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNTkFKGN0bDAwJENvbnRlbnRQYW5lJGNiQnVzaW5lc3NQZW9wbGVTaGVsYnkFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDYwBRdjdGwwMCRDb250ZW50UGFuZSRjdGw2MQUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjIFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDYzBRdjdGwwMCRDb250ZW50UGFuZSRjdGw2NAUXY3RsMDAkQ29udGVudFBhbmUkY3RsNjUFF2N0bDAwJENvbnRlbnRQYW5lJGN0bDY2BRdjdGwwMCRDb250ZW50UGFuZSRjdGw2N1QsJ7sSUJNNm5B7rmsdU%2Bdo3vvX
ctl00%24ContentPane%24tbFirstName=j
ctl00%24ContentPane%24tbLastName=lowrance
ctl00%24ContentPane%24tbCompany=
ctl00%24ContentPane%24cbPrimary=on
ctl00%24ContentPane%24tbFrom=
ctl00%24ContentPane%24tbTo=
ctl00%24ContentPane%24ctl01=on
ctl00%24ContentPane%24btnSearch=Search
ctl00%24ContentPane%24cbRealEstateDevelopmentShelby=on
ctl00%24ContentPane%24ctl25=on
ctl00%24ContentPane%24ctl26=on
ctl00%24ContentPane%24ctl27=on
ctl00%24ContentPane%24ctl28=on
ctl00%24ContentPane%24ctl29=on
ctl00%24ContentPane%24ctl30=on
ctl00%24ContentPane%24ctl31=on
ctl00%24ContentPane%24ctl32=on
ctl00%24ContentPane%24ctl33=on
ctl00%24ContentPane%24ctl34=on
ctl00%24ContentPane%24ctl35=on
ctl00%24ContentPane%24ctl36=on
ctl00%24ContentPane%24ctl37=on
ctl00%24ContentPane%24ctl38=on
ctl00%24ContentPane%24ctl39=on
ctl00%24ContentPane%24ctl40=on
ctl00%24ContentPane%24ctl41=on
ctl00%24ContentPane%24cbLawGovernmentShelby=on
ctl00%24ContentPane%24ctl42=on
ctl00%24ContentPane%24ctl43=on
ctl00%24ContentPane%24ctl44=on
ctl00%24ContentPane%24ctl45=on
ctl00%24ContentPane%24ctl46=on
ctl00%24ContentPane%24ctl47=on
ctl00%24ContentPane%24ctl48=on
ctl00%24ContentPane%24ctl49=on
ctl00%24ContentPane%24ctl50=on
ctl00%24ContentPane%24ctl51=on
ctl00%24ContentPane%24ctl52=on
ctl00%24ContentPane%24ctl53=on
ctl00%24ContentPane%24ctl54=on
ctl00%24ContentPane%24ctl55=on
ctl00%24ContentPane%24ctl56=on
ctl00%24ContentPane%24ctl57=on
ctl00%24ContentPane%24ctl58=on
ctl00%24ContentPane%24ctl59=on
ctl00%24ContentPane%24cbBusinessPeopleShelby=on
ctl00%24ContentPane%24ctl60=on
ctl00%24ContentPane%24ctl61=on
ctl00%24ContentPane%24ctl62=on
ctl00%24ContentPane%24ctl63=on
ctl00%24ContentPane%24ctl64=on
ctl00%24ContentPane%24ctl65=on
ctl00%24ContentPane%24ctl66=on
ctl00%24ContentPane%24ctl67=on
ctl00%24ContentPane%24hfState=
__SCROLLPOSITIONX=0
__SCROLLPOSITIONY=41
__EVENTVALIDATION=%2FwEWQwLisrbkCgLHjbmqBAKL%2F5SfCAKG%2FrmMAgLBut3pDwKojaKRBwLcpLuHCAK%2By7HvCgLEt%2FyXCgLl64mCCQKl0sqUBQKV1%2FGvBQKoypnMCQL%2Bg4mtBAKouZ%2BACQLDor2VAwLs%2Ffy2DQLD6rWkBwLgpYbqCQL058Y6Apjq%2F7EBAq%2FHvOoLAsqw2v8FAuWZ%2BBQCgIOWqgoCm%2ByzvwQCqLmjgAkCw6LBlQMC3ovfqg0C%2BfT8vwcClN6a1QECr8e46gsCyrDW%2FwUC5Zn0FAKAg5KqCgKb7K%2B%2FBAKoua%2BACQLDos2VAwKdls6bAQLei%2BuqDQL59IjABwKU3qbVAQKvx8TqCwLKsOL%2FBQLlmYAVAoCDnqoKApvsu78EAqi5q4AJAsOiyZUDAt6L56oNAvn0hMAHApTeotUBAq%2FHwOoLAsqw3v8FAuWZ%2FBQCgIOaqgoCm%2By3vwQC5Jr8%2FgoCqLm3gAkCw6LVlQMC3ovzqg0C%2BfSQwAcClN6u1QECr8fM6gsCyrDq%2FwUC5ZmIFQLajdfIBF9etMTk5BYcS%2BKGoVho%2B9%2BlmFm%2F

 */				
				//Search by name
				simTmp = SetModuleSearchByName(11 + docTypeFilterParams.length + 16, msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX, "/NASearch.aspx", TSConnectionURL.idPOST, "ctl00$ContentPane$tbLastName", "ctl00$ContentPane$tbFirstName");
                simTmp.setReferer("http://www.memphisdailynews.com/NASearch.aspx");
                
				try
				{
					PageZone searchByName = new PageZone("searchByName", "Search By Property Owner Name", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
					searchByName.setBorder(true);
					
					crtFunctionNo = 0;
					
					HTMLControl lastName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$ContentPane$tbLastName", "Last Name", 1, 1, 1, 1, 30, null, simTmp.getFunction( crtFunctionNo++ ), searchId );
					lastName.setJustifyField( true );
					lastName.setRequiredExcl( true );
                    lastName.setFieldNote( "Please enter at least the last name initial!" );
					searchByName.addHTMLObject( lastName );
					
					HTMLControl firstName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$ContentPane$tbFirstName", "First Name", 1, 1, 2, 2, 30, null, simTmp.getFunction( crtFunctionNo++ ), searchId );
					firstName.setJustifyField( true );
					firstName.setRequiredExcl( true );
                    firstName.setFieldNote( "Please enter at least the first name initial!" );
					searchByName.addHTMLObject( firstName );
					
					HTMLControl tbCrimeAddr = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$ContentPane$tbCompany", "Company Name", 1, 1, 3, 3, 30, null, simTmp.getFunction( crtFunctionNo++ ), searchId );
					tbCrimeAddr.setJustifyField( true );
					tbCrimeAddr.setRequiredExcl( true );
					searchByName.addHTMLObject( tbCrimeAddr );

					simTmp.setModuleParentSiteLayout( searchByName );

				}
				catch( FormatException e )
				{
				    e.printStackTrace();
				}
                
				crtFunctionNo = 3;
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTTARGET","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTARGUMENT","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__LASTFOCUS","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__VIEWSTATE",VIEWSTATE_NAME_VALUE); 
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbPrimary","on");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbFrom","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbTo","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$ctl01", "on");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$btnSearch","Search");
                
                for( int i = 0 ; i <docTypeFilterParams.length ; i ++ )
                {
                    simTmp.getFunction( crtFunctionNo++ ).setHiddenParam( docTypeFilterParams[i], "on" );
                }
                
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$hfState","");	                
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeAddr","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeCity","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeZip","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$Report","rbRep4");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbPromo","");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTVALIDATION", EVENTVALIDATION_NAME_VALUE);
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__SCROLLPOSITIONX", "0");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__SCROLLPOSITIONY", "191");
                
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbLawGovernmentShelby", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbBusinessPeopleShelby", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbRealEstateDevelopmentDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbLawGovernmentDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbBusinessPeopleDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbRealEstateDevelopmentShelby", "on");

				
				
                		
				//Search by address
				simTmp= SetModuleSearchByAddress(12 + docTypeFilterParams.length + 16, msiServerInfoDefault, 
						TSServerInfo.ADDRESS_MODULE_IDX, "/NASearch.aspx", TSConnectionURL.idPOST, 
						"ctl00$ContentPane$tbStNo", "ctl00$ContentPane$tbStName");
				
				simTmp.setReferer("http://www.memphisdailynews.com/NASearch.aspx");			
				
				try
				{
					PageZone searchByAddress = new PageZone("searchByAddress", "Search By Address", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
					searchByAddress.setBorder(true);
					
					crtFunctionNo = 0; 
					
					HTMLControl streetNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$ContentPane$tbStNo", "Street No", 1, 1, 1, 1, 30, null, simTmp.getFunction(crtFunctionNo++), searchId );
					streetNo.setJustifyField( true );
					streetNo.setRequiredExcl( true );
					searchByAddress.addHTMLObject( streetNo );
					
					HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$ContentPane$tbStName", "Street Name<BR>Street Type", 1, 1, 2, 2, 30, null, simTmp.getFunction(crtFunctionNo++), searchId );
					streetName.setJustifyField( true );
					streetName.setRequiredExcl( true );
					searchByAddress.addHTMLObject( streetName );

					simTmp.setModuleParentSiteLayout( searchByAddress );

				}
				catch( FormatException e )
				{
				    e.printStackTrace();
				}
				                
				crtFunctionNo = 2;
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTTARGET","");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTARGUMENT","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__LASTFOCUS","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__VIEWSTATE", VIEWSTATE_ADDRESS_VALUE);                
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbCity","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbState","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbZip","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbFrom","");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$tbTo","");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$ctl01", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$btnSearch","Search");
                
                for( int i = 0 ; i < docTypeFilterParams.length ; i ++ )
                {
                    simTmp.getFunction(crtFunctionNo++).setHiddenParam(docTypeFilterParams[i], "on" );
                }
                                
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$hfState","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeAddr","");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeCity","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbCrimeZip","");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$Report","rbRep4");	
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$RightPane$tbPromo","");				
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__SCROLLPOSITIONX", "0");
                simTmp.getFunction(crtFunctionNo++).setHiddenParam("__SCROLLPOSITIONY", "191");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("__EVENTVALIDATION", EVENTVALIDATION_ADDRESS_VALUE);
				
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbLawGovernmentShelby", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbBusinessPeopleShelby", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbRealEstateDevelopmentDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbLawGovernmentDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbBusinessPeopleDesoto", "on");
				simTmp.getFunction(crtFunctionNo++).setHiddenParam("ctl00$ContentPane$cbRealEstateDevelopmentShelby", "on");

			}
			msiServerInfoDefault.setupParameterAliases();
			setModulesForAutoSearch(msiServerInfoDefault);		
			setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		}
		
        ServerInfoDSMMap DSM=new ServerInfoDSMMap();
       	
    	//msiServerInfoDefault= DSM.getServerInfo("TNShelbydn.xml", searchId);
        msiServerInfoDefault.setupParameterAliases();
    	setModulesForAutoSearch(msiServerInfoDefault);		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
	        return msiServerInfoDefault;
		
	}

	public TNShelbyDN(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		setGoBackOneLevel(true);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForAutoSearch(l);		
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForGoBackOneLevelSearch(l);		
	}
	
	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	static int index = 0;
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String sTmp3 = "";
		String sParcelId = "";
		String rsResponce = Response.getResult();
		
		//quickfix
		rsResponce=rsResponce.replaceAll("NBSP","nbsp"); 
		
		if( rsResponce.contains( "you are not logged in" ) ){
			HTTPSiteInterface thisSite = HTTPSiteManager.pairHTTPSiteForTSServer( "TNShelbyDN" ,searchId,miServerID);
			
			if( thisSite != null ){
				thisSite.destroyAllSessions();
			}
		}
		
		if(rsResponce.indexOf("Too many records to display")!=-1)
		{
			Response.getParsedResponse().setError(rsResponce);									
			return;
		}
		if ( rsResponce.indexOf("Unexpected Error")!=-1 || rsResponce.indexOf("Your search did not") != -1 || rsResponce.indexOf("Your search has returned -2 listings.") != -1) {
			return; //no result
		}
		String initialResponse = rsResponce; 
		switch (viParseID) {
			case ID_SEARCH_BY_NAME :
            case ID_SEARCH_BY_ADDRESS :
            	try{
            		rsResponce=rsResponce.substring(rsResponce.indexOf("Results", rsResponce.indexOf("NOTE")));
            	}
            	catch( Exception e ){
            		return;
            	}
            	
                rsResponce=rsResponce.substring(0, rsResponce.lastIndexOf("</table>", rsResponce.lastIndexOf("</div>"))+8);
                String rez="<table cellspacing=\"0\" border=\"1\" style=\"width:100%;border-collapse:collapse;\">"+
                           "<tr style=\"background-color:White;font-weight:bold;\">"+
                           "<td>Details</td><td>Date</td><td>Name Type</td><td>Name</td><td>Address</td><td>City</td><td>State</td><td>Doc Type</td></tr>";
                int i, z=0;
                while ((i=rsResponce.indexOf("<span style", z))!=-1) {
                    int j = rsResponce.indexOf( "</a>", i ) + 4;

                    int j2=rsResponce.indexOf("(", j);

                    String type=rsResponce.substring(j, j2);
                    
                    j=rsResponce.indexOf("<tr>", j);
                    z=rsResponce.indexOf("</table>", j);
                    
                    if( j < 0 || z < 0 )
                    {
                        break;
                    }
                    
                    String body=rsResponce.substring(j, z);
                    body = body.replaceAll( "<tr>\\s*<th.*</th>\\s*</tr>", "" );
                    
                    body=body.replaceAll("</tr>", "<td>"+type+"</td></tr>");
                    rez+=body;
                }
                rez+="</table>";
                rez=rez.replaceAll(" class=\"alt\"", "");
                rsResponce=rez;

				sTmp3 = CreatePartialLink(TSConnectionURL.idGET);
                rsResponce=rsResponce.replaceAll("javascript:OpenChild\\('([^']*)','([^']*)'\\)", sTmp3+"/Details.aspx&fk=$1&xid=$2");
                rsResponce=rsResponce.replaceAll("javascript:OpenChildRegister\\('([^']*)','([^']*)','([^']*)'\\)", sTmp3+"/Details.aspx&fk=$1&xid=$2&tblCode=$3");

				parser.Parse(Response.getParsedResponse(), rsResponce, 
						Parser.PAGE_ROWS,getLinkPrefix(TSConnectionURL.idGET ), TSServer.REQUEST_SAVE_TO_TSD);

				break;
			case ID_SEARCH_BY_PARCEL: // /this is the final page
			int ist,
			iend;

			ist = rsResponce.indexOf("<span");
			iend = rsResponce.lastIndexOf("</table>") + 8;
			if (ist == -1) {
				return; // no result
			}

			// /extract data table
			rsResponce = rsResponce.substring(ist, iend);

			// get parcel id
			sParcelId = getParcelId(Response.getQuerry());

			if ((rsResponce.indexOf("Judgments") != -1) || (rsResponce.indexOf("Filings") != -1)) {
				if (rsResponce.indexOf("Judgments") != -1) {
					sParcelId = "J" + sParcelId;
				}

				if (rsResponce.indexOf("Filings") != -1) {
					sParcelId = "F" + sParcelId;
				}
				rsResponce = "File ID: " + sParcelId + "<hr/>" + rsResponce;
			} else {
				rsResponce = "File ID: " + sParcelId + "<hr/>" + rsResponce;
			}

			String documentType = getDocTypeFromResponse( rsResponce );
			
			if ((!downloadingForSave)) {
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + sAction + "&" + Response.getQuerry();

				if (FileAlreadyExist(sParcelId + documentType + ".html") ) {
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, sAction, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
			} else {
				// for html
				msSaveToTSDFileName = sParcelId + documentType + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
			}
			break;
			case ID_GET_LINK :
			case ID_SAVE_TO_TSD :
				if (sAction.equals("/NASearch.aspx" ) && Response.getQuerry().indexOf("btnSearch")!=-1)
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				else
			      if (sAction.equals("/NASearch.aspx" ) && Response.getQuerry().indexOf("btnDupe")!=-1)
								ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				if (viParseID == ID_GET_LINK) {
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
					} else {// on save
						downloadingForSave = true;
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
						downloadingForSave = false;
					}
				break;			
		}
	}

    protected String getParcelId(String l)
    {
    	return StringUtils.getNestedString(l,"fk=","&").trim();
    }
    
    private String getDocTypeFromResponse( String rsResponse )
    {
        String docType = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("<span id=\"lblDocument\">", ": Record Details</span>", rsResponse);
        docType = docType.replaceAll( "[^A-Za-z0-9\\-]", "" );
        docType = docType.toUpperCase();
        
        /*
         Bug No. 949 - fixed
         Shelby - same instrument retrieved both form Ro and DN
         It was necesary to make a standard notation so . 
         The solution was to parse AGREEMENT to AGMT.
         */
        
        docType = docType.replaceAll( "AGREEMENT", "AGMT");
        return docType;
    }
    
	protected String getFileNameFromLink(String link) {
		String parcelId = StringUtils.getNestedString(link,"fk=","&").trim();
		if (link.indexOf("SCT_GS_NEW")!=-1) {
		    parcelId="F"+parcelId;
		}
		if (link.indexOf("SCT_GS_JUDG")!=-1) {
		    parcelId="J"+parcelId;
		}
		return parcelId + ".html";
	}

/*	public ServerResponse LogIn()  throws ServerResponseException{
        
        ServerResponse rtrnResponse = new ServerResponse();
        
        if(CookieManager.isCookieValid("name[TNShelbyDN] ") && CookieManager.isCookieValid( "addr[TNShelbyDN] " ))
        {
            return rtrnResponse;
        }

        String nameCookie = getSearchSession( true );
        if( "".equals( nameCookie ) )
        {
            rtrnResponse.setError("Null Cookie on connect request [Shelby DN]");
            throw new ServerResponseException(rtrnResponse); 
        }
        else
        {
            CookieManager.addCookie("name[TNShelbyDN] ", nameCookie);
        }
        
        String addrCookie = getSearchSession( false );
        if( "".equals( addrCookie ) )
        {
            rtrnResponse.setError("Null Cookie on connect request [Shelby DN]");
            throw new ServerResponseException(rtrnResponse); 
        }
        else
        {
            CookieManager.addCookie("addr[TNShelbyDN] ", addrCookie);
        }
        
        return rtrnResponse;
	}
*/
    private String getSearchSession( boolean nameSearch )
    {
        StringBuffer finalcookie=new StringBuffer();
        
        // main page
		ATSConn c = new ATSConn(11, "http://www.memphisdailynews.com/", ATSConnConstants.GET, null, null,searchId,miServerID);
		c.doConnection();
		// obtain first cookie
		Object o = c.getResultHeaders().get("Set-Cookie");
		if (o == null) {
			return "";
		}
        
        String cookie = o.toString();
		cookie = cookie.replaceAll("\\[|\\]", "");
		cookie = cookie.substring(0, cookie.indexOf(";"));

		String dmid = "" + (int) (Math.random() * 1000000) + "_" + System.currentTimeMillis();
		cookie = "_dmid=" + dmid + "; " + cookie;
        
        String rsResponse = c.getResult().toString();
        
        try
        {
        	// Login
            int istart = rsResponse.indexOf("__VIEWSTATE\" value=\"");
			int iend = rsResponse.indexOf("\"", istart + 20);
			String viewState = rsResponse.substring(istart + 20, iend);

			istart = rsResponse.indexOf("__EVENTVALIDATION\" value=\"");
			iend = rsResponse.indexOf("\"", istart + 26);
			String eventValidation = rsResponse.substring(istart + 26, iend);

			HashMap<String, String> conparams = new HashMap<String, String>();
			HashMap<String, String> reqparams = new HashMap<String, String>();
            
            conparams.put("Cookie", cookie);
            conparams.put("Referer", "http://www.memphisdailynews.com");
            
            reqparams.put("__EVENTTARGET","");
            reqparams.put("__EVENTARGUMENT","");
            reqparams.put("__VIEWSTATE", viewState);
            reqparams.put("__EVENTVALIDATION", eventValidation);
            
            reqparams.put("ctl00$ContentPane$tbUsername", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNShelbyDN", "user"));
            reqparams.put("ctl00$ContentPane$tbPassword", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNShelbyDN", "password"));
            reqparams.put("ctl00$ContentPane$btnLogin","Login");
            reqparams.put("ctl00$RightPane$tbUsername", "");
            reqparams.put("ctl00$RightPane$tbPassword", "");	        	
            reqparams.put("ctl00$RightPane$tbCrimeAddr","");    
            reqparams.put("ctl00$RightPane$tbCrimeCity","");    
            reqparams.put("ctl00$RightPane$tbCrimeZip","");
            reqparams.put("ctl00$RightPane$Report","rbRep4");            
            reqparams.put("ctl00$RightPane$tbPromo","");

            c = new ATSConn(11, "http://www.memphisdailynews.com/Default.aspx", ATSConnConstants.POST, reqparams, conparams,searchId,miServerID);
			c.setFollowRedirects(false);
			c.doConnection();
			
			List o2 =(List) c.getResultHeaders().get("Set-Cookie");
			if (o2 == null) {
				return "";
			}

			String cookie2 = "";
			Iterator cookieIterator = o2.iterator();
			while( cookieIterator.hasNext() ){
				String currentCookie = cookieIterator.next().toString();
				
				currentCookie = currentCookie.replaceAll("\\[|\\]","");
				currentCookie = currentCookie.substring(0,currentCookie.indexOf(";"));
				
				cookie2 += currentCookie + "; ";
			}
			
			cookie2 = cookie2.replaceAll("\\[|\\]","");
			cookie2 = cookie2.substring(0,cookie2.lastIndexOf(";"));
            
            finalcookie.append(cookie);
            finalcookie.append("; ");
            finalcookie.append(cookie2); 

            CookieManager.addCookie(Integer.toString(miServerID), finalcookie.toString());
            
            // redirect to
            conparams=new HashMap<String,String>();
            
            conparams.put("Cookie",finalcookie.toString());
            conparams.put("Referer","http://www.memphisdailynews.com");
            
            c = new ATSConn( 11, "http://www.memphisdailynews.com/", ATSConnConstants.GET, null, conparams  ,searchId,miServerID);
            c.doConnection();
            
            String rez = c.getResult().toString();
            

            // click on Name and Property Research
            conparams = new HashMap<String, String>();
			conparams.put("Cookie", finalcookie.toString());
			conparams.put("Referer", "http://www.memphisdailynews.com");
			conparams.put("Host", "www.memphisdailynews.com");

			c = new ATSConn(11, "http://www.memphisdailynews.com/NASearch.aspx", ATSConnConstants.GET, null, conparams,searchId,miServerID);
			c.doConnection();
			rez = c.getResult().toString();
            
			
            if( !nameSearch )
            {
                // click on PropertySearch
                istart = rez.indexOf("__VIEWSTATE\" value=\"");
                iend = rez.indexOf("\"", istart + 20);
                viewState = rez.substring(istart + 20, iend);
    
                String eventVal = "__EVENTVALIDATION\" value=\"";
                istart = rez.indexOf( eventVal );
                iend = rez.indexOf("\"", istart + eventVal.length());
                eventValidation = rez.substring(istart + eventVal.length(), iend);
                
                reqparams = new HashMap<String,String>();
                reqparams.put( "__EVENTTARGET", "ctl00$ContentPane$lbPropertySearch" );
                reqparams.put( "__EVENTARGUMENT", "" );
                reqparams.put( "__VIEWSTATE", viewState );
                reqparams.put( "ctl00$ContentPane$tbFirstName", "" );
                reqparams.put( "ctl00$ContentPane$tbLastName", "" );
                reqparams.put( "ctl00$ContentPane$tbCompany", "" );
                reqparams.put( "ctl00$ContentPane$cbPrimary", "on" );
                reqparams.put( "ctl00$ContentPane$tbFrom", "" );
                reqparams.put( "ctl00$ContentPane$tbTo", "" );
                reqparams.put( "ctl00$ContentPane$hfState", "" );
                reqparams.put( "ctl00$RightPane$tbCrimeAddr", "" );
                reqparams.put( "ctl00$RightPane$tbCrimeCity", "" );
                reqparams.put( "ctl00$RightPane$tbCrimeZip", "" );
                reqparams.put( "ctl00$RightPane$Report", "rbRep4" );
                reqparams.put( "ctl00$RightPane$tbPromo", "" );
                reqparams.put( "__EVENTVALIDATION", eventValidation );
                reqparams.put( "__LASTFOCUS", "");
                reqparams.put( "__SCROLLPOSITIONX", "0");
                reqparams.put( "__SCROLLPOSITIONY", "68");
                reqparams.put( "ctl00$ContentPane$ctl01", "on");
                
                conparams=new HashMap<String,String>();
                conparams.put("Cookie",finalcookie.toString());
                conparams.put("Referer","http://www.memphisdailynews.com/NASearch.aspx");
                
                c = new ATSConn( 11, "http://www.memphisdailynews.com/NASearch.aspx", ATSConnConstants.POST, reqparams, conparams ,searchId,miServerID );
                c.doConnection();
                rez = c.getResult().toString();

            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return "";
        }
        
        return finalcookie.toString();
    }
    
    public void resetServerSession()
    {
        CookieManager.resetCookie( "name" + Integer.toString(miServerID), true );
        CookieManager.resetCookie( "addr" + Integer.toString(miServerID), true );
    }
    
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        if (pageId == Parser.ONE_ROW)
            p.splitResultRows(pr, htmlString, pageId, "<tr>","</table>", linkStart,  action);
    }

}