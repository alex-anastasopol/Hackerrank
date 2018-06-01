package ro.cst.tsearch.servers.types;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.parser.HtmlParser3;

/**
 * @author Mihai D.
 */
public class COWeldTR extends COGenericTylerTechTR {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6123777416727104262L;


	public COWeldTR(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    }
	
	
	public String getDetailsPrefix() {
		return "/treasurer/treasurerweb/";
	}
		
	public static void getTestcases() {
		HttpClient client = new HttpClient();
		String url = "http://apps.douglas.co.us/apps/treasurer/tidi/ownerAddressSearchForm.do?ownerName=Smith";
		GetMethod method = new GetMethod(url);
		
		int statusCode = 0;
		try {
			statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
		    }
			// Read the response body.
		    String response = method.getResponseBodyAsString();
		    System.out.println(response);
		    HtmlParser3 parser = new HtmlParser3(response);
		    TableTag mainTable = (TableTag)parser.getNodeByAttribute("class","ApptableDisplayTag",true);
		    for(TableRow row : mainTable.getRows()) {
		    	if(row.getColumnCount()>4) {
		    		System.out.println(row.getColumns()[2].toPlainTextString());
		    	}
		    }
		    for(int i=2; i<=20; i++) {
		    	String urlNext = "http://apps.douglas.co.us/apps/treasurer/tidi/parcelSearchResults.jsp?ResultListTag.COMMAND="+i;
		    	method = new GetMethod(urlNext);
		    	statusCode = client.executeMethod(method);
			    response = method.getResponseBodyAsString();
			    parser = new HtmlParser3(response);
			    mainTable = (TableTag)parser.getNodeByAttribute("class","ApptableDisplayTag",true);
			    for(TableRow row : mainTable.getRows()) {
			    	if(row.getColumnCount()>4) {
			    		System.out.println(row.getColumns()[2].toPlainTextString());
			    	}
			    }
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	
    /*public static void main(String... args) {
		getTestcases();
	}*/

}