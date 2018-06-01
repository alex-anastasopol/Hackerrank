package ro.cst.tsearch.connection.http2;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import noNamespace.AuthenticateRemoteUserDocument;
import noNamespace.AuthenticateRemoteUserDocument.AuthenticateRemoteUser;
import noNamespace.AuthenticateRemoteUserResponseDocument;
import noNamespace.AuthenticateRemoteUserResponseDocument.AuthenticateRemoteUserResponse;
import noNamespace.LoadBookPageSearchDocument;
import noNamespace.LoadBookPageSearchDocument.LoadBookPageSearch;
import noNamespace.LoadBookPageSearchResponseDocument;
import noNamespace.LoadBookPageSearchResponseDocument.LoadBookPageSearchResponse;
import noNamespace.LoadDocumentImagesDocument;
import noNamespace.LoadDocumentImagesDocument.LoadDocumentImages;
import noNamespace.LoadDocumentImagesResponseDocument;
import noNamespace.LoadDocumentImagesResponseDocument.LoadDocumentImagesResponse;
import noNamespace.LoadDocumentSearchDocument;
import noNamespace.LoadDocumentSearchDocument.LoadDocumentSearch;
import noNamespace.LoadDocumentSearchResponseDocument;
import noNamespace.LoadDocumentSearchResponseDocument.LoadDocumentSearchResponse;
import noNamespace.LoadPartySearchDocument;
import noNamespace.LoadPartySearchDocument.LoadPartySearch;
import noNamespace.LoadPartySearchResponseDocument;
import noNamespace.LoadPartySearchResponseDocument.LoadPartySearchResponse;
import noNamespace.LoadPropertySearchDocument;
import noNamespace.LoadPropertySearchDocument.LoadPropertySearch;
import noNamespace.LoadPropertySearchResponseDocument;
import noNamespace.LoadPropertySearchResponseDocument.LoadPropertySearchResponse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.datacontract.schemas._2004._07.register_webservices.AccountInfo;
import org.datacontract.schemas._2004._07.register_webservices.ArrayOfDocumentImage;
import org.datacontract.schemas._2004._07.register_webservices.DocumentImage;
import org.datacontract.schemas._2004._07.register_webservices.DocumentImages;
import org.datacontract.schemas._2004._07.register_webservices.SearchResults;
import org.datacontract.schemas._2004._07.register_webservices.SearchValues;
import org.datacontract.schemas._2004._07.register_webservices.UserAccount;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.tnhamiltonro.RecordSearchStub;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TiffConcatenator;

public class TNHamiltonROConnection {
	
	protected static final Category logger = Logger.getLogger(TNHamiltonROConnection.class);
	
	private static final String BASE = BaseServlet.REAL_PATH /* "D:/workspace2/TS_main/web"*/ + "/WEB-INF/";
	private static final String AXIS2FILE = BASE+"conf/axis2.xml";
	
	private Search search;
	private DataSite dataSite;
	
	private final RecordSearchStub stub;
	private static ConfigurationContext ctx;
	
	private String appLoginKey = null; 
	private UserAccount userAcount = null;
	
	static{
		try {
			ctx  = ConfigurationContextFactory.createConfigurationContextFromFileSystem( BASE, AXIS2FILE );
		} catch (AxisFault e) {
			e.printStackTrace();
		}
	}
	
	public TNHamiltonROConnection(DataSite data, Search search) throws RemoteException {
		if (data == null) {
			throw new RuntimeException("Please provide good sites information [data = null] !!");
		}
		if (search == null) {
			throw new RuntimeException("Please provide good search [search = null] !!");
		}
		this.search = search;
		this.dataSite = data;
		int commId = search.getSa().getCommId();
		
		String userName = SitesPasswords.getInstance().
				getPasswordValue(commId, "TNHamiltonRO","user"); //"TENNSE01";
		String password = SitesPasswords.getInstance().
				getPasswordValue(commId, "TNHamiltonRO","password"); //"LAND162";
		String appLoginKey = SitesPasswords.getInstance().
				getPasswordValue(commId, "TNHamiltonRO","appLoginKey"); //"BB204A8E-68E1-4F1F-B0C4-ACC84519710D";
		
		if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(appLoginKey)) {
			throw new RuntimeException("Please fill user, password and domain in Password Settings (at least one is empty)");
		}
		
		
		stub = new RecordSearchStub(ctx, dataSite.getLink());
				
		AuthenticateRemoteUserDocument authenticateRemoteUserDoc = AuthenticateRemoteUserDocument.Factory.newInstance();
		AuthenticateRemoteUser authenticateRemoteUser = authenticateRemoteUserDoc.addNewAuthenticateRemoteUser();
		authenticateRemoteUser.setUserName(userName);
		authenticateRemoteUser.setPassword(password);
		
