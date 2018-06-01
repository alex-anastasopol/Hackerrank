package ro.cst.tsearch.search.filter.testnamefilter;


import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;

public class GenericNameFilterTestFilesSelect extends SelectTag{
		
	protected String createOptions() throws Exception {

		GenericNameFilterTestFiles allFiles[] = DBManager.loadAllFilesForSelect();

		int reportState[] = {-2};
		//Probably is looking for a object with values for this select in some contexts 9session, page, request 
		//loadAttribute("names");
		

		StringBuffer sb = new StringBuffer(3000);		
		sb.append(allOption(reportState));
		
		for (int i = 0; i < allFiles.length; i++) {
			sb.append(
				"<option "
					+ (Util.isValueInArray(allFiles[i].getFileId().intValue(), reportState) ? "selected" : "")
					+ " value='"
					+ allFiles[i].getFileId().intValue()
					+ "'>"
					+ allFiles[i].getName()
					+ "</option>");
		}
		return sb.toString();
	}	
	
	protected  String allOption(int[] id)	throws Exception {

		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All Files</option>" ;
		} else {
			return "";
		}
	}

}
