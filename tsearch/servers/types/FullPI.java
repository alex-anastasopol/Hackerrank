package ro.cst.tsearch.servers.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn.ImageDownloadResponse;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class FullPI extends GenericPI{

	private static final long serialVersionUID = 8863687363582028102L;

	public FullPI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public FullPI(long searchId) {
		super(searchId);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if(tsServerInfoModule != null) {
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
				
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					
					
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(
							siteName, TSServerInfo.PARCEL_ID_MODULE_IDX, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
				
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					
					
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(
							siteName, TSServerInfo.ADDRESS_MODULE_IDX, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
				
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.INSTR_NO_MODULE_IDX, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		return msiServerInfoDefault;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			String pid = sa.getAtribute(SearchAttributes.LD_PARCELNO);
			if(StringUtils.isBlank(pid)){
				pid = sa.getAtribute(SearchAttributes.LD_PARCELNO2);
			}
			
			if(StringUtils.isBlank(pid)){
				pid = sa.getAtribute(SearchAttributes.LD_PARCELNONDB);
			}
			
			if(StringUtils.isNotBlank(pid)){
				pid = pid.replaceAll("[ -]", "");
				sa.setAtribute(SearchAttributes.LD_PARCELNO2_ALTERNATE, pid);
			}
			
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, global.getID(), null);
			nameFilterOwner.setInitAgain(true);
			FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter(searchId);
//			LastTransferOrMortgageDateFilter lastTransferDateFilter 	= new LastTransferOrMortgageDateFilter(searchId);
			PinFilterResponse pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO2_ALTERNATE, searchId);
			pinFilter.setStartWith(true);
			pinFilter.setIgNoreZeroes(true);
			
			GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
			addressFilter.setEnableUnit(false);
			
//			FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
			
			FilterResponse[] filtersO 	= { nameFilterOwner};
			FilterResponse[] filters 	= { legalFilter, pinFilter };
//			FilterResponse[] filtersRef = { pinFilter, addressFilter, legalFilter };
			
			{
//				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
//				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//		    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
//				module.addExtraInformation("COUNTY_NAME", countyName);
//	
//				InstrumentGenericIterator instrumentNoInterator = getInstrumentIterator();
//				instrumentNoInterator.setLoadFromRoLike(false);
//				module.addIterator(instrumentNoInterator);
//				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
//				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
//				for(int i = 0; i < filtersRef.length; i++){
//			    	module.addFilter(filtersRef[i]);
//			    }
//	
//				modules.add(module);
			}
			
			if(StringUtils.isNotBlank(pid)){
				Collection<String> pins = getSearchAttributes().getPins(-1);
				if(pins.size() > 0){
					if (pins.size() < 4){
						for(String pin: pins){
							TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
							module.clearSaKeys();
							
							pin = pin.replaceAll("(?is)\\p{Punct}", "");
							
							String area = pin.substring(0, 2);
							String section = pin.substring(2, 4);
							String block = pin.substring(4, 7);
							String parcel = pin.substring(7, 10);
							String unit = "";
							if (pin.length() >= 14){
								unit = pin.substring(10, 14);
							}
							if (getDataSite().getCountyId() == CountyConstants.IL_Will){
								area = pin.substring(2, 4);
								section = pin.substring(4, 6);
								block = pin.substring(6, 9);
								parcel = pin.substring(9, 12);
								unit = StringUtils.stripStart(pin.substring(12, 16), "0");
							}
							module.forceValue(0, StringUtils.stripStart(area, "0"));
							module.forceValue(1, StringUtils.stripStart(section, "0"));
							module.forceValue(2, StringUtils.stripStart(block, "0"));
							module.forceValue(3, StringUtils.stripStart(parcel, "0"));
							if (StringUtils.isNotEmpty(unit)){
								module.forceValue(4, unit);
							}
							//module.addFilter(addressFilter);
						    modules.add(module);
						}
					} else{
						SearchLogger.logWithServerName("More than three PINs, search skipped!", searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
					}
				}
			}
			
			{
//				String[] relatedSourceDoctype = new String[]{
//						DocumentTypes.ASSIGNMENT,
//						DocumentTypes.APPOINTMENT,
//						DocumentTypes.MODIFICATION,
//						DocumentTypes.RELEASE,
//						DocumentTypes.SUBORDINATION
//						};
//				
//				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
//				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//						"Searching (related) with instrument list from all " + Arrays.toString(relatedSourceDoctype));
//				module.addExtraInformation("COUNTY_NAME", countyName);
//							
//				DocumentResaverIterator instrumentResaverIterator = new DocumentResaverIterator(searchId){
//					private static final long serialVersionUID = 1L;
//					
//					@Override
//					protected void loadDerrivation(TSServerInfoModule module, InstrumentI instrument) {
//						for (Object functionObject : module.getFunctionList()) {
//							if (functionObject instanceof TSServerInfoFunction) {
//								TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
//								if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE) {
//									function.setParamValue(getInstrumentNoFrom(instrument.getInstno(), instrument.getYear(), module.getExtraInformation("COUNTY_NAME")));
//								} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_YEAR) {
//									function.setParamValue(getYearFrom(instrument.getInstno(), instrument.getYear(),  module.getExtraInformation("COUNTY_NAME")));
//								}
//							}
//						}
//						
//					}
//					
//				};
//				
//				instrumentResaverIterator.setRoDoctypesToLoad(relatedSourceDoctype);
//				module.addIterator(instrumentResaverIterator);
//				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
//				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
//		
//				modules.add(module);
			}
			
			addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO);
			
			 // OCR last transfer - instrument search
			//addOCRSearch(modules, serverInfo, filters);
			{
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			
			    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			    module.clearSaKeys();
			    
			    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_SKLD_INSTRUMENT_FIRST_PART);
			    module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_SKLD_INSTRUMENT_SECOND_PART);
			    
			    OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId, getDataSite()) {
			    	private static final long serialVersionUID = 1L;
			    	
			    	@Override
			    	public List<Instrument> extractInstrumentNoList(TSServerInfoModule initial) {
			    		List<Instrument> extractInstrumentNoList = super.extractInstrumentNoList(initial);
			    		
			    		for (Instrument instrument : extractInstrumentNoList) {
							String inst = instrument.getInstrumentNo();
							if(StringUtils.isNotEmpty(inst)){
								if (getDataSite().getCountyId() == CountyConstants.IL_Cook){
									inst = StringUtils.stripStart(inst, "0");
									instrument.setInstrumentNo(inst + "-0000");
								} else if (getDataSite().getCountyId() == CountyConstants.IL_Du_Page){
									if (inst.startsWith("R")){
										inst = inst.replaceFirst("R", "");
										inst = inst.replaceAll("\\p{Punct}", "");
										
										String year = "";
										if (inst.startsWith("2")){
											year = inst.substring(0, 4);
											inst = StringUtils.stripStart(inst.substring(4), "0");
										} else {
											year = "19" + inst.substring(0, 2);
											inst = StringUtils.stripStart(inst.substring(2), "0");
										}
										instrument.setInstrumentNo(inst + "-" + year);
									}
								} else if (getDataSite().getCountyId() == CountyConstants.IL_Kendall){
									String year = inst.substring(0, 4);
									inst = StringUtils.stripStart(inst.substring(4), "0");
									instrument.setInstrumentNo(inst + "-" + year);
								} else if (getDataSite().getCountyId() == CountyConstants.IL_Lake){
										inst = StringUtils.stripStart(inst, "0");
										instrument.setInstrumentNo(inst + "-0000");
								} else if (getDataSite().getCountyId() == CountyConstants.IL_McHenry){
									if (inst.contains("R")){
										inst = inst.replaceAll("\\p{Punct}", "");
										
										String year = "";
										if (inst.indexOf("R") == 4){
											year = inst.substring(0, 4);
											inst = StringUtils.stripStart(inst.substring(5), "0");
										}
										instrument.setInstrumentNo(inst + "-" + year);
									} else {
										instrument.setInstrumentNo(inst + "-0000");
									}
								} else if (getDataSite().getCountyId() == CountyConstants.IL_Will){
									if (inst.startsWith("R")){
										inst = inst.replaceFirst("R", "");
										inst = inst.replaceAll("\\p{Punct}", "");
										if (inst.startsWith("20")){
											String year = inst.substring(0, 4);
											inst = StringUtils.stripStart(inst.substring(4), "0");
											instrument.setInstrumentNo(inst + "-" + year);
										} else {
											String year = inst.substring(0, 2);
											inst = StringUtils.stripStart(inst.substring(2), "0");
											instrument.setInstrumentNo(inst + "-19" + year);
										}
									}
								} else{
									String year = "";
									if (inst.startsWith("2")){
										year = inst.substring(0, 4);
										inst = inst.substring(4);
										inst = inst.replaceAll("(?is)\\A[A-Z]", "");
										inst = StringUtils.stripStart(inst, "0");
									} else {
										year = "19" + inst.substring(0, 2);
										inst = inst.substring(2);
										inst = inst.replaceAll("(?is)\\A[A-Z]", "");
										inst = StringUtils.stripStart(inst, "0");
									}
									instrument.setInstrumentNo(inst + "-" + year);
								}
							}
						}
			    		setSearchIfPresent(false);
			    		return extractInstrumentNoList;
			    	}
			    	
			    };
			    
			    ocrBPIteratoriterator.setInitAgain(true);
		    	module.addIterator(ocrBPIteratoriterator);
		    	
			    for(int i = 0; i < filters.length; i++){
			    	module.addFilter(filters[i]);
			    }
			    addBetweenDateTest(module, false, false, false);
				modules.add(module);
			}
		}
		serverInfo.setModulesForAutoSearch(modules);	
   }
	
	protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		//module.clearSaKeys();
		module.setSaObjKey(key);
		
		for (int i = 0; i < filters.length; i++) {
			module.addFilter(filters[i]);
		}
		addBetweenDateTest(module, false, true, true);
		addFilterForUpdate(module, true);

		module.setIteratorType( 0,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		
	
		modules.add( module );
		return searchedNames;
	}
	
	private InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId, getDataSite()) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				
