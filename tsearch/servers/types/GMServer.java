package ro.cst.tsearch.servers.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GoogleMapsStructure;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.TiffConcatenator;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.PlatI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class GMServer extends TSServer {
	
	private static final long serialVersionUID = 2673077563242187915L;

	private static final String KEY_JSON_ARRAY = "jsonArray";
	private static final String KEY_RESPONSE_STATUS = "status";
	private static final String KEY_RESPONSE_RESULTS = "results";
	
	private static final String KEY_ROW_ADDRESS_COMPONENTS = "address_components";
	@SuppressWarnings("unused")
	private static final String KEY_ROW_ADRC_SHORT_NAME = "short_name";
	private static final String KEY_ROW_ADRC_LONG_NAME = "long_name";
	private static final String KEY_ROW_ADRC_TYPE = "types";
	
	private static final String KEY_ROW_GEOMETRY = "geometry";
	private static final String KEY_ROW_G_LOCATION = "location";
	private static final String KEY_ROW_GL_LATITUDE = "lat";
	private static final String KEY_ROW_GL_LONGITUDE = "lng";
	
	private static final String KEY_ROW_ELEVATION = "elevation";
	
	private static final String KEY_AC_STREET_NUMBER = "[\"street_number\"]";
	private static final String KEY_AC_STREET_NAME = "[\"route\"]";
	private static final String KEY_AC_CITY1 = "[\"locality\"]";
	private static final String KEY_AC_CITY2 = "[\"locality\",\"political\"]";
	private static final String KEY_AC_ZIP = "[\"postal_code\"]";
	
	
	
	public GMServer(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public GMServer(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void prepareModuleForSearch(TSServerInfoModule module) {
		super.prepareModuleForSearch(module);
		
		if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX) {
			
			String center = module.getFunction(0).getParamValue() 		//address
					+ " " + module.getFunction(3).getParamValue()		//city
					+ ", " + getDataSite().getCountyName()
					+ ", " + getDataSite().getStateAbrev()
					+ " " + module.getFunction(4).getParamValue(); 		//ZIP
			
			center = center.replaceAll("\\s{2,}", " ").trim();
			
			//fake parameters as label so they will not be added in the link
			module.getFunction(0).setControlType(4);
			module.getFunction(3).setControlType(4);
			module.getFunction(4).setControlType(4);
			
			//finally set center value
			module.getFunction(5).setParamValue(center);				//center
			
		} else if(module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, Boolean.TRUE);
			
			module.getFunction(6).setParamValue(module.getParamValue(6).replaceAll("\\|", "%7C") + module.getFunction(7).getParamValue());
			
			
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
			
			
			try {
				JSONObject jsonObject = new JSONObject(rsResponse);
				
				String status = jsonObject.getString(KEY_RESPONSE_STATUS);
				if(!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
					Response.getParsedResponse().setWarning(NO_DATA_FOUND + "<br>Request Status: " + status);
					return;
				}
				
				JSONArray jsonArray = jsonObject.getJSONArray(KEY_RESPONSE_RESULTS);
				if(jsonArray == null || jsonArray.length() == 0) {
					Response.getParsedResponse().setWarning(NO_DATA_FOUND);
					return;
				}
				
				Response.getParsedResponse().setAttribute(KEY_JSON_ARRAY, jsonArray);
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, null, outputTable);
				Response.getParsedResponse().setAttribute(KEY_JSON_ARRAY, null);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
					
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
		            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
		            			"<td>Document Content</td></tr>";

		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}

		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
	            }
				
				
				
			} catch (JSONException e) {
				Response.getParsedResponse().setError("Site error (unexpected result)");
				return;
			}
			
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

