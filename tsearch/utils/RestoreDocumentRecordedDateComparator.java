package ro.cst.tsearch.utils;

import java.util.Comparator;

import com.stewart.ats.base.document.RestoreDocumentDataI;

public class RestoreDocumentRecordedDateComparator implements
		Comparator<RestoreDocumentDataI> {

	@Override
	public int compare(RestoreDocumentDataI o1, RestoreDocumentDataI o2) {
		if(o1.getRecordedDate() == null || o2.getRecordedDate() == null){
			return 0;
		} 
		return o1.getRecordedDate().compareTo(o2.getRecordedDate());
	}

}
