package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;

public class MODataTreeImgRetriever {
	
	transient protected List<DataTreeStruct> datTreeList;

	MODataTreeImgRetriever(DataSite dataSite){
		if (datTreeList == null){
			datTreeList = initDataTreeStruct(dataSite);
		}
	}
	
	protected List<DataTreeStruct> initDataTreeStruct(DataSite dataSite){
		return DataTreeManager.getProfileDataUsingStateAndCountyFips(dataSite.getCountyFIPS(), dataSite.getStateFIPS());
	}
	
	public static DataTreeAccount getDataTreeAccount(String communityId){
		SitesPasswords instance = SitesPasswords.getInstance();
		return new DataTreeAccount(
				instance.getPasswordValue(communityId, "DataTree", "datatree.account"),
				instance.getPasswordValue(communityId, "DataTree", "datatree.user"),
				instance.getPasswordValue(communityId, "DataTree", "datatree.password"),
				instance.getPasswordValue(communityId, "DataTree", "datatree.identifier")
				);
	}
	
	public boolean retrieveImageFromDataTree(InstrumentI instrument, String fileName, String path, String year, long searchId, DataSite dataSite) throws DataTreeImageException{
		
		if (datTreeList == null){
			datTreeList = initDataTreeStruct(dataSite);
		}
		
		int commId = InstanceManager.getManager().getCommunityId(searchId);
		
		DataTreeAccount acc = getDataTreeAccount(String.valueOf(commId));
		List<DataTreeStruct> toSearch = new ArrayList<DataTreeStruct>(2);
		
		for (DataTreeStruct struct : datTreeList){
			if ("DAILY_DOCUMENT".equalsIgnoreCase(struct.getDataTreeDocType())){
				toSearch.add(struct);
			}
		}
		
		boolean imageDownloaded = false;
		List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
		
		for (DataTreeStruct struct : toSearch) {
			try {
				if ((imageDownloaded = DataTreeManager.downloadImageFromDataTree(acc, struct, instrument, path, "", ""))) {
					break;
				}
			} catch (DataTreeImageException e) {
				exceptions.add(e);
			}
		}

		if (imageDownloaded){	
			SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" 
									+ instrument.getBook() + "page=" 
									+ instrument.getPage() + "inst="
									+ instrument.getInstno() + " was taken from DataTree<br/>", searchId);
		} else {	
			SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" 
					+ instrument.getBook() + "page=" 
					+ instrument.getPage() + "inst="
					+ instrument.getInstno() + " was not found on DataTree<br/>", searchId);
		}

		if (toSearch.size() == exceptions.size() && !exceptions.isEmpty()) {
			DataTreeConn.logDataTreeImageException(instrument, searchId, exceptions, true);
		}

		return imageDownloaded;
	}
	
}