//			StringBuilder accountId = new StringBuilder();
//			String details = getDetails(rsResponse, accountId);
//			String filename = accountId + ".html";
//			
//			if (viParseID != ID_SAVE_TO_TSD) {
//				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
//				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
//
//				HashMap<String, String> data = new HashMap<String, String>();
//				loadDataHash(data);
//				if (isInstrumentSaved(accountId.toString(),null,data)){
//					details += CreateFileAlreadyInTSD();
//				}
//				else {
//					mSearch.addInMemoryDoc(sSave2TSDLink, details);
//					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
//				}
//
//				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
//				Response.getParsedResponse().setResponse( details );
//				
//			} else {
//				smartParseDetails(Response,details);
//				
//				msSaveToTSDFileName = filename;
//				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
//				Response.getParsedResponse().setResponse( details );
//				
//				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
//				
//			}
			
			DocumentI document = parsedResponse.getDocument();
			
			if(document!= null) {
				msSaveToTSDFileName = document.getId() + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				
				@SuppressWarnings("unchecked")
				Map<String, GoogleMapsStructure> map = (Map<String, GoogleMapsStructure>)getSearch().getAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY);
				if (map!=null) {
					GoogleMapsStructure structure = map.get(document.getInstno());
					if (structure!=null) {
						String newIframeText = "<iframe src=\"" + URLMaping.path + URLMaping.GOOGLE_MAPS + 
								"?" + RequestParams.SEARCH_ID + "=" + searchId +
								"&" + RequestParams.INSTRUMENT_NUMBER + "=" + document.getInstno() +
								"&" + RequestParams.GOOGLE_MAPS_LATITUDE + "=" + structure.getLatitude() + 
								"&" + RequestParams.GOOGLE_MAPS_LONGITUDE + "=" + structure.getLongitude() +
								"&" + RequestParams.GOOGLE_MAPS_ZOOM + "=" + structure.getZoom() + 
								"&" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=" + structure.getTypeId() +
								"&" + RequestParams.GOOGLE_MAPS_IS_ALREADY_SAVED + "=" + true +
								"\" name=\"imageFrame\" width=\"640\" height=\"670\"></iframe>";
						
						Map<String, String> elevations = getElevations(getCurrentServerName(), searchId, structure.getLatitude() + "," + structure.getLongitude());
						String elevation = elevations.get(structure.getLatitude() + "_" + structure.getLongitude());
						if (elevation==null) {
							elevation = "0";
						}
						
						String result = replaceIframeText(Response.getResult(), newIframeText);
						result = replaceLatLongElev(result, structure, elevation);
						Response.setResult(result);
						
						String response  = replaceIframeText(parsedResponse.getResponse(), newIframeText);
						response = replaceLatLongElev(response, structure, elevation);
						parsedResponse.setResponse(response);
						
						String serverRowResponse =  (String)parsedResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
						if (result!=null) {
							serverRowResponse = replaceIframeText(serverRowResponse, newIframeText);
							serverRowResponse = replaceLatLongElev(serverRowResponse, structure, elevation);
							parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, serverRowResponse);
						}
						getSearch().addImagesToDocument(document, 
								msRequestSolverName + 
								"?" + msServerID + "&" + RequestParams.SEARCH_ID + "=" + getSearch().getID() + 
								"&dispatcher=" + TSServerInfo.ADV_SEARCH_MODULE_IDX + 
								"&center=" + structure.getLatitude() + "," + structure.getLongitude() +
								"&" + RequestParams.GOOGLE_MAPS_ZOOM + "=" + structure.getZoom() +
								"&" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=" + structure.getTypeId() +
								"&fakeName=fake.tiff");
						
						if (document instanceof PlatI) {
							PlatI plat = (PlatI)document;
							plat.setLatitude(structure.getLatitude() + "&#176;");
							plat.setLongitude(structure.getLongitude() + "&#176;");
							plat.setElevation(elevation + " m");
						}
						
					}
					
				}
				
			}
			
			break;
		case ID_GET_LINK :
			ParseResponse(sAction, Response, rsResponse.contains("Sort Search Results")
														? ID_SEARCH_BY_NAME
														: ID_DETAILS);
			break;
		default:
			break;
		}
		
		
	}
	
	public static String replaceIframeText(String text, String newIframeText) {
		int i1 = text.indexOf("<iframe");
		if (i1!=-1) {
			int i2 = text.indexOf("</iframe>", i1+1);
			if (i2!=-1) {
				text = text.substring(0, i1) + newIframeText + text.substring(i2+"</iframe>".length());
			}
		}
		return text;
	}
	
	public static String replaceLatLongElev(String text, GoogleMapsStructure structure, String elevation) {
		text = text.replaceFirst("(?is)(<td>\\s*<b>\\s*Latitude:\\s*</b>\\s*</td>\\s*<td>).*?(&#176;\\s*</td>)", "$1" + structure.getLatitude() + "$2");
		text = text.replaceFirst("(?is)(<td>\\s*<b>\\s*Longitude:\\s*</b>\\s*</td>\\s*<td>).*?(&#176;\\s*</td>)", "$1" + structure.getLongitude() + "$2");
		text = text.replaceFirst("(?is)(<td>\\s*<b>\\s*Elevation:\\s*</b>\\s*</td>\\s*<td>).*?( m\\s*</td>)", "$1" + elevation + "$2");
		return text;
	}
	
	public static HashMap<String, String> getElevations(String currentServerName, long searchId, String locationsQuery) {
		HashMap<String, String> elevations = new HashMap<String, String>();
		
		if(locationsQuery != null) {
			locationsQuery = "http://maps.googleapis.com/maps/api/elevation/json?sensor=false&locations=" + locationsQuery;
			
			String elevationResponse = "";
			HttpSite3 site = HttpManager3.getSite(currentServerName, searchId);
			try {
				for(int i=0; i<3; i++){
					try {
						elevationResponse = site.process(new HTTPRequest(locationsQuery)).getResponseAsString();
						break;
					} catch (RuntimeException e){
						logger.warn("Could not bring link:" + locationsQuery, e);
					}
				}
			} finally {
				// always release the HttpSite
				HttpManager3.releaseSite(site);
			}
				
			if (!"".equals(elevationResponse)) {
				try {
					JSONObject jsonObject = new JSONObject(elevationResponse);
				
					JSONArray jsonElevationArray = jsonObject.getJSONArray(KEY_RESPONSE_RESULTS);
					for (int i = 0; i < jsonElevationArray.length(); i++) {
						JSONObject elevationRow = jsonElevationArray.getJSONObject(i);
						
						JSONObject location = elevationRow.getJSONObject(KEY_ROW_G_LOCATION);
						
						String keyNumber = location.getString(KEY_ROW_GL_LATITUDE) + "_" + location.getString(KEY_ROW_GL_LONGITUDE);
						
						elevations.put(keyNumber, elevationRow.getString(KEY_ROW_ELEVATION));
						
					}
					
				} catch(JSONException jsonException) {
					logger.error("Error parsing elevation from link " + locationsQuery, jsonException);
				}	
			}
			
			
		}
		return elevations;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String notUsedHere,
			StringBuilder outputTable) {
		
		Collection<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		JSONArray jsonArray = null;
		
		if(response == null 
				|| response.getParsedResponse() == null
				|| !(response.getParsedResponse().getAttribute(KEY_JSON_ARRAY) instanceof JSONArray)){
			return intermediaryResponse;
		}
						
		jsonArray = (JSONArray)response.getParsedResponse().getAttribute(KEY_JSON_ARRAY);
		
		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		
		HashMap<String, String> elevations = new HashMap<String, String>(jsonArray.length());
		
		String locationsQuery = null;
		
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				JSONObject elem = (JSONObject)jsonArray.get(i);
				
				
				JSONObject geometry = elem.getJSONObject(KEY_ROW_GEOMETRY);
				
				JSONObject location = geometry.getJSONObject(KEY_ROW_G_LOCATION);
				
				String latitude = location.getString(KEY_ROW_GL_LATITUDE);
				String longitude = location.getString(KEY_ROW_GL_LONGITUDE);
				
				if(locationsQuery == null) {
					locationsQuery = latitude + "," + longitude;
				} else {
					locationsQuery += "%7C" + latitude + "," + longitude;
				}
				
			} catch(JSONException jsonException) {
				logger.error("Error lat_long parsing row " + i, jsonException);
			}
		}
		
		elevations = getElevations(getCurrentServerName(), searchId, locationsQuery);
				
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				JSONObject elem = (JSONObject)jsonArray.get(i);
				
				
				JSONObject geometry = elem.getJSONObject(KEY_ROW_GEOMETRY);
				
				JSONObject location = geometry.getJSONObject(KEY_ROW_G_LOCATION);
				
				String latitude = location.getString(KEY_ROW_GL_LATITUDE);
				String longitude = location.getString(KEY_ROW_GL_LONGITUDE);
				
