package ro.cst.tsearch.servers.response;

import java.util.Comparator;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.sort.RecordedDateComparator;

public class ParsedResponseDateComparator implements Comparator<Object>{

	@Override
	public int compare(Object object1, Object object2) {
		if (object1 instanceof ParsedResponse && object2 instanceof ParsedResponse) {
			ParsedResponse parsedResponse1 = (ParsedResponse) object1;
			ParsedResponse parsedResponse2 = (ParsedResponse) object2;
			DocumentI doc1 = parsedResponse1.getDocument();
			DocumentI doc2 = parsedResponse2.getDocument();
			if(doc1 != null && doc2 != null && 
					doc1 instanceof RegisterDocumentI && doc2 instanceof RegisterDocumentI){
				RegisterDocumentI r1 = (RegisterDocumentI)doc1;
				RegisterDocumentI r2 = (RegisterDocumentI)doc2;
				return new RecordedDateComparator().compare(r1, r2);
			}
		}
		return 0;
	}

}
