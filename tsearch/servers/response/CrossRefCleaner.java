package ro.cst.tsearch.servers.response;

import java.util.Vector;

import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;

public class CrossRefCleaner
{
    public static void removeTooManyCrossRef(ServerResponse response,long searchId) 
    {
        ParsedResponse pr = response.getParsedResponse();
        int crossRefNo = pr.getResultRows().size();
        
        if (crossRefNo > ServletServerComm.MAX_CROSS_REFS_SEARCH) // daca am gasit mai mult de n crossrefuri, nu mai caut dupa ele
                // dar marchez documentul respectiv ca nu s-a cautat dupa crossref pt a fi evidentiat in TSR Index
        {
            pr.setOnlyResultRows(new Vector());
            DocumentI document = pr.getDocument();
            if (document != null){
	            SearchLogger.info("</div><div>Document " + document.prettyPrint() + " has more than 10 crossreferences, so they will not be saved.</div>", searchId);
	        }
        }
    }
}