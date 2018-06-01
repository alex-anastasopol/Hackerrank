package ro.cst.tsearch.search;

import java.io.Serializable;

import org.apache.log4j.Category;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * @author elmarie
 */
public class ResponseManager implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
	protected static final Category logger = Category.getInstance(ResponseManager.class.getName());
	


	public static ResponseManager getInstance(){
	 	return new ResponseManager();
	}

	
	private ServerResponse previous = null;

	public ResponseManager(){
		resetPreviousResponse();
	}

	public void manageResponse(ServerResponse sr)  {
		ParsedResponse pr = sr.getParsedResponse();
		if (pr.isUnique() || (pr.getResultsCount() == ParsedResponse.UNKNOW_RESULTS_COUNT)) {
			previous = sr; //tin minte acest rezultat
		}else if (pr.isMultiple()){
			//logger.debug("obtained multiple");
			previous = sr; //tin minte ultimul rezultat de multiple obtinut
		}else{
			if (previous.isError()){ //error de zero interations
				previous = sr; //tin minte acest raspuns de none
			}
			//logger.debug("obtained none");
		}
	}

		
	public ServerResponse getLastGoodResponse(){
		return previous;
	}

	public void resetPreviousResponse() {
		previous = new ServerResponse();
		previous.setError( ServerResponse.ZERO_MODULE_ITERATIONS_ERROR);
	}




	
}
