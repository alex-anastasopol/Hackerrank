package ro.cst.tsearch.servlet.parentsite;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.servers.types.GenericServerADI;
import ro.cst.tsearch.servers.types.ILCookLA;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.servers.types.GMServer;
import ro.cst.tsearch.servers.types.GenericPI;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServerDASL;
import ro.cst.tsearch.servers.types.TSServerDASLAdapter;
import ro.cst.tsearch.servers.types.TSServerROLike;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GoogleMapsStructure;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.ServletUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Plat;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class ParentSiteActions extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String SUBDIVISION_NAME_LOOKUP = "SUBDIVISION_NAME_LOOKUP";
	public static final String SUBDIVISION_SECONDARY_NAME_LOOKUP = "SUBDIVISION_SECONDARY_NAME_LOOKUP";
	public static final String RESTORE_DOCUMENTS = "RESTORE_DOCUMENTS";
	public static final String VIEW_IMAGE = "VIEW_IMAGE";
	public static final String SAVE_SEARCH_PARAMS = "SAVE_SEARCH_PARAMS";
	public static final String SUBC_FOR_CAT = "SUBC_FOR_CAT";
	
	
	
	@Override
	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		String isChangeMap = request.getParameter(RequestParams.GOOGLE_MAPS_CHANGE_MAP);
    	if ("true".equalsIgnoreCase(isChangeMap)) {
    		String searchIdString = request.getParameter(RequestParams.SEARCH_ID);
    		if (searchIdString!=null) {
    			long searchId = -1;
    			try {
    				searchId = Long.parseLong(searchIdString);
    			} catch (NumberFormatException nfe) {}
    			if (searchId!=-1) {
    				Search search = SearchManager.getSearch(searchId, false);
    				if (search==null) {
    					search = SearchManager.getSearchFromDisk(searchId);
    				}
    				if (search!=null) {
    					String instrumentNumber = request.getParameter(RequestParams.INSTRUMENT_NUMBER);
        				if (!StringUtils.isEmpty(instrumentNumber)) {
        					String latitudeString = request.getParameter(RequestParams.GOOGLE_MAPS_LATITUDE);
        					float latitude = -1.0f;
        					if (latitudeString!=null) {
        						try {
        							latitude = Float.parseFloat(latitudeString);
        		    			} catch (NumberFormatException nfe) {}
        					}
        					String longitudeString = request.getParameter(RequestParams.GOOGLE_MAPS_LONGITUDE);
        					float longitude = -1.0f;
        					if (longitudeString!=null) {
        						try {
        							longitude = Float.parseFloat(longitudeString);
        		    			} catch (NumberFormatException nfe) {}
        					}
        					String zoomString = request.getParameter(RequestParams.GOOGLE_MAPS_ZOOM);
        					int zoom = -1;
        					if (zoomString!=null) {
        						try {
        							zoom = Integer.parseInt(zoomString);
        		    			} catch (NumberFormatException nfe) {}
        					}
        					String typeId = request.getParameter(RequestParams.GOOGLE_MAPS_TYPE_ID);
        					if (latitude!=-1.0f&&longitude!=-1.0f&&zoom!=-1&&!StringUtils.isEmpty(typeId)) {
        						String isAlreadySaved = request.getParameter(RequestParams.GOOGLE_MAPS_IS_ALREADY_SAVED);
        						if ("true".equalsIgnoreCase(isAlreadySaved)) {
        							DocumentsManagerI manager = search.getDocManager();
        							if (manager!=null) {
        								DocumentI doc = null;
        								try {
        									manager.getAccess();
        									InstrumentI instrument = new Instrument(instrumentNumber, DocumentTypes.PLAT, "GoogleMap", -1);
        									ArrayList<RegisterDocumentI> list = manager.getRegisterDocuments(instrument, true);
        									if (list.size()>0) {
        										doc = list.get(0);
        									}
        								} finally{
        									manager.releaseAccess();
        								}
        								if (doc!=null) {
        									
        									String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
        									String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
        									Map<String, String> elevations = GMServer.getElevations(crtState + crtCounty + "GM", searchId, latitudeString + "," + longitudeString);
        									String elevation = elevations.get(latitudeString + "_" + longitudeString);
        									if (elevation==null) {
        										elevation = "0";
        									}
        									
        									if (doc instanceof Plat) {
        										Plat plat = (Plat)doc;
        										plat.setLatitude(latitudeString + "&#176;");
        										plat.setLongitude(longitudeString + "&#176;");
        										plat.setElevation(elevation + " m");
        									}
        									ImageI image = doc.getImage();
        									if (image!=null) {
        										String link = image.getLink(0);
        										if (!StringUtils.isEmpty(link)) {
        											link = link + "&";
        											link = link.replaceFirst("(?is)\\b" + "(center" + "=).+?(&)", "$1" + latitude + "," + longitude + "$2");
        											link = link.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_ZOOM + "=).+?(&)", "$1" + zoom + "$2");
        											link = link.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=).+?(&)", "$1" + typeId + "$2");
        											link = link.substring(0, link.length()-1);
        											Set<String> set = new HashSet<String>();
        											set.add(link);
        											image.setLinks(set);
        										}
        									}
        									String docIndex = DBManager.getDocumentIndex(doc.getIndexId());
        									if (docIndex!=null) {
        										String newDocIndex = docIndex;
        										int i1=newDocIndex.indexOf("<iframe");
        										if (i1!=-1) {
        											int i2=newDocIndex.indexOf("</iframe>", i1+1);
        											if (i2!=-1) {
        												String iframeText = newDocIndex.substring(i1, i2 + "</iframe>".length());
        												iframeText = iframeText.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_LATITUDE + "=).+?(&)", "$1" + latitudeString + "$2");
        												iframeText = iframeText.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_LONGITUDE + "=).+?(&)", "$1" + longitudeString + "$2");
        												iframeText = iframeText.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_ZOOM + "=).+?(&)", "$1" + zoom + "$2");
        												iframeText = iframeText.replaceFirst("(?is)\\b(" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=).+?(&)", "$1" + typeId + "$2");
        												newDocIndex = newDocIndex.substring(0,  i1) + iframeText + newDocIndex.substring(i2 + "</iframe>".length());
        											}
        										}
        										newDocIndex = newDocIndex.replaceFirst("(?is)(<td>\\s*<b>\\s*Latitude:\\s*</b>\\s*</td>\\s*<td>).*?(&deg;\\s*</td>)", "$1" + latitudeString + "$2");
        										newDocIndex = newDocIndex.replaceFirst("(?is)(<td>\\s*<b>\\s*Longitude:\\s*</b>\\s*</td>\\s*<td>).*?(&deg;\\s*</td>)", "$1" + longitudeString + "$2");
        										newDocIndex = newDocIndex.replaceFirst("(?is)(<td>\\s*<b>\\s*Elevation:\\s*</b>\\s*</td>\\s*<td>).*?( m\\s*</td>)", "$1" + elevation + "$2");
        										if (!newDocIndex.equals(docIndex)) {
        											doc.setIndexId( DBManager.addDocumentIndex(newDocIndex, search));
        										}
        									}
        								}
        							}
        						} else {
        							@SuppressWarnings("unchecked")
    								Map<String, GoogleMapsStructure> map = (Map<String, GoogleMapsStructure>)search.getAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY);
            						GoogleMapsStructure structure = new GoogleMapsStructure(latitude, longitude, zoom, typeId);
            						if (map==null) {
            							map = new HashMap<String, GoogleMapsStructure>();
            						}
            						map.put(instrumentNumber, structure);
            						search.setAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY, map);
        						}
        					}
        				}	
    				}
    				
    			}
    		}
    		return;
    	}
		
		ParameterParser parameterParser = new ParameterParser(request);
		long searchId = parameterParser.getLongParameter(RequestParams.SEARCH_ID);
		Search search = null;
		String opCodeString = parameterParser.getStringParameter(TSOpCode.OPCODE);
		
		if(opCodeString==null){
			
		} else if(opCodeString.equals(SUBDIVISION_NAME_LOOKUP)) {
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			String nameValue = null;
			try{nameValue = parameterParser.getStringParameter("doclink");}catch (Exception e) {	}
			try{nameValue = parameterParser.getStringParameter("docLink");}catch (Exception e) {	}
			
			if(nameValue != null) {
				
				if(nameValue.contains("DL__")){
					
					nameValue = nameValue.replace("&isSubResult=true", "");
					nameValue = nameValue.replaceFirst("&crossRefSource=[^&]+", "");

				        if (search.existsInMemoryDoc(nameValue) || 
				        		search.existsInMemoryDoc(nameValue.replaceAll("&parentSite=true", ""))) {
				        
				            Object item = (nameValue.indexOf("&parentSite=true") >= 0 ? 
				            		search.getInMemoryDoc(nameValue.replaceAll("&parentSite=true", "")) : 
				            			search.getInMemoryDoc(nameValue));
				            
				            search.removeInMemoryDoc(nameValue.indexOf("&parentSite=true") >= 0 ? 
				            		nameValue.replaceAll("&parentSite=true", "") : 
				            			nameValue);
				           
				            if(item instanceof ParsedResponse){
				            	ParsedResponse pr = (ParsedResponse)item;
				            	DocumentI doc = pr.getDocument();
				            	if(doc!=null && doc instanceof RegisterDocumentI){
				            		RegisterDocumentI regDoc = (RegisterDocumentI)doc;
				            		
				            		for (PropertyI prop:regDoc.getProperties()){
				            			if(prop.hasSubdividedLegal()){
				            				SubdivisionI subdiv = prop.getLegal().getSubdivision();
				            				String name = subdiv.getName();
				            				String platBook = subdiv.getPlatBook();
				            				String platPage = subdiv.getPlatPage();
				            				String lot = subdiv.getLot();
				            				String block = subdiv.getBlock();
				            				
				            				search.getSa().setAtribute(SearchAttributes.LD_TS_PLAT_BOOK, platBook);
				            				search.getSa().setAtribute(SearchAttributes.LD_TS_PLAT_PAGE, platPage);
				            				search.getSa().setAtribute(SearchAttributes.LD_TS_SUBDIV_NAME, name);
				            				search.getSa().setAtribute(SearchAttributes.LD_TS_LOT, lot);
				            				search.getSa().setAtribute(SearchAttributes.LD_TS_BLOCK, block);
				            				
				            			}
				            		}
				            		
				            	}
				            }
				        }
				} else if (nameValue.contains("typeOfSearch")){
					nameValue = URLDecoder.decode(nameValue, "UTF-8");
					String[] fields = nameValue.split(GenericSKLD.FIELD_SEPARATOR);
					if(fields.length == 2) {
						for (String field : fields) {
							if(field.startsWith(GenericServerADI.TYPE_OF_LEGAL_KEY)) {
								search.setAdditionalInfo(SearchAttributes.LD_TAD_SUBDIVISION_OR_ACREAGE, 
										field.substring(GenericServerADI.TYPE_OF_LEGAL_KEY.length()));
							} else if (field.startsWith(GenericServerADI.PB_PG_ABS_KEY)) {
								search.setAdditionalInfo(SearchAttributes.LD_TAD_PLAT_BOOK_PAGE,
										field.substring(GenericServerADI.PB_PG_ABS_KEY.length()));
							}
						}
					}
				} else if (nameValue.contains("mapCode")){
					nameValue = URLDecoder.decode(nameValue, "UTF-8");
					String[] fields = nameValue.split(GenericSKLD.FIELD_SEPARATOR);
					if (nameValue.contains(GenericPI.MAP_CODE)) {
						for (String field : fields) {
							if (field.startsWith(GenericPI.MAP_BOOK)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAP_BOOK, field.substring(GenericPI.MAP_BOOK.length()));
							} else if (field.startsWith(GenericPI.MAP_PAGE)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAP_PAGE, field.substring(GenericPI.MAP_PAGE.length()));
							} else if (field.startsWith(GenericPI.MAP_CODE)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAP_CODE, field.substring(GenericPI.MAP_CODE.length()));
							} else if (field.startsWith(GenericPI.MAP_MJR_LEGAL_NAME)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAJ_LEGAL_NAME, field.substring(GenericPI.MAP_MJR_LEGAL_NAME.length()));
							}
						}
					} else {
						for (String field : fields) {
							if (field.startsWith(GenericPI.MAP_BOOK)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAP_BOOK, field.substring(GenericPI.MAP_BOOK.length()));
							} else if (field.startsWith(GenericPI.MAP_PAGE)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_MAP_PAGE, field.substring(GenericPI.MAP_PAGE.length()));
							}else if (field.startsWith(GenericPI.MAP_LOT)) {
								search.setAdditionalInfo(SearchAttributes.LD_PI_LOT, field.substring(GenericPI.MAP_LOT.length()));
							}
						}
					}
				} else{
					nameValue = URLDecoder.decode(nameValue, "UTF-8");
					String[] fields = nameValue.split(GenericSKLD.FIELD_SEPARATOR);
					if(fields.length == 3) {
						for (String field : fields) {
							if(field.startsWith(GenericSKLD.SUBDIVISION_NAME_KEY)) {
								search.setAdditionalInfo(SearchAttributes.LD_SK_SUBDIVISION_NAME, 
										field.substring(GenericSKLD.SUBDIVISION_NAME_KEY.length()));
							} else if (field.startsWith(GenericSKLD.MAPID_BOOK_KEY)) {
								search.setAdditionalInfo(SearchAttributes.LD_SK_MAPID_BOOK,
										field.substring(GenericSKLD.MAPID_BOOK_KEY.length()));
							} else if (field.startsWith(GenericSKLD.MAPID_PAGE_KEY)) {
								search.setAdditionalInfo(SearchAttributes.LD_SK_MAPID_PAGE,
										field.substring(GenericSKLD.MAPID_PAGE_KEY.length()));
							}
						}
						search.setAdditionalInfo(SearchAttributes.LD_SK_LOT_LOW,"");
						search.setAdditionalInfo(SearchAttributes.LD_SK_LOT_HIGH,"");
						search.setAdditionalInfo(SearchAttributes.LD_SK_BLOCK_LOW,"");
						search.setAdditionalInfo(SearchAttributes.LD_SK_BLOCK_HIGH,"");
					}
				} 
			}
			sendRedirect(request, response, AppLinks.getParentSiteNoSaveHref(searchId));
		} else if(opCodeString.equals(SUBDIVISION_SECONDARY_NAME_LOOKUP)) {
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			String nameValue = parameterParser.getStringParameter("doclink");
			if(nameValue != null) {
				nameValue = URLDecoder.decode(nameValue, "UTF-8");
				String[] fields = nameValue.split(GenericSKLD.FIELD_SEPARATOR);
				if(fields.length == 7) {
					
					for (String field : fields) {
						if(field.startsWith(GenericSKLD.SUBDIVISION_NAME_KEY)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_SUBDIVISION_NAME, 
									field.substring(GenericSKLD.SUBDIVISION_NAME_KEY.length()));
						} else if (field.startsWith(GenericSKLD.MAPID_BOOK_KEY)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_MAPID_BOOK,
									field.substring(GenericSKLD.MAPID_BOOK_KEY.length()));
						} else if (field.startsWith(GenericSKLD.MAPID_PAGE_KEY)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_MAPID_PAGE,
									field.substring(GenericSKLD.MAPID_PAGE_KEY.length()));
						} else if (field.startsWith(GenericSKLD.LOT_LOW)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_LOT_LOW,
									field.substring(GenericSKLD.LOT_LOW.length()));
						} else if (field.startsWith(GenericSKLD.LOT_HIGH)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_LOT_HIGH,
									field.substring(GenericSKLD.LOT_HIGH.length()));
						} else if (field.startsWith(GenericSKLD.BLOCK_LOW)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_BLOCK_LOW,
									field.substring(GenericSKLD.BLOCK_LOW.length()));
						} else if (field.startsWith(GenericSKLD.BLOCK_HIGH)) {
							search.setAdditionalInfo(SearchAttributes.LD_SK_BLOCK_HIGH,
									field.substring(GenericSKLD.BLOCK_HIGH.length()));
						}
					}
					
				}
			}
			response.sendRedirect(AppLinks.getParentSiteNoSaveHref(searchId));
			
		} else if(opCodeString.equals(RESTORE_DOCUMENTS)) {
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			search.clearVisitedLinks();
			search.clearValidatedLinks();
			search.clearRecursiveAnalisedLinks();
			search.removeAllVisitedDocs();
			search.removeAllRemovedInstruments();
			HashMap<Long, RestoreDocumentDataI> allRestorableDocuments = search.getRestorableDocuments();
			String getCheckboxName = request.getParameter("nameForCheckbox");
			String[] idsToSave = request.getParameterValues(getCheckboxName);
			int initialNumberOfDocs = search.getNumberOfDocs();
			if(idsToSave != null) {
				HashMap<Integer, TSInterface> allInterfaces = new HashMap<Integer, TSInterface>();
				HashMap<String, List<Long>> idsToSaveILCookLA = new HashMap<String, List<Long>>();
				
				for (String idToSave : idsToSave) {
					TSInterface tsInterface = null;
					try {
						long documentId = Long.parseLong(idToSave);
						int noOfDocsForThisTry = search.getNumberOfDocs();
						RestoreDocumentDataI document = allRestorableDocuments.get(documentId);
						if(document == null) {
							continue;
						}
						tsInterface = allInterfaces.get(document.getServerId());
						if(tsInterface == null) {
							tsInterface = TSServersFactory.GetServerInstance(document.getServerId(), searchId);
							tsInterface.setServerForTsd(search, BaseServlet.FILES_PATH);
							allInterfaces.put(document.getServerId(), tsInterface);
						}
						
						if ("ILCookLA".equals(tsInterface.toString())){
							String pin = ((ILCookLA) tsInterface).getPinFromDescription(document);
							if (org.apache.commons.lang.StringUtils.isNotEmpty(pin)){
								if (idsToSaveILCookLA.containsKey(pin)){
									List<Long> instrNumberList = idsToSaveILCookLA.get(pin);
									instrNumberList.add(documentId);
									idsToSaveILCookLA.put(pin, instrNumberList);
								} else{
									List<Long> instrNumberList = new ArrayList<Long>();
									instrNumberList.add(documentId);
									idsToSaveILCookLA.put(pin, instrNumberList);
								}
							}
							continue;
						}
						
						Object moduleObject = tsInterface.getRecoverModuleFrom(document);
						if(moduleObject == null) {
							continue;
						}
						
						if(moduleObject instanceof TSServerInfoModule) {
							TSServerInfoModule module = (TSServerInfoModule)moduleObject;
							
							ServerResponse result = tsInterface.performAction(
									TSServer.REQUEST_SEARCH_BY, 
									"", 
									module, 
									new SearchDataWrapper(request));
							result.setParentSiteSearch(true);
							tsInterface.setParentSite(true);
							tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
							tsInterface.SetAttribute("TSRIE_Restore", true);
							LinkInPage selflink = result.getParsedResponse().getPageLink();
							if (( selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD))  {
								tsInterface.performLinkInPage(selflink);
							}else{
								List<FilterResponse> filters = module.getFilterList();
								for (FilterResponse filterResponse : filters) {
									filterResponse.filterResponse(result);
								}
								tsInterface.recursiveAnalyzeResponse(result);
							}
							if(search.getNumberOfDocs() > noOfDocsForThisTry) {
								document.deleteFromDatabase();
							}
						} else if(moduleObject instanceof List){
							
							List<TSServerInfoModule> listOfModules = (List<TSServerInfoModule>) moduleObject;
							for (TSServerInfoModule module : listOfModules) {
								ServerResponse result = tsInterface.performAction(
										TSServer.REQUEST_SEARCH_BY, 
										"", 
										module, 
										new SearchDataWrapper(request));
								result.setParentSiteSearch(true);
								tsInterface.setParentSite(true);
								tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
								tsInterface.SetAttribute("TSRIE_Restore", true);
								LinkInPage selflink = result.getParsedResponse().getPageLink();
								if (( selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD))  {
									tsInterface.performLinkInPage(selflink);
								} else {
									List<FilterResponse> filters = module.getFilterList();
									for (FilterResponse filterResponse : filters) {
										filterResponse.filterResponse(result);
									}
									tsInterface.recursiveAnalyzeResponse(result);
								}
								if(search.getNumberOfDocs() > noOfDocsForThisTry) {
									break;
								}
							}
							if(search.getNumberOfDocs() > noOfDocsForThisTry) {
								document.deleteFromDatabase();
							}
						}

					} catch (Exception e) {
						logger.error("Problem restoring document with idToSave: " + idToSave, e);
					} finally {
						if(tsInterface != null) {
							tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", false);
							tsInterface.SetAttribute("TSRIE_Restore", false);
						}
					}
				}
				if (idsToSaveILCookLA.size() > 0 && allInterfaces.size() > 0){

					Iterator it = idsToSaveILCookLA.entrySet().iterator();
					while (it.hasNext()){
						Map.Entry pairs = (Map.Entry)it.next();
//					    System.out.println(pairs.getKey() + " = " + pairs.getValue());
					        
						String pin = (String) pairs.getKey();
						List<Long> documentIDs = (List<Long>) pairs.getValue();
						
						if (documentIDs != null && documentIDs.size() > 0){
							TSInterface tsInterface = null;
							for (Long documentId : documentIDs){
								RestoreDocumentDataI document = allRestorableDocuments.get(documentId);
								tsInterface = allInterfaces.get(document.getServerId());
								
								if (tsInterface == null){
									tsInterface = TSServersFactory.GetServerInstance(document.getServerId(), searchId);
									tsInterface.setServerForTsd(search, BaseServlet.FILES_PATH);
									allInterfaces.put(document.getServerId(), tsInterface);
								} else{
									break;
								}
							}
							
							if (tsInterface != null){
						        Object moduleObject = ((ILCookLA)tsInterface).getILCookLARecoverModuleFrom(pin, allRestorableDocuments, documentIDs);
						        int noOfDocsForThisTry = search.getNumberOfDocs();
						        
						        if (moduleObject == null){
									continue;
								}
								if (moduleObject instanceof TSServerInfoModule){
									TSServerInfoModule module = (TSServerInfoModule)moduleObject;
									
									tsInterface.setParentSite(true);
									tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
									tsInterface.SetAttribute("TSRIE_Restore", true);
									
									ServerResponse result = tsInterface.performAction(TSServer.REQUEST_SEARCH_BY, "", module, new SearchDataWrapper(request));
									result.setParentSiteSearch(true);
									
									LinkInPage selflink = result.getParsedResponse().getPageLink();
									if ((selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD)){
										tsInterface.performLinkInPage(selflink);
									} else{
										List<FilterResponse> filters = module.getFilterList();
										for (FilterResponse filterResponse : filters){
											filterResponse.filterResponse(result);
										}
										tsInterface.recursiveAnalyzeResponse(result);
									}
									noOfDocsForThisTry = search.getNumberOfDocs() - noOfDocsForThisTry;
									
									for (Long documentId : documentIDs){
										RestoreDocumentDataI document = allRestorableDocuments.get(documentId);
										if (document == null){
											continue;
										}
										if (noOfDocsForThisTry > 0){
											document.deleteFromDatabase();
											noOfDocsForThisTry--;
										}
									}
								}
						    }
						}
					}
				}
			}
			request.setAttribute("NumberOfSavedDocuments", search.getNumberOfDocs() - initialNumberOfDocs);
			forward(request, response, "/jsp/TSDIndexPage/recoverDocuments.jsp"
					+ "?" + RequestParams.SEARCH_ID + "=" + searchId);	
		} else if(opCodeString.equals(VIEW_IMAGE)) {
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			search.clearVisitedLinks();
			search.clearValidatedLinks();
			search.clearRecursiveAnalisedLinks();
			search.removeAllVisitedDocs();
			search.removeAllRemovedInstruments();
			HashMap<Long, RestoreDocumentDataI> allRestorableDocuments = search.getRestorableDocuments();
			String idToView = request.getParameter("idToView");
			

			try {
				long documentId = Long.parseLong(idToView);
				RestoreDocumentDataI document = allRestorableDocuments.get(documentId);
				if(document == null) {
					response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
					return;
				}
				TSInterface tsInterface = TSServersFactory.GetServerInstance(document.getServerId(), searchId);
				tsInterface.setServerForTsd(search, BaseServlet.FILES_PATH);
				
				Object objectImageDownloader = tsInterface.getImageDownloader(document);
				
				if(objectImageDownloader == null) {
					response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
					return;	
				}
				if(objectImageDownloader instanceof TSServerInfoModule) {
					TSServerInfoModule module = (TSServerInfoModule)objectImageDownloader;
					tsInterface.setParentSite(true);
					ServerResponse result = null;
					if(tsInterface instanceof TSServerDASL) {
						result = tsInterface.performAction(
							TSServer.REQUEST_SEARCH_BY, 
							"", 
							module, 
							new ImageLinkInPage(false));
					} else {
						result = tsInterface.performAction(
								TSServer.REQUEST_SEARCH_BY, 
								"", 
								module, 
								new SearchDataWrapper(request));
					}
					
					manageResultForImage(response, tsInterface, module, result);
					
				} else if(objectImageDownloader instanceof List){
					
					List<TSServerInfoModule> listOfModules = (List<TSServerInfoModule>) objectImageDownloader;
					boolean managedImage = false;
					for (TSServerInfoModule module : listOfModules) {
						tsInterface.setParentSite(true);
						ServerResponse result = null;
						if(tsInterface instanceof TSServerDASL) {
							result = tsInterface.performAction(
								TSServer.REQUEST_SEARCH_BY, 
								"", 
								module, 
								new ImageLinkInPage(false));
						} else {
							result = tsInterface.performAction(
									TSServer.REQUEST_SEARCH_BY, 
									"", 
									module, 
									new SearchDataWrapper(request));
						}
						Vector resultRows = result.getParsedResponse().getResultRows();
						if(resultRows.size() > 0 || result.getParsedResponse().getImageLinksCount() > 0) {
							manageResultForImage(response, tsInterface, module, result);
							managedImage = true;
							break;
						}
						/*
						result.setParentSiteSearch(true);
						tsInterface.setParentSite(true);
						tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
						tsInterface.SetAttribute("TSRIE_Restore", true);
						LinkInPage selflink = result.getParsedResponse().getPageLink();
						if (( selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD))  {
							tsInterface.performLinkInPage(selflink);
						} else {
							List<FilterResponse> filters = module.getFilterList();
							for (FilterResponse filterResponse : filters) {
								filterResponse.filterResponse(result);
							}
							tsInterface.recursiveAnalyzeResponse(result);
						}
						//TODO: correct this
						int noOfDocsForThisTry = -1;
						if(search.getNumberOfDocs() > noOfDocsForThisTry) {
							break;
						}
						*/
					}
					if(!managedImage) {
						response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
					}
					
				} else if(objectImageDownloader instanceof String ) {
					viewImageByLink(response, tsInterface, (String)objectImageDownloader);
				} else {
					response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
				}
				
				
			} catch (Exception e) {
				logger.error("Problem restoring document with idToSave: " + idToView, e);
				response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
			}
			
		} else if(opCodeString.equals(SAVE_SEARCH_PARAMS)) {
			String result = null;
			String key = parameterParser.getStringParameter("key");
			int serverId = parameterParser.getIntParameter("serverId");
			if(StringUtils.isNotEmpty(key) && serverId > 0) {
				search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				Object possibleModule = search.getAdditionalInfo(key);
				if(possibleModule instanceof TSServerInfoModule) {
					TSInterface tsInterface = TSServersFactory.GetServerInstance(serverId, searchId);
					
					if(tsInterface instanceof TSServerROLike) {
						result = ((TSServerROLike)tsInterface).saveSearchedParameters((TSServerInfoModule)possibleModule);
					} else if(tsInterface instanceof TSServerDASLAdapter) {
						result = ((TSServerDASLAdapter)tsInterface).saveSearchedParameters((TSServerInfoModule)possibleModule);
					}
				}
			}
			if(result != null) {
				response.getWriter().print(result);
			} else {
				response.getWriter().print("There was a problem saving searched parameters. Nothing was saved");
			}
		} else if(opCodeString.equals(SUBC_FOR_CAT)) {
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			JSONObject jsonObject = new JSONObject();
			
			String category = parameterParser.getStringParameter("cat", "");
			
			List<String> subcategories = null;
			
			if(org.apache.commons.lang.StringUtils.isNotBlank(category)) {
				subcategories = DocumentTypes.getSubcategoryForCategory(category, searchId);
			}
			if(subcategories == null) {
				subcategories = new ArrayList<String>();
			}
			
			
			jsonObject.put("value", subcategories);
			
			out.print(jsonObject);
			out.flush();
		}
		
		
		
	}




	public boolean manageResultForImage(HttpServletResponse response,
			TSInterface tsInterface, TSServerInfoModule module,
			ServerResponse result) throws IOException, ServerResponseException {
		if(result!=null){
		    DownloadImageResult dResult = result.getImageResult();
		    if(dResult!=null){
				if( dResult.getStatus() == DownloadImageResult.Status.OK ){
					ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
					return true;
				} else {
					response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
				}
		    } else {
		    	boolean error = true;
		    	if(module.getModuleIdx() != TSServerInfo.IMG_MODULE_IDX) {
		    		LinkInPage selflink = result.getParsedResponse().getPageLink();
		    		if(result.getParsedResponse().getImageLinksCount() > 0) {
		    			ImageI image = ImageTransformation.imageLinkInPageToImage(result.getParsedResponse().getImageLink(0));
		    			if(StringUtils.isEmpty(image.getPath())) {
		    				String basePath = ((TSServer)tsInterface).getImagePath();
		            		File file= new File(basePath);
		            		if(!file.exists()){
		            			file.mkdirs();
		            		}
		    				String imagePath = basePath+File.separator+image.getFileName();
		    				image.setPath(imagePath);
		    			}
						dResult =  tsInterface.downloadImage( image, null );
						if(dResult!=null){
							if( dResult.getStatus() == DownloadImageResult.Status.OK ){
								ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
								error = false;
							} else {
								response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
							}
			            }
					} else if (( selflink != null) && (selflink.getActionType() == TSServer.REQUEST_SAVE_TO_TSD))  {
						result = tsInterface.performLinkInPage(selflink);
						List<FilterResponse> filters = module.getFilterList();
						for (FilterResponse filterResponse : filters) {
							filterResponse.filterResponse(result);
						}
						if(result.getParsedResponse().getImageLinksCount() > 0) {
							dResult =  tsInterface.downloadImage( ImageTransformation.imageLinkInPageToImage(result.getParsedResponse().getImageLink(0)), null );
							if(dResult!=null){
								if( dResult.getStatus() == DownloadImageResult.Status.OK ){
									ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
									error = false;
								} else {
									response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
								}
				            }
						} else {
							if(result.getParsedResponse().getResultRows().size() > 0) {
								ParsedResponse pr = (ParsedResponse) result.getParsedResponse().getResultRows().get(0);
								LinkInPage linkObj = pr.getPageLink();
								if (linkObj != null && linkObj.getLink()!=null) {
									Collection<ParsedResponse> collectionResponses = null;
									try {
										tsInterface.SetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT", true);
										tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
										collectionResponses = ((TSServer)tsInterface).getParsedResponsesFromLinkInPage(linkObj, -1);
									} finally {
										if(tsInterface != null) {
											tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", false);
											tsInterface.SetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT", false);
										}
									}
									if(collectionResponses != null && collectionResponses.size() > 0) {
										ParsedResponse parsedResponse = collectionResponses.iterator().next();
										DocumentI documentI = parsedResponse.getDocument();
										if(documentI != null) {
											if(documentI.hasImage()) {
												dResult =  tsInterface.downloadImage( documentI );
												if(dResult!=null){
													if( dResult.getStatus() == DownloadImageResult.Status.OK ){
														ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
														error = false;
													} else {
														response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
													}
									            }
											} else {
												result.setError("Image is not available on the official site!");
												result.setResult(null);
												result.getParsedResponse().setError(result.getError());
											}
										} 
										
									}
					            }
							} 
						
						}
						
					}else{
						List<FilterResponse> filters = module.getFilterList();
						for (FilterResponse filterResponse : filters) {
							filterResponse.filterResponse(result);
						}
						//tsInterface.recursiveAnalyzeResponse(result);
						if(result.getParsedResponse().getResultRows().size() > 0) {
							ParsedResponse pr = (ParsedResponse) result.getParsedResponse().getResultRows().get(0);
							
							if(pr.getImageLinksCount() > 0) {
		            			ImageI image = ImageTransformation.imageLinkInPageToImage(pr.getImageLink(0));
		            			if(StringUtils.isEmpty(image.getPath())) {
		            				String basePath = ((TSServer)tsInterface).getImagePath();
		                    		File file= new File(basePath);
		                    		if(!file.exists()){
		                    			file.mkdirs();
		                    		}
		            				String imagePath = basePath+File.separator+image.getFileName();
		            				image.setPath(imagePath);
		            			}
								dResult =  tsInterface.downloadImage( image, null );
								if(dResult!=null){
									if( dResult.getStatus() == DownloadImageResult.Status.OK ){
										ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
										error = false;
									} else {
										response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
									}
					            }
							} else {
								LinkInPage linkObj = pr.getPageLink();
								if (linkObj != null && linkObj.getLink()!=null) {
									Collection<ParsedResponse> collectionResponses = null;
									try {
										tsInterface.SetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT", true);
										tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", true);
										collectionResponses = ((TSServer)tsInterface).getParsedResponsesFromLinkInPage(linkObj, -1);
									} finally {
										if(tsInterface != null) {
											tsInterface.SetAttribute("DO_NOT_ANALYZE_RESPONSE", false);
											tsInterface.SetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT", false);
										}
									}
									if(collectionResponses != null && collectionResponses.size() > 0) {
										ParsedResponse parsedResponse = collectionResponses.iterator().next();
										DocumentI documentI = parsedResponse.getDocument();
										if(documentI != null) {
											if(documentI.hasImage()) {
												dResult =  tsInterface.downloadImage( documentI );
												if(dResult!=null){
													if( dResult.getStatus() == DownloadImageResult.Status.OK ){
														ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
														error = false;
													} else {
														response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
													}
									            }
											} else {
												result.setError("Image is not available on the official site!");
												result.setResult(null);
												result.getParsedResponse().setError(result.getError());
											}
										} 
										
									}
					            }
							}
						} 
					}
		    	} 
		    	if(error) {
		        	String sHTML = "";
		            //error from the parent server
		            if (result.isError()) {
		                if (result.getErrorCode() == ServerResponse.ZERO_MODULE_ITERATIONS_ERROR) {
		                                   
		                } else if (result.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING) {           	
		                    
		                } else {
		                    sHTML = URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE
		                            + "<br><br>The error message is:"+ "\n\n </br>" + result.getParsedResponse().getError() + "\n\n"
		                            + (result.getResult() != null ? "<BR>" + result.getResult() : "<BR>No Server response available");
		                }
		                if(StringUtils.isNotEmpty(sHTML)) {
		                	response.getWriter().print(sHTML);	
		                } else {
		                	response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		                }
		            } else {
		            	response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		            }
		    	} else {
		    		return true;
		    	}
		    }
		} else {
			response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		}
		return false;
	}




	private boolean viewImageByLink(HttpServletResponse response,
			TSInterface tsInterface, String objectImageDownloader)
			throws ServerResponseException, IOException {
		tsInterface.setParentSite(true);
		ServerResponse result = tsInterface.GetLink(objectImageDownloader, true);
		if(result!=null){
		    DownloadImageResult dResult = result.getImageResult();
		    if(dResult!=null){
				if( dResult.getStatus() == DownloadImageResult.Status.OK ){
					ServletUtils.writeImageToClient(dResult.getImageContent(), dResult.getContentType(), response);
					return true;
				} else {
					response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
				}
		    } else {
		    	
		    	String sHTML = "";
		        //error from the parent server
		        if (result.isError()) {
		            if (result.getErrorCode() == ServerResponse.ZERO_MODULE_ITERATIONS_ERROR) {
		                               
		            } else if (result.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING) {           	
		                
		            } else {
		                sHTML = URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE
		                        + "<br><br>The error message is:"+ "\n\n </br>" + result.getParsedResponse().getError() + "\n\n"
		                        + (result.getResult() != null ? "<BR>" + result.getResult() : "<BR>No Server response available");
		            }
		            if(StringUtils.isNotEmpty(sHTML)) {
		            	response.getWriter().print(sHTML);	
		            } else {
		            	response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		            }
		        } else {
		        	response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		        }
		    }
		} else {
			response.getWriter().print(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
		}
		return false;
	}

}