		AuthenticateRemoteUserResponseDocument authenticateRemoteUserResponseDocument = 
				stub.authenticate_RemoteUser(authenticateRemoteUserDoc);
		AuthenticateRemoteUserResponse authenticateRemoteUserResponse = authenticateRemoteUserResponseDocument.getAuthenticateRemoteUserResponse();
		this.userAcount = authenticateRemoteUserResponse.getAuthenticateRemoteUserResult();
		this.appLoginKey = appLoginKey;
		
	}
	
	public TNHamiltonROConnection() throws AxisFault {
		stub = new RecordSearchStub(ctx, "http://regssl.hamiltontn.gov/RecordSearch/Services/RecordSearch.svc");
	}

	public static void main(String[] args) {
		
		try {
			
			String userName = "TENNSE01";
			String password = "LAND162";
			
			TNHamiltonROConnection conn = new TNHamiltonROConnection();
			
			
			AuthenticateRemoteUserDocument authenticateRemoteUserDoc = AuthenticateRemoteUserDocument.Factory.newInstance();
			AuthenticateRemoteUser authenticateRemoteUser = authenticateRemoteUserDoc.addNewAuthenticateRemoteUser();
			authenticateRemoteUser.setUserName(userName);
			authenticateRemoteUser.setPassword(password);
			
			AuthenticateRemoteUserResponseDocument authenticateRemoteUserResponseDocument = 
					conn.stub.authenticate_RemoteUser(authenticateRemoteUserDoc);
			AuthenticateRemoteUserResponse authenticateRemoteUserResponse = authenticateRemoteUserResponseDocument.getAuthenticateRemoteUserResponse();
			UserAccount authenticateRemoteUserResult = authenticateRemoteUserResponse.getAuthenticateRemoteUserResult();
			
			AccountInfo addNewAccountInfo = null;
			
//			LoadPartySearchDocument load_PartySearch = LoadPartySearchDocument.Factory.newInstance();
//			LoadPartySearch loadPartySearch = load_PartySearch.addNewLoadPartySearch();
//			addNewAccountInfo = loadPartySearch.addNewAccountInfo();
			
			LoadBookPageSearchDocument load_BookPageSearch = LoadBookPageSearchDocument.Factory.newInstance();
			LoadBookPageSearch loadBookPageSearch = load_BookPageSearch.addNewLoadBookPageSearch();
			addNewAccountInfo = loadBookPageSearch.addNewAccountInfo();
			
			
			
			addNewAccountInfo.setAppLoginKey("BB204A8E-68E1-4F1F-B0C4-ACC84519710D");
			addNewAccountInfo.setUserAccountID(authenticateRemoteUserResult.getUserAccountsID());
			addNewAccountInfo.setUserLoginKey(authenticateRemoteUserResult.getUserLoginKey());
			
			
//			SearchValues searchValues = loadPartySearch.addNewSearchValues();
			SearchValues searchValues = loadBookPageSearch.addNewSearchValues();
			
			initDefaultValues(searchValues);
			
			/*
			searchValues.setFirstName("david");
			searchValues.setFromDate("07/01/1969");
			searchValues.setSearchType("PartySearch");
			searchValues.setLastName("Smith");
			*/
			searchValues.setBookNo(2093);
			searchValues.setBookTypeID(1);
			searchValues.setPageNo(147);
			searchValues.setSearchType("BookPage");
			searchValues.unsetInstrumentNo();
//			searchValues.setNilBlock();
//			
////			searchValues.setNilFirstName();
//			searchValues.setNilGroup();
////			searchValues.setNilLastName();
//			searchValues.setNilLot();
//			searchValues.setNilMap();
////			searchValues.setNilMiddleName();
//			searchValues.setNilParcel();
//			searchValues.setNilSearchDate();
//			searchValues.setNilStreetDir();
//			searchValues.setNilStreetName();
//			searchValues.setNilStreetNo();
//			searchValues.setNilSubdivision();
//			searchValues.setNilUnit();
			
			
			
			
//			LoadPartySearchResponseDocument load_PartySearchResponseDocument = conn.stub.load_PartySearch(load_PartySearch );
//			LoadPartySearchResponse loadPartySearchResponse = load_PartySearchResponseDocument.getLoadPartySearchResponse();
//			SearchResults loadPartySearchResult = loadPartySearchResponse.getLoadPartySearchResult();
			
			LoadBookPageSearchResponseDocument load_BookPageSearchResponseDocument = conn.stub.load_BookPageSearch(load_BookPageSearch);
			LoadBookPageSearchResponse loadBookPageSearchResponse = load_BookPageSearchResponseDocument.getLoadBookPageSearchResponse();
			SearchResults loadPartySearchResult = loadBookPageSearchResponse.getLoadBookPageSearchResult();
			
			System.out.println("Succes " + loadPartySearchResult.isSetIsSuccessful());
			System.out.println("Count " + loadPartySearchResult.getItemCount());
			System.out.println("end");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize default value for each field<br>
	 * Does NOT set searchType which should be set by each search
	 * @param searchValues
	 */
	private static void initDefaultValues(SearchValues searchValues) {
		searchValues.setBlock(null);
		searchValues.setBookNo(0);
		searchValues.setBookTypeID(0);
		searchValues.setCityID(0);
		searchValues.setDocGroupID(0);
		searchValues.setDocTypeID(0);
//		searchValues.setDocTypeOf(null);
		searchValues.setExactNameSrch(false);
		searchValues.setFirstItemID(0);
		//searchValues.setFirstName(null);
//		searchValues.setNilFromDate();
		searchValues.setGroup(null);
		searchValues.setInstrumentNo(null);
		searchValues.setIsReport(false);
		searchValues.setLastItemID(0);
		//searchValues.setLastName(null);
		searchValues.setLot(null);
		searchValues.setMap(null);
		//searchValues.setMiddleName(null);
		searchValues.setPageNo(0);
		searchValues.setParcel(null);
		searchValues.setPartyType(0);
		searchValues.setSearchDate(null);
		//searchValues.setSearchType(searchType); 		//To be set by user
		searchValues.setStreetDir(null);
		searchValues.setStreetName(null);
		searchValues.setStreetNo(null);
		searchValues.setSubdivision(null);
//		searchValues.setThruDate(null);
		searchValues.setUnit(null);
		
	}

	public SearchResults searchByPersonName(TSServerInfoModule module) {
		if(module == null || module.getModuleIdx() != TSServerInfo.NAME_MODULE_IDX) {
			return null;
		}
		
		
		LoadPartySearchDocument loadSearchDocument = LoadPartySearchDocument.Factory.newInstance();
		LoadPartySearch loadSearch = loadSearchDocument.addNewLoadPartySearch();
		AccountInfo addNewAccountInfo = loadSearch.addNewAccountInfo();
		
		/**
		 * 
		 */
		addNewAccountInfo.setAppLoginKey(this.appLoginKey);
		addNewAccountInfo.setUserAccountID(this.userAcount.getUserAccountsID());
		addNewAccountInfo.setUserLoginKey(this.userAcount.getUserLoginKey());
		
		SearchValues searchValues = loadSearch.addNewSearchValues();
		
		String temp = module.getParamValue(6).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocGroupID(Integer.parseInt(temp));
		}
		temp = module.getParamValue(7).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocTypeID(Integer.parseInt(temp));
		}
		temp = module.getParamValue(11).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocTypeOf(temp);
		}
		temp = module.getParamValue(8).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setPartyType(Integer.parseInt(temp));
		}
		temp = module.getParamValue(9).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setExactNameSrch(true);
		} else {
			searchValues.setExactNameSrch(false);
		}
		
		searchValues.setFirstName(module.getParamValue(1).trim());
		searchValues.setLastName(module.getParamValue(0).trim());
		searchValues.setMiddleName(module.getParamValue(3).trim());
		
		searchValues.setInstrumentNo(module.getParamValue(10).trim());		
		
		searchValues.setSearchType(module.getParamValue(2).trim());
		searchValues.setFromDate(module.getParamValue(4).trim());
		searchValues.setThruDate(module.getParamValue(5).trim());
		
		searchValues.setLastItemID(Integer.parseInt(module.getParamValue(12).trim()));
		
		
		LoadPartySearchResponseDocument loadSearchResponseDocument;
		SearchResults loadSearchResult = null;
		try {
			loadSearchResponseDocument = stub.load_PartySearch(loadSearchDocument);
			LoadPartySearchResponse loadSearchResponse = loadSearchResponseDocument.getLoadPartySearchResponse();
			loadSearchResult = loadSearchResponse.getLoadPartySearchResult();
		} catch (RemoteException e) {
			e.printStackTrace();
			logger.error("Error while performing name search", e);
		}
		
		
		return loadSearchResult;
	}

	public SearchResults searchByBookPage(TSServerInfoModule module) {
		
		if(module == null || module.getModuleIdx() != TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
			return null;
		}
		
		int book = Integer.parseInt(module.getParamValue(0).trim());
		int page = Integer.parseInt(module.getParamValue(1).trim());
		int bookTypeId = Integer.parseInt(module.getParamValue(2).trim());
		
		
		LoadBookPageSearchDocument load_BookPageSearch = LoadBookPageSearchDocument.Factory.newInstance();
		LoadBookPageSearch loadBookPageSearch = load_BookPageSearch.addNewLoadBookPageSearch();
		AccountInfo addNewAccountInfo = loadBookPageSearch.addNewAccountInfo();
		
		/**
		 * 
		 */
		addNewAccountInfo.setAppLoginKey(this.appLoginKey);
		addNewAccountInfo.setUserAccountID(this.userAcount.getUserAccountsID());
		addNewAccountInfo.setUserLoginKey(this.userAcount.getUserLoginKey());
		
		SearchValues searchValues = loadBookPageSearch.addNewSearchValues();
		
		searchValues.setBookNo(book);
		searchValues.setBookTypeID(bookTypeId);
		searchValues.setPageNo(page);
		searchValues.setSearchType("BookPage");
		
		LoadBookPageSearchResponseDocument load_BookPageSearchResponseDocument;
		SearchResults loadPartySearchResult = null;
		try {
			load_BookPageSearchResponseDocument = stub.load_BookPageSearch(load_BookPageSearch);
			LoadBookPageSearchResponse loadBookPageSearchResponse = load_BookPageSearchResponseDocument.getLoadBookPageSearchResponse();
			loadPartySearchResult = loadBookPageSearchResponse.getLoadBookPageSearchResult();
		} catch (RemoteException e) {
			e.printStackTrace();
			logger.error("Error while performing book-page search", e);
		}
		
		
		return loadPartySearchResult;
	}

	public SearchResults searchByInstrument(TSServerInfoModule module) {
		if(module == null || module.getModuleIdx() != TSServerInfo.INSTR_NO_MODULE_IDX) {
			return null;
		}
		
		
		LoadDocumentSearchDocument loadSearchDocument = LoadDocumentSearchDocument.Factory.newInstance();
		LoadDocumentSearch loadSearch = loadSearchDocument.addNewLoadDocumentSearch();
		AccountInfo addNewAccountInfo = loadSearch.addNewAccountInfo();
		
		/**
		 * 
		 */
		addNewAccountInfo.setAppLoginKey(this.appLoginKey);
		addNewAccountInfo.setUserAccountID(this.userAcount.getUserAccountsID());
		addNewAccountInfo.setUserLoginKey(this.userAcount.getUserLoginKey());
		
		SearchValues searchValues = loadSearch.addNewSearchValues();
		
		searchValues.setInstrumentNo(module.getParamValue(0).trim());
		String temp = module.getParamValue(6).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocGroupID(Integer.parseInt(temp));
		}
		temp = module.getParamValue(2).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocTypeID(Integer.parseInt(temp));
		}
		temp = module.getParamValue(4).trim();
		if(StringUtils.isNotEmpty(temp)) {
			searchValues.setDocTypeOf(temp);
		}
		searchValues.setSearchType(module.getParamValue(1).trim());
		searchValues.setFromDate(module.getParamValue(3).trim());
		searchValues.setThruDate(module.getParamValue(5).trim());
		
		
		LoadDocumentSearchResponseDocument loadSearchResponseDocument;
		SearchResults loadSearchResult = null;
		try {
			loadSearchResponseDocument = stub.load_DocumentSearch(loadSearchDocument);
			LoadDocumentSearchResponse loadSearchResponse = loadSearchResponseDocument.getLoadDocumentSearchResponse();
			loadSearchResult = loadSearchResponse.getLoadDocumentSearchResult();
		} catch (RemoteException e) {
			e.printStackTrace();
			logger.error("Error while performing instrument search", e);
		}
		
		
		return loadSearchResult;
	}

	public SearchResults searchByProperty(TSServerInfoModule module) {
		if(module == null || 
				(module.getModuleIdx() != TSServerInfo.ADDRESS_MODULE_IDX && 
						module.getModuleIdx() != TSServerInfo.PARCEL_ID_MODULE_IDX &&
						module.getModuleIdx() != TSServerInfo.SUBDIVISION_MODULE_IDX)) {
			return null;
		}
		
		
		LoadPropertySearchDocument loadSearchDocument = LoadPropertySearchDocument.Factory.newInstance();
		LoadPropertySearch loadSearch = loadSearchDocument.addNewLoadPropertySearch();
		AccountInfo addNewAccountInfo = loadSearch.addNewAccountInfo();
		
		/**
		 * 
		 */
		addNewAccountInfo.setAppLoginKey(this.appLoginKey);
		addNewAccountInfo.setUserAccountID(this.userAcount.getUserAccountsID());
		addNewAccountInfo.setUserLoginKey(this.userAcount.getUserLoginKey());
		
		SearchValues searchValues = loadSearch.addNewSearchValues();
		
		if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX) {
			searchValues.setStreetNo(module.getParamValue(5).trim());
			searchValues.setStreetName(module.getParamValue(0).trim());
			searchValues.setStreetDir(module.getParamValue(6).trim());
			
			searchValues.setSearchType(module.getParamValue(1).trim());
			searchValues.setFromDate(module.getParamValue(2).trim());
			searchValues.setThruDate(module.getParamValue(3).trim());
			searchValues.setLastItemID(Integer.parseInt(module.getParamValue(4).trim()));
		} else if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) {
			
			searchValues.setMap(module.getParamValue(0).trim());
			searchValues.setGroup(module.getParamValue(1).trim());
			searchValues.setParcel(module.getParamValue(2).trim());
			
			searchValues.setFromDate(module.getParamValue(3).trim());
			searchValues.setThruDate(module.getParamValue(4).trim());
			searchValues.setLastItemID(Integer.parseInt(module.getParamValue(5).trim()));
		} else if(module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX){
			String temp = module.getParamValue(3).trim();
			if(StringUtils.isEmpty(temp)) {
				searchValues.setCityID(0);
			} else {
				searchValues.setCityID(Integer.parseInt(temp));	
			}
			
			searchValues.setSubdivision(module.getParamValue(4).trim());
			searchValues.setLot(module.getParamValue(6).trim());
			searchValues.setBlock(module.getParamValue(5).trim());
			searchValues.setUnit(module.getParamValue(7).trim());
			
			searchValues.setSearchType(module.getParamValue(0).trim());
			searchValues.setFromDate(module.getParamValue(1).trim());
			searchValues.setThruDate(module.getParamValue(2).trim());
			searchValues.setLastItemID(Integer.parseInt(module.getParamValue(8).trim()));
		}
		
		LoadPropertySearchResponseDocument loadSearchResponseDocument;
		SearchResults loadSearchResult = null;
		try {
			loadSearchResponseDocument = stub.load_PropertySearch(loadSearchDocument);
			LoadPropertySearchResponse loadSearchResponse = loadSearchResponseDocument.getLoadPropertySearchResponse();
			loadSearchResult = loadSearchResponse.getLoadPropertySearchResult();
		} catch (RemoteException e) {
			e.printStackTrace();
			logger.error("Error while performing property search", e);
		}
		
		
		return loadSearchResult;
	}

	public byte[] getImage(int documentID, int documentTypeID) {
		LoadDocumentImagesDocument loadDocumentImagesDocument = LoadDocumentImagesDocument.Factory.newInstance();
		LoadDocumentImages loadDocumentImages = loadDocumentImagesDocument.addNewLoadDocumentImages();
		AccountInfo addNewAccountInfo = loadDocumentImages.addNewAccountInfo();
		addNewAccountInfo.setAppLoginKey(this.appLoginKey);
		addNewAccountInfo.setUserAccountID(this.userAcount.getUserAccountsID());
		addNewAccountInfo.setUserLoginKey(this.userAcount.getUserLoginKey());
		
		loadDocumentImages.setDocumentID(documentID);
		loadDocumentImages.setDocumentTypeID(2);
		
		try {
			LoadDocumentImagesResponseDocument load_DocumentImages = stub.load_DocumentImages(loadDocumentImagesDocument);
			LoadDocumentImagesResponse loadDocumentImagesResponse = load_DocumentImages.getLoadDocumentImagesResponse();
			DocumentImages loadDocumentImagesResult = loadDocumentImagesResponse.getLoadDocumentImagesResult();
			if(loadDocumentImagesResult != null && loadDocumentImagesResult.getIsSuccessful()) {
				ArrayOfDocumentImage imageList = loadDocumentImagesResult.getImageList();
				
				if(imageList != null && imageList.sizeOfDocumentImageArray() > 0) {
					DocumentImage[] documentImageArray = imageList.getDocumentImageArray();
					
					List<byte[]> pageFiles = new ArrayList<byte[]>(documentImageArray.length);
					for (DocumentImage documentImage : documentImageArray) {
						pageFiles.add(documentImage.getBinaryItem());
					}
					
					return TiffConcatenator.concatePngInTiff(pageFiles);
				}
				
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			logger.error("Error while performing image search", e);
		}
		
		
		return null;
	}

}
