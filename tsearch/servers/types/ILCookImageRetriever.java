package ro.cst.tsearch.servers.types;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

public enum ILCookImageRetriever {
	INSTANCE;
	/**
	 * Retrieve ILCook image, based on instrument number
	 * @param inst
	 * @param fileName
	 * @param searchId
	 * @return boolean value indicating whether the image was retrieved or not
	 */
	public boolean retrieveImage(String inst, String fileName, String type, String year, long searchId){
		
		if( FileUtils.existPath(fileName) ){
			return true;
		}
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		
		boolean recorderOffice = false;
		for(int i=0; i<5; i++){
			recorderOffice = ILCookRO.retrieveImage(inst, fileName, (int)TSServersFactory.getSiteId("IL", "Cook", "RO"), searchId);
			if(recorderOffice)
				break;
			
			try{
	    		TimeUnit.SECONDS.sleep(3);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}	
		}	
		if(recorderOffice){
			search.countNewImage(GWTDataSite.RO_TYPE);
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> found on Recorder's Office.</div>", searchId);
			return true;
		} else {
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> not found on Recorder's Office.</div>", searchId);
		}
		
		boolean propertyInfo = ILCookLA.retrieveImage(inst, fileName, (int)TSServersFactory.getSiteId("IL", "Cook", "LA"), searchId);
		if(propertyInfo){
			search.countNewImage(GWTDataSite.LA_TYPE);
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> found on PropertyInfo.</div>", searchId);
			return true;
		} else {
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> not found on PropertyInfo.</div>", searchId);
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
			//image is counted while downloading
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> found on PropertyInsight.</div>", searchId);
			return true;
		} else {
			SearchLogger.info("<div class='image'>Image <b>" + inst + "</b> not found on PropertyInsight.</div>", searchId);
		}
		
		return false;
	}
	
	private static String getPiQuery(String instrument, String type, String year, long searchId){		
		return FLGenericDASLDT.getBasePiQuery(searchId) + ",Type=Rec,SubType=All,Year="+year+",Inst="+ instrument;
	}
	
}