//				long longKey = System.currentTimeMillis();
//		    	String keyNumber = getDataSite().getSiteTypeAbrev() + CRC.quick(elem.toString() + longKey);
		    	
		    	String keyNumber = latitude + "_" + longitude;
		    	
		    	String serverDocType = "Google Map";
				String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
				String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
				
		    	
				
				InstrumentI instr = new com.stewart.ats.base.document.Instrument();	
				instr.setDocType(docCateg);
		    	instr.setDocSubType(stype);
		    	instr.setInstno(keyNumber);
		    	instr.setYear(Calendar.getInstance().get(Calendar.YEAR));
		    	
		    	RegisterDocumentI docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
		    	
	    		docR.setSearchType(DocumentI.SearchType.AD);	
				docR.setInstrument(instr);
				docR.setServerDocType(serverDocType);
		    	docR.setType(SimpleChapterUtils.DType.ROLIKE);
		    	docR.setDataSource(getDataSite().getSiteTypeAbrev());
		    	docR.setSiteId(getServerID());
		    	docR.setRecordedDate(Calendar.getInstance().getTime());
		    	
		    	docR = DocumentsManager.createRegisterDocument(getSearch().getID(), docCateg, (RegisterDocument) docR, null);
		    	
		    	PartyI grantee = new Party(PType.GRANTEE);
		    	NameI nameGrantee = new Name();
		    	nameGrantee.setCompany(true);
		    	nameGrantee.setLastName("County of " + getDataSite().getCountyName());
		    	grantee.add(nameGrantee);
				docR.setGrantee(grantee);
				
				PartyI grantor = new Party(PType.GRANTOR);
				NameI nameGrantor = new Name();
				nameGrantor.setCompany(true);
				nameGrantor.setLastName("Property Map");
				grantor.add(nameGrantor);
				docR.setGrantor(grantor);
		    	
		    	if (docR instanceof PlatI) {
					PlatI plat = (PlatI) docR;
					plat.setLatitude(latitude + "&#176;");
					plat.setLongitude(longitude + "&#176;");
					plat.setElevation(elevations.get(keyNumber) + " m");
				}
		    	
		    	PropertyI prop = Property.createEmptyProperty();
		    	
		    	
		    	JSONArray addressComponents = elem.getJSONArray(KEY_ROW_ADDRESS_COMPONENTS);
		    	
		    	String addressToken = null;
		    	AddressI address = new Address();
		    	
		    	for (int j = 0; j < addressComponents.length(); j++) {
					JSONObject addressRow = addressComponents.getJSONObject(j);
					String type = addressRow.getString(KEY_ROW_ADRC_TYPE);
					if(type.equals(KEY_AC_STREET_NUMBER)) {
						if(addressToken == null) {
							addressToken = addressRow.getString(KEY_ROW_ADRC_LONG_NAME);
						} else {
							addressToken = addressRow.getString(KEY_ROW_ADRC_LONG_NAME) + addressToken;
						}
					} else if (type.equals(KEY_AC_STREET_NAME)) {
						if(addressToken == null) {
							addressToken = addressRow.getString(KEY_ROW_ADRC_LONG_NAME);
						} else {
							addressToken += " " + addressRow.getString(KEY_ROW_ADRC_LONG_NAME);
						}
					} else if (type.equals(KEY_AC_CITY1)||type.equals(KEY_AC_CITY2)) {
						address.setCity(addressRow.getString(KEY_ROW_ADRC_LONG_NAME));
					} else if (type.equals(KEY_AC_ZIP)) {
						address.setZip(addressRow.getString(KEY_ROW_ADRC_LONG_NAME));
					}
				}
		    	
		    	if(StringUtils.isNotBlank(addressToken)) {
		    		Bridge.fillAddressI(address, addressToken);
		    	}
		    	
		    	prop.setAddress(address);
		    	
		    	docR.addProperty(prop);
		    	
		    	
		    	ParsedResponse currentResponse = new ParsedResponse();
		    	currentResponse.setDocument(docR);
		    	
		    	String checkBox = "checked";
				if (isInstrumentSaved(keyNumber, docR, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			checkBox = "saved";
	    		} else {
	    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + docR.getId() + "'>";
	    			
	    			LinkInPage linkInPage = new LinkInPage(
	    					linkPrefix + "FK____" + docR.getId(), 
	    					linkPrefix + "FK____" + docR.getId(), 
	    					TSServer.REQUEST_SAVE_TO_TSD);
	    			
	    			
	    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + docR.getId())==null){
	    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + docR.getId(), currentResponse);
	    			}
	    			currentResponse.setPageLink(linkInPage);
		    			
	    			
	    		}
				
				
				
				String imageLink = "<br><a href=\"" + msRequestSolverName + 
						"?" + msServerID + "&" + RequestParams.SEARCH_ID + "=" + getSearch().getID() + 
						"&dispatcher=" + TSServerInfo.ADV_SEARCH_MODULE_IDX +
						"&" + RequestParams.INSTRUMENT_NUMBER + "=" + keyNumber +
						"&center=" + latitude + "," + longitude +
						"\" title=\"View Image\" target=\"_blank\">View Image</a>";
		    	
				getSearch().addImagesToDocument(docR, 
						msRequestSolverName + 
						"?" + msServerID + "&" + RequestParams.SEARCH_ID + "=" + getSearch().getID() + 
						"&dispatcher=" + TSServerInfo.ADV_SEARCH_MODULE_IDX + 
						"&center=" + latitude + "," + longitude + 
						"&" + RequestParams.GOOGLE_MAPS_ZOOM + "=" + "17" + 
						"&" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=" + "hybrid" + 
						"&fakeName=fake.tiff");
				
				String asHtml = docR.asHtml();
				String imageFrame1 = "<br><br>" + 
						"<table><tr><td><iframe src=\"" + URLMaping.path + URLMaping.GOOGLE_MAPS + 
						"?" + RequestParams.SEARCH_ID + "=" + searchId +
						"&" + RequestParams.INSTRUMENT_NUMBER + "=" + keyNumber +
						"&" + RequestParams.GOOGLE_MAPS_LATITUDE + "=" + latitude + 
						"&" + RequestParams.GOOGLE_MAPS_LONGITUDE + "=" + longitude +
						"&" + RequestParams.GOOGLE_MAPS_ZOOM + "=" + "17" + 
						"&" + RequestParams.GOOGLE_MAPS_TYPE_ID + "=" + "hybrid";
				String imageFrame2 = "&" + RequestParams.GOOGLE_MAPS_IS_ALREADY_SAVED + "=" + true;
				String imageFrame3 = "\" name=\"imageFrame\" width=\"640\" height=\"670\"></iframe></td></tr></table>";
				String completeHtml1 = "<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + imageFrame1 + imageFrame3  + "</td></tr>";
				String completeHtml2 = "<tr><td>" + asHtml + imageFrame1 + imageFrame2 + imageFrame3  + "</td></tr>";
				currentResponse.setOnlyResponse(completeHtml1);
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, completeHtml2);
				currentResponse.setSearchId(searchId);
				currentResponse.setUseDocumentForSearchLogRow(true);
				intermediaryResponse.add(currentResponse);
				outputTable.append(completeHtml2);
				
				@SuppressWarnings("unchecked")
				Map<String, GoogleMapsStructure> map = (Map<String, GoogleMapsStructure>)getSearch().getAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY);
				GoogleMapsStructure structure = new GoogleMapsStructure(Float.parseFloat(latitude), Float.parseFloat(longitude), 17, "hybrid");
				if (map==null) {
					map = new HashMap<String, GoogleMapsStructure>();
				}
				map.put(keyNumber, structure);
				getSearch().setAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY, map);
				
			} catch(JSONException jsonException) {
				logger.error("Error parsing row " + i, jsonException);
			}
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		ServerResponse serverResponse = null;
		try{
			LinkParser linkParser = new LinkParser(image.getLink(0));
			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
			TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(
					Integer.parseInt(linkParser.getParamValue(URLConnectionReader.PRM_DISPATCHER)), 
					searchDataWrapper);
			
			module.getFunction(7).setParamValue(linkParser.getParamValue("center"));
			module.getFunction(0).setParamValue(linkParser.getParamValue(RequestParams.GOOGLE_MAPS_ZOOM));
			module.getFunction(5).setParamValue(linkParser.getParamValue(RequestParams.GOOGLE_MAPS_TYPE_ID));
			
			searchDataWrapper.setImage(image);
			
			
			serverResponse = SearchBy(module, searchDataWrapper);
			if(serverResponse.getImageResult() == null ||  !DownloadImageResult.Status.OK.equals(serverResponse.getImageResult().getStatus())) {
				if(serverResponse.getImageResult() != null) {
					
					File testFile = new File(image.getPath());
					if(testFile.exists() && testFile.length() > 0) {
						return new DownloadImageResult( DownloadImageResult.Status.OK, FileUtils.readFileToByteArray(testFile), image.getContentType() );
					}
				}

				serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
				
			} else {
				DownloadImageResult imageResult = serverResponse.getImageResult();
				byte[] imageContent = imageResult.getImageContent();
				List<byte[]> pages = new ArrayList<byte[]>();
				pages.add(imageContent);
				imageResult.setContentType(image.getContentType());
				imageResult.setImageContent(TiffConcatenator.concatePngInTiff(pages));
				
				FileUtils.writeByteArrayToFile(new File(image.getPath()), imageResult.getImageContent());
				
				return imageResult;
			}
		} catch (Exception e) {
			if(image == null || image.getLinks() == null || image.getLinks().isEmpty()) {
				logger.error("Could not download image because there is not data ", e);
			} else {
				logger.error("Could not download image for " + image.getLink(0), e);
			}
		}
		
		if(serverResponse == null) {
			serverResponse = new ServerResponse();
			serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
		}
		return serverResponse.getImageResult();
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
		module.addFilter(AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d));
		moduleList.add(module);
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc,ServerResponse response){
		super.addDocumentAdditionalPostProcessing(doc,response);

		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			if(manager.contains(doc)) {
				if(doc instanceof RegisterDocumentI){
					RegisterDocumentI regDoc = (RegisterDocumentI)doc;
					regDoc.setChecked(false);
					regDoc.setIncludeImage(false);
				}
			}
		} catch (Throwable t) {
			logger.error("Error while post processing document", t);
		} finally {
			manager.releaseAccess();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {
		if (sd instanceof SearchDataWrapper) {
			SearchDataWrapper sdw = (SearchDataWrapper)sd;
			if (sdw!=null) {
				HttpServletRequest request = sdw.getRequest();
				if (request!=null) {
					String instr = (String)sdw.getRequest().getParameter(RequestParams.INSTRUMENT_NUMBER);
					if (instr!=null) {
						Map<String, GoogleMapsStructure> map = (Map<String, GoogleMapsStructure>)getSearch().getAdditionalInfo(AdditionalInfoKeys.GOOGLE_MAPS_KEY);
						if (map!=null) {
							GoogleMapsStructure structure = map.get(instr);
							if (structure!=null) {
								module.getFunction(7).setParamValue(structure.getLatitude() + "," + structure.getLongitude());
								module.getFunction(0).setParamValue(Integer.toString(structure.getZoom()));
								module.getFunction(5).setParamValue(structure.getTypeId());
							}
						}
					}
				}
			}
			
		}
		return super.SearchBy(bResetQuery, module, sd);
	}
	
	@Override
	public DownloadImageResult downloadImage(ImageI image, String documentIdStr, DocumentI doc) throws ServerResponseException {
		if (image!=null) {
			String path = image.getPath();
			if (!StringUtils.isEmpty(path)) {
				File f = new File(path);
				FileUtils.deleteQuietly(f);
			}
		}
		return super.downloadImage(image, documentIdStr, doc);
	}
}
