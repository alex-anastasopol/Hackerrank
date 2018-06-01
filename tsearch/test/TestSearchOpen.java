package ro.cst.tsearch.test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.ZipUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn.TransactionResponse;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoResultType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoType;
import com.stewartworkplace.starters.ssf.services.docadmin.ErrorContainerType;
import com.stewartworkplace.starters.ssf.services.docadmin.ErrorType;
import com.stewartworkplace.starters.ssf.services.docadmin.SuccesType;

public class TestSearchOpen {
	
	private static final Logger logger = Logger.getLogger(TestSearchOpen.class);
	
	private int year = 2008;
	private int month = -1;
	private int noOfSearches = 1;
	private int maxNoOfSearches = -1;
	private boolean saveImages = false;
	private boolean uploadImagesOnSsf = false;
	private String destinationFolder = "/data/imagesocr/";
	private String stateAbr = null;
	private String countyName = null;
	private float delay = 1;
	
	public TestSearchOpen() {
		
	}
	
	public TestSearchOpen(int year, int noOfSearches) {
		this.year = year;
		this.noOfSearches = noOfSearches;
	}
	
	public String getSearchTestResults(){
		StringBuilder sb = new StringBuilder();
		sb.append("<table border=\"1\" align=\"center\">\n");
		sb.append("<tr>\n<td>");
		sb.append("No.");
		sb.append("</td>\n<td>");
		sb.append("Search Id");
		sb.append("</td>\n<td>");
		sb.append("Start Date");
		sb.append("</td>\n<td>");
		sb.append("County Name");
		sb.append("</td>\n<td>");
		sb.append("Reopen status");
		sb.append("</td></tr>\n");
		sb.append(testYear(year, noOfSearches));
		sb.append("</table>\n");
		return sb.toString();
	}
	
	private static final String SQL_SEARCH_ID = 
		"SELECT a." + DBConstants.FIELD_SEARCH_ID + ", a. " + DBConstants.FIELD_SEARCH_SDATE + 
		" from ts_search a join ts_search_data1 b on a.id = b.searchId " + 
		" join ts_property c ON a.property_id = c.id " +
		" join ts_search_flags d ON a.id = d.search_id "+
		" where (b.context is not null or " + DBConstants.FIELD_SEARCH_FLAGS_TO_DISK + " = 1) " + 
		" AND year(a.sdate) = ? " +
		" AND (month(a.sdate) = ? OR -1 = ?) " +
		" AND c.county_id = ? " +
		" AND (c.state_id = ? OR -1 = ?) " +
		" limit ?";
	
	private static final String SQL_SEARCH_ID_TSR_CREATED = 
		"SELECT a." + DBConstants.FIELD_SEARCH_ID + ", a. " + DBConstants.FIELD_SEARCH_SDATE + 
		" from ts_search a join ts_search_data1 b on a.id = b.searchId " + 
		" join ts_property c ON a.property_id = c.id " +
		" join ts_search_flags d ON a.id = d.search_id "+
		" where (b.context is not null or " + DBConstants.FIELD_SEARCH_FLAGS_TO_DISK + " = 1) " + 
		" AND d.tsr_created = 1 " +
		" AND year(a.sdate) = ? " +
		" AND (month(a.sdate) = ? OR -1 = ?) " +
		" AND c.county_id = ? " +
		" AND (c.state_id = ? OR  -1 = ?) " +
		" limit ?";
	
