package ro.cst.tsearch.servers.types;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.SearchLogger;

public enum ILWillImageRetriever {
	INSTANCE;
	/**
	 * Retrieve ILWill image, based on instrument number
	 * @param inst
	 * @param fileName
	 * @param searchId
	 * @return boolean value indicating whether the image was retrieved or not
	 */
	public boolean retrieveImage(String inst, String fileName, String type, String year, long searchId){
		
		if (FileUtils.existPath(fileName)){
			return true;
		}
		
		boolean propertyInsight =  false;
		try {
			if(StringUtils.isNotBlank(year)){
				propertyInsight = FLSubdividedBasedDASLDT.downloadImageFromPropertyInsight(fileName, getPiQuery(inst, type, year, searchId), searchId).success;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(propertyInsight){
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> found on PropertyInsight.</div>", searchId);
			return true;
		} else {
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> not found on PropertyInsight.</div>", searchId);
		}

		return false;
	}
	
	private static String getPiQuery(String instrument, String type, String year, long searchId){		
		return FLGenericDASLDT.getBasePiQuery(searchId) + ",Type=Rec,SubType=All,Year=" + year + ",Inst=" + instrument;
	}
	
}
