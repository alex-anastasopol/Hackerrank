package ro.cst.tsearch.search;

import java.io.Serializable;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;

/**
 * @author elmarie
 */
public class Decision implements Serializable {
    
    static final long serialVersionUID = 10000000;
	
	private ServerResponse sr;
	private TSInterface intrfServer;

	public Decision(ServerResponse sr){
		this.sr = sr; 
	}
	
	public Decision(ServerResponse sr, TSInterface intrfServer){
		this.sr = sr; 
		this.intrfServer = intrfServer;
	}

	/**
	 * @return
	 */
	public ParsedResponse getParsedResponse() {
		if (sr == null) { 
			return null;
		}else {
			return sr.getParsedResponse();
		}
	}

	public String toString(){
		return "Decision: [ ServerResponse = " + sr +"]"; 
	}
	/**
	 * @return
	 */
	public ServerResponse getServerResponse() {
		return sr;
	}

	/**
	 * @param response
	 */
	public void setServerResponse(ServerResponse response) {
		sr = response;
	}

	/**
	 * @return
	 */
	public TSInterface getIntrfServer() {
		return intrfServer;
	}

	/**
	 * @param interface1
	 */
	public void setIntrfServer(TSInterface interface1) {
		intrfServer = interface1;
	}

}