	private String testYear(int year, int times){
		StringBuilder sb = new StringBuilder();
		
		long stateId = -1;
		long countyId = -1;
		try {
			stateId = DBManager.getStateForAbvStrict(getStateAbr()).getId();
			countyId = DBManager.getCountyForNameAndStateIdStrict(getCountyName(), stateId,true).getId();
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("Using all states or counties");
		}
		GenericCounty[] allCounties = null;
		if(countyId != -1) {
			allCounties = new GenericCounty[1];
			allCounties[0] = DBManager.getCountyForNameAndStateIdStrict(getCountyName(), stateId,true);
		} else {
			if(stateId != -1) {
				List<CountyWithState> tempCounties = DBManager.getAllCountiesForState((int)stateId);
				allCounties = new GenericCounty[tempCounties.size()];
				int i = 0;
				for (CountyWithState countyWithState : tempCounties) {
					GenericCounty gc = new GenericCounty();
					gc.setId(countyWithState.getCountyId());
					gc.setName(countyWithState.getCountyName());
					gc.setStateId(stateId);
					allCounties[i++] = gc;
				}
			} else {
				allCounties = DBManager.getAllCounties();
			}
		}
		
		int noOfSearchesAnalyzed = 0;
		for (int i = 0; i < allCounties.length; i++) {
			GenericCounty county = allCounties[i];
			try {
				List<Map<String, Object>> allData = null;
				if(isSaveImages()) {
					allData = DBManager.getSimpleTemplate().queryForList(SQL_SEARCH_ID, year, getMonth(), getMonth(), county.getId(), stateId, 100000000);
				} else if(isUploadImagesOnSsf()){
					allData = DBManager.getSimpleTemplate().queryForList(SQL_SEARCH_ID_TSR_CREATED, year, getMonth(), getMonth(), county.getId(), stateId,stateId, Long.MAX_VALUE);
				}else {
					allData = DBManager.getSimpleTemplate().queryForList(SQL_SEARCH_ID, year, getMonth(), getMonth(), county.getId(), stateId, stateId, times);
				}
				for (Map<String, Object> row : allData) {
					if(noOfSearchesAnalyzed++ == getMaxNoOfSearches()){
						return sb.toString();
					}
					Thread.sleep( ((Float)(getDelay() * 1000)).longValue() );
					BigInteger searchId = (BigInteger)row.get(DBConstants.FIELD_SEARCH_ID);
					String status = getSearchOpenStatus(searchId.longValue());
					sb.append("<tr>\n<td>");
					sb.append(noOfSearchesAnalyzed);
					sb.append("</td>\n<td>");
					sb.append(searchId);
					sb.append("</td>\n<td>");
					sb.append(row.get(DBConstants.FIELD_SEARCH_SDATE));
					sb.append("</td>\n<td>");
					sb.append(county.getName());
					sb.append("</td>\n<td>");
					sb.append(status);
					sb.append("</td></tr>\n");
					System.err.println("Search " +  searchId + " status " + status);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
	private String getSearchOpenStatus(Long searchId) {
		byte[] databaseContext = null;
		String status = "";
		try {
			databaseContext = DBManager.loadSearchDataFromDB(searchId);
			ZipUtils.unzipContext( databaseContext, "/temp", searchId );
			File file = new File("/temp" + File.separator + searchId + File.separator + "__search.xml");
			if(!file.exists())
				status = "<font color=\"red\">Failed -- No __search.xml</font>";
			else {
				Reader inputReader = new FileReader(file);
	            
	            StringBuffer sb = new StringBuffer();
	            char[] buffer= new char[4*1024];
				while (true)
				{
					int bytes_read= inputReader.read(buffer);
					if (bytes_read == -1){
						break;
					}
					sb.append(buffer, 0, bytes_read);
				}
				inputReader.close();
				Search search = (Search)XStreamManager.getInstance().fromXML( sb.toString() );
				sb = null;
				if(search != null) {
					status = "Succes";
					if(saveImages) {
						saveImagesFor(search);
					}else if(uploadImagesOnSsf){
						uploadImagesOnSsf(search);
					}
				}
				else
					status = "<font color=\"red\">Failed</font>";
				search = null;
				System.gc();
			}
		} catch (Throwable e) {
			status = "Failed";
			e.printStackTrace();
			
		} finally {
			try {
				File f = new File("/temp" + File.separator + searchId);
				FileUtils.deleteDir(f);
				if(f.exists())
					f.delete();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}
		return status;
	}

	private boolean saveImagesFor(Search search) {
		if(search == null)
			return false;
		DocumentsManagerI docManagerI = search.getDocManager();
		boolean noErrors = true;
		try {
			docManagerI.getAccess();
			for (DocumentI document : docManagerI.getDocumentsWithDocType("TRANSFER","MORTGAGE")) {
				try {
				if(document.hasImage() && document.getImage().isSaved()) {
					File imageFile = new File(document.getImage().getPath());
					
					if(imageFile.exists()) {
						String state = DBManager.getStateForId(
								Long.parseLong(search.getSa().getAtribute(SearchAttributes.P_STATE))).getStateAbv();
						String county = DBManager.getCountyForId(
								Long.parseLong(search.getSa().getAtribute(SearchAttributes.P_COUNTY))).getName().replaceAll(" ", "");
							
						String newFullName = destinationFolder + File.separator + 
							state + File.separator + county + File.separator + 
							document.getNiceFullFileName() + FileUtils.getFileExtension(document.getImage().getPath());
						File newImageFile = new File(newFullName);
						if(!newImageFile.exists()) {
							org.apache.commons.io.FileUtils.copyFile(imageFile, newImageFile);
						}
						else {
							System.err.println("File " + newFullName + " already exists");
						}
					}
				}
				} catch (Exception e) {
					e.printStackTrace();
					noErrors = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			docManagerI.releaseAccess();
		}
		return noErrors;
	}

	private void uploadImagesOnSsf(Search search) {
		if(search == null){
			return;
		}
		logger.info("------- start uploadImagesOnSsf searchId=" +search.getID());
		boolean atLeastOne = false;
		DocumentsManagerI docManagerI = search.getDocManager();
		try {
			docManagerI.getAccess();
			for (DocumentI doc : docManagerI.getRoLikeDocumentList(true)) {
				
				if (doc.isOneOf(DocumentTypes.PATRIOTS, DocumentTypes.OTHERFILES, DocumentTypes.PRIORFILE)
						|| !doc.hasImage() 
						|| !doc.isIncludeImage() 
						|| !doc.getImage().isSaved()) {
					continue;
				}
				File img = new File(doc.getImage().getPath());
				if(!img.exists()){
					logger.info("Could not find file"+img.getPath() );
					continue;
				}
				try {
					
			    	
			    	String stateIdStr = (String)search.getSa().getAttributes().get(SearchAttributes.P_STATE);
			    	String countyIdStr = (String)search.getSa().getAttributes().get(SearchAttributes.P_COUNTY);
			    	
			    	int stateId = Integer.parseInt(stateIdStr);
			    	int countyId = Integer.parseInt(countyIdStr);
			    	
			    	int countyFips = County.getCounty(countyId).getCountyFips();
			    	int stateFips = State.getState(stateId).getStateFips();
			    	
			    	int imageTransactionId = search.getImageTransactionId();
			    	if(imageTransactionId <=0){//first time
				    	try{
					    	if(ServerConfig.isStewartSsfImagePostEnabled() ){
					    		TransactionResponse transResponse =(new  DocAdminConn(search.getCommId())).reserveUniqueTransactionId(search.getID()+"",stateFips,countyFips);
					    		imageTransactionId = transResponse.id;
					    		search.setImageTransactionId(imageTransactionId);
					    	}
				    	}catch(Exception e){
				    		e.printStackTrace();
				    		logger.error(e.getMessage()+"   "+e.getCause());
				    	}
			    	}
			    	
			    	if(imageTransactionId<=0){
			    		logger.error("Could NOT RESERVE SSF TRANSACTION ID");
			    		return;
			    	}
			    	
			    	
			    	if(StringUtils.isBlank(doc.getImage().getSsfLink())){
						DocumentInfoResultType result = (new DocAdminConn(search.getCommId()))
								.uploadDocument(doc, imageTransactionId, stateFips, countyFips, 
										(String) search.getSa().getAttributes().get(SearchAttributes.LD_PARCELNO), 
										false, false, false);
						boolean success = (SuccesType.SUCCESS == result.getStatus());
						if(success){
							if(result.getDocInfo().length>0){
								DocumentInfoType docInfo = result.getDocInfo()[0];
								String link =docInfo.getLink().toString();	
								doc.getImage().setSsfLink(link);
								atLeastOne = true;
								SearchLogger.info("<br/>Image : "+"<a href='"+link+"'>"+doc.prettyPrint()+"</a>"+" uploaded to SSF ", search.getID());
							}else{
								SearchLogger.info("<br/>Image : "+doc.prettyPrint()+" can't be uploaded to SSF ", search.getID());
							}
						}else{
							ErrorContainerType erorContainer = result.getErrors();
							if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
								ErrorType []allerr = erorContainer.getError();
								for(int i=0;i<allerr.length;i++){
									if("ALREADY_PRESENT".equals(allerr[i].getCode().toString())){
										DocumentInfoType docInfo = result.getDocInfo()[0];
										String link =docInfo.getLink().toString();	
										doc.getImage().setSsfLink(link);
										SearchLogger.info("<br/>Image : "+"<a href='"+link+"'>"+doc.prettyPrint()+"</a>"+" found on SSF ", search.getID());
									}else{
										SearchLogger.info("<br/>Image : "+doc.prettyPrint()+" can't be uploaded to SSF. Reason: "+allerr[i], search.getID());
									}
								}
							}else{
								SearchLogger.info("<br/>Image : "+doc.prettyPrint()+" can't be uploaded to SSF ", search.getID());
							}
						}
			    	}
				}catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getCause());
				}
			}
			if(atLeastOne){
				Search.saveSearch(search);
				logger.error("Search "+ search.getID() +" Saved because at least one SSF upload succeded");
			}
		}catch (Exception e) {
			logger.error(e.getMessage()+"  "+e.getCause());
		}finally {
			docManagerI.releaseAccess();
		}
		
		try{
			docManagerI.getAccess();
			for( DocumentI doc:docManagerI.getRoLikeDocumentList() ){
				if(doc.hasImage()&&doc.getImage().isSaved()){
					if(doc.deleteImage(search.getID(), true)){
						logger.info("<br/>Delete Image: "+doc.prettyPrint());
					}else{
						logger.info("<br/>Couldn't delete image: "+doc.prettyPrint());
					}
				}
			}
		}finally{
			docManagerI.releaseAccess();
		}
		if(!atLeastOne){
			logger.info("NO UPLOAD PERFORMED");
		}
		logger.info("------- end uploadImagesOnSsf searchid = "+search.getID());
	}
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getNoOfSearches() {
		return noOfSearches;
	}

	public void setNoOfSearches(int noOfSearches) {
		this.noOfSearches = noOfSearches;
	}

	public int getMaxNoOfSearches() {
		return maxNoOfSearches;
	}

	public void setMaxNoOfSearches(int maxNoOfSearches) {
		this.maxNoOfSearches = maxNoOfSearches;
	}

	/**
	 * @return the saveImages
	 */
	public boolean isSaveImages() {
		return saveImages;
	}

	/**
	 * @param saveImages the saveImages to set
	 */
	public void setSaveImages(boolean saveImages) {
		this.saveImages = saveImages;
	}

	/**
	 * @return the destinationFolder
	 */
	public String getDestinationFolder() {
		return destinationFolder;
	}

	/**
	 * @param destinationFolder the destinationFolder to set
	 */
	public void setDestinationFolder(String destinationFolder) {
		this.destinationFolder = destinationFolder;
	}

	/**
	 * @return the stateAbr
	 */
	public String getStateAbr() {
		return stateAbr;
	}

	/**
	 * @param stateAbr the stateAbr to set
	 */
	public void setStateAbr(String stateAbr) {
		this.stateAbr = stateAbr;
	}

	/**
	 * @return the countyAbr
	 */
	public String getCountyName() {
		return countyName;
	}

	/**
	 * @param countyAbr the countyAbr to set
	 */
	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}

	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}

	/**
	 * @param delay the delay to set
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}
	
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(String delay) {
		try {
			this.delay = Float.parseFloat(delay);
			if(this.delay == 0){
				this.delay = 1;
			}
		} catch (Exception e) {
		}
	}

	/**
	 * @return the month
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * @param month the month to set
	 */
	public void setMonth(int month) {
		if(month < 1) {
			this.month = -1;
		}
		else if(month > 12){
			this.month = 12;
		} else {
			this.month = month;
		}
	}
	
	/**
	 * @param month the month to set
	 */
	public void setMonth(String month) {
		if(month != null) {
			try {
				setMonth(Integer.parseInt(month));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isUploadImagesOnSsf() {
		return uploadImagesOnSsf;
	}

	public void setUploadImagesOnSsf(boolean uploadImagesOnSsf) {
		this.uploadImagesOnSsf = uploadImagesOnSsf;
	}
}