//				if (getDataSite().getCountyId() == CountyConstants.IL_Will){
//					String instNo = inst.replaceFirst("^R+", "");
//					if(instNo.isEmpty()) {
//						return instNo;
//					}
//					instNo = instNo.replaceAll("(?is)\\p{Punct}", "");
//					
//					if (instNo.startsWith("2")){
//						instNo = instNo.substring(4, instNo.length());
//					} else {
//						instNo = instNo.substring(2, instNo.length());
//					}
//					
//					instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
//					
//					return instNo;
//				}
				
				return inst;
			}
			
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instNo = state.getInstno();
				
				instNo = getInstrNoFrom(instNo, state.getYear());
				
				return instNo;
			}
			@Override
			public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instNo = state.getInstno();
				
				String year = getYear(instNo, state.getYear());
				
				return year;
			}
		};
		return iterator;
	}
	
	@Override
	public ImageDownloadResponse downloadImageResponse(Map<String, String> params, PropertyInsightImageConn imagesConn){
	 	
 		ImageDownloadResponse imageresponse = null;
		try {
			String imageId = params.get("imageId");
			if (StringUtils.isNotEmpty(imageId)){
				imageId = prepareImageId(imageId);
				imageresponse = imagesConn.getDocumentsByParameters2(FLGenericDASLDT.getBasePiQuery(searchId) + ",Type=Rec,SubType=All," + imageId);
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,Type=STR,SubType=PLANT,Year=2003,Order=ST5050464,Flag=ST");
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,Type=REC,SubType=ALL,Year=1989");
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,County=ILLK,Type=STR,SubType=PLANT,DATE=19991011,ORDER=632042,CMP=CTI,CMP3=CTI,DOC=STR,FLAG=ST");

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imageresponse;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image)  throws ServerResponseException{
	   
		TSServerInfo info = getDefaultServerInfo();
	   	TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
	    String imageId = "";
		String link = image.getLink();
		int poz = link.indexOf("?");
		
		if(poz>0){
			link = link.substring(poz+1);
		}
		
		String[] allParameters = link.split("[&=]");
		
		for(int i=0;i<allParameters.length-1;i+=2){
			if("imageId".equalsIgnoreCase(allParameters[i])){
				imageId = allParameters[i+1];
			}
		}
	   	imageId = prepareImageId(imageId);
	   	module.setParamValue( 0, imageId );
	  
	   
	   	String imageName = image.getPath();
	   	if(FileUtils.existPath(imageName)){
	   		byte b[] = FileUtils.readBinaryFile(imageName);
	   		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
	   	}
	   	
	   	ServerResponse response = SearchBy(module, null);
	   	DownloadImageResult res = response.getImageResult();
	   	return res;
	   	
	   }
	
	public String prepareImageId(String imageId){
		
		imageId = imageId.replaceAll("@", "=");
		if (imageId.contains(":")){
			imageId = imageId.replaceFirst("[^:]+:(.*\\s+)", "Year=$1").replaceFirst("\\s+", ",Inst=");
		}
		
		return imageId;
	}
	
	@Override
	public String cleanHtmlBeforeSavingDocument(String htmlContent){
		
		htmlContent = htmlContent.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*<input[^>]*>\\s*</td>\\s*<td[^>]*>\\s*(<b>\\s*)?PI\\s*(</b>\\s*)?</td>", "");
		return htmlContent;
	}
	
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(!ocrReportedBookPage){
			
			String inst = StringUtils.defaultString(in.getInstno());
			if(inst.contains("-")){				
				inst = inst.replaceAll("[-]+", "");
				
				in.setInstno(inst);
			}
		}
		
	}
	
	public String getInstrNoFrom(String instNo, int year) {
		
		if (getDataSite().getCountyId() == CountyConstants.IL_Cook){
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
		} else if (getDataSite().getCountyId() == CountyConstants.IL_DeKalb){

				instNo = instNo.substring(4);
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
			
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Du_Page){
			if (instNo.startsWith("R")){
				instNo = instNo.replaceFirst("^R+", "");
				instNo = instNo.substring(4);
				
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");

			} else{
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Kane){
			if (instNo.contains("K")){
				instNo = instNo.substring(instNo.indexOf("K") + 1);
				
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");

			} else{
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Kendall){
			if (year != SimpleChapterUtils.UNDEFINED_YEAR){
				String yearS = Integer.toString(year);
				if (instNo.startsWith(yearS)){
					instNo = instNo.replace(yearS, "");
					instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
				}
			} else {
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo.substring(4), "0");
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_McHenry){
			if (instNo.contains("R")){
				instNo = instNo.substring(instNo.indexOf("R") + 1);
				
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");

			} else{
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Will){
			if (instNo.startsWith("R")){
				instNo = instNo.replaceFirst("^R+", "");
				
				instNo = instNo.replaceAll("(?is)\\p{Punct}", "");
				
				if (instNo.startsWith("2")){
					instNo = instNo.substring(4);
				} else {
					instNo = instNo.substring(2);
				}
				
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");

			} else{
				instNo = org.apache.commons.lang.StringUtils.stripStart(instNo, "0");
			}
		} 
		
		return instNo;
	}

	public String getYear(String instNo, int intYear) {

		String year = "";
		if (getDataSite().getCountyId() == CountyConstants.IL_DeKalb){
			year = instNo.substring(0, 4);
			
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Du_Page){
			if (instNo.startsWith("R")){
				instNo = instNo.replaceFirst("^R+", "");
				year =  org.apache.commons.lang.StringUtils.stripStart(instNo.substring(0, 4), "0");
			} 
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Kane){
			if (instNo.contains("K")){
				year = instNo.substring(0, instNo.indexOf("K"));
				
				if (year.length() == 2){
					year = "19" + year;
				}

			} else {
				year = Integer.toString(intYear);
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Kendall){
			if (intYear != SimpleChapterUtils.UNDEFINED_YEAR){
				year =  Integer.toString(intYear);
			} else {
				year = instNo.substring(0, 4);
			}
			
		} else if (getDataSite().getCountyId() == CountyConstants.IL_McHenry){
			if (instNo.contains("R")){
				year = instNo.substring(0, instNo.indexOf("R"));

			} else {
				year = Integer.toString(intYear);
			}
		} else if (getDataSite().getCountyId() == CountyConstants.IL_Will){
			if (instNo.startsWith("R")){
				instNo = instNo.replaceFirst("^R+", "");
				
				instNo = instNo.replaceAll("(?is)\\p{Punct}", "");
				
				if (instNo.startsWith("2")){
					year =  org.apache.commons.lang.StringUtils.stripStart(instNo.substring(0, 4), "0");
				} else {
					year =  "19" + org.apache.commons.lang.StringUtils.stripStart(instNo.substring(0, 2), "0");
				}
			} 
		} else{
			year =  Integer.toString(intYear);
		}
		
		return year;
	}
}
