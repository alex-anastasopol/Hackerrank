package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;


public class TNGenericCountyAO extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave=false;
	private String specificCnty = "Anderson";
	private String searchedParcelDetail = "ParcelDetail3.asp";
	private static HashMap<String, String> countyListValues = new HashMap<String, String>();
	private static final Pattern pdPattern = Pattern.compile("(?is)\\s*[^>]action\\s*=\\s*[\\\"]([0-9a-z.]*)[^>]+>");
	private static final Pattern p_radio = Pattern.compile("(?is)<\\s*input[^>]+name\\s*=\\s*[\\\"]?mradParcelID[\\\"]?\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9\\s]+)[\\\"][^>]*>");	
	private static final Pattern p_mSystem = Pattern.compile("(?is)name\\s*=\\s*[\\\"]mSystem[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9]*)[\\\"]");
	private static final Pattern p_mCounty = Pattern.compile("(?is)name\\s*=\\s*[\\\"]mCounty[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9]*)[\\\"]");
	private static final Pattern p_CountyNumber = Pattern.compile("(?is)name\\s*=\\s*[\\\"]CountyNumber[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9]+)[\\\"]");
	private static final Pattern p_CountyName = Pattern.compile("(?is)name\\s*=\\s*[\\\"]CountyName[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9\\s]+)[\\\"]");
	private static final Pattern p_TaxYear = Pattern.compile("(?is)name\\s*=\\s*[\\\"]TaxYear[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9\\s]+)[\\\"]");
//	private static final Pattern p_mradParcelID = Pattern.compile("(?is)name\\s*=\\s*[\\\"]mradParcelID[\\\"]\\s+value\\s*=\\s*[\\\"]([a-zA-Z0-9\\s]+)[\\\"]");
	
	static	{
		countyListValues.put("Anderson", "001");
		countyListValues.put("Bedford", "002");
		countyListValues.put("Benton", "003");
		countyListValues.put("Bledsoe", "004"); 
		countyListValues.put("Blount", "005"); 
		countyListValues.put("Bradley", "006"); 
		countyListValues.put("Campbell", "007"); 
		countyListValues.put("Cannon", "008"); 
		countyListValues.put("Carroll", "009"); 
		countyListValues.put("Carter", "010"); 
		countyListValues.put("Cheatham", "011"); 
	    countyListValues.put("Chester", "012"); 
	    countyListValues.put("Claiborne", "013"); 
	    countyListValues.put("Clay", "014"); 
	    countyListValues.put("Cocke", "015"); 
	    countyListValues.put("Coffee", "016"); 
	    countyListValues.put("Crockett", "017"); 
	    countyListValues.put("Cumberland", "018");
//	    countyListValues.put("Davidson", "019"); 
	    countyListValues.put("Decatur", "020");
	    countyListValues.put("De Kalb", "021");
	    countyListValues.put("DeKalb", "021");
	    countyListValues.put("Dickson", "022"); 
	    countyListValues.put("Dyer", "023"); 
	    countyListValues.put("Fayette", "024"); 
	    countyListValues.put("Fentress", "025"); 
	    countyListValues.put("Franklin", "026"); 
	    countyListValues.put("Gibson", "027"); 
	    countyListValues.put("Giles", "028"); 
	    countyListValues.put("Grainger", "029"); 
	    countyListValues.put("Greene", "030"); 
	    countyListValues.put("Grundy", "031"); 
	    countyListValues.put("Hamblen", "032");
//	    countyListValues.put("Hamilton", "033");
	    countyListValues.put("Hancock", "034"); 
	    countyListValues.put("Hardeman", "035"); 
	    countyListValues.put("Hardin", "036"); 
	    countyListValues.put("Hawkins", "037"); 
	    countyListValues.put("Haywood", "038"); 
	    countyListValues.put("Henderson", "039"); 
	    countyListValues.put("Henry", "040"); 
	    countyListValues.put("Hickman", "041"); 
	    countyListValues.put("Houston", "042"); 
	    countyListValues.put("Humphreys", "043"); 
	    countyListValues.put("Jackson", "044"); 
	    countyListValues.put("Jefferson", "045"); 
	    countyListValues.put("Johnson", "046");  
//	    countyListValues.put("Knox", "047");
	    countyListValues.put("Lake", "048");  
	    countyListValues.put("Lauderdale", "049");  
	    countyListValues.put("Lawrence", "050");  
	    countyListValues.put("Lewis", "051");  
	    countyListValues.put("Lincoln", "052");  
	    countyListValues.put("Loudon", "053");  
	    countyListValues.put("McMinn", "054");	    
	    countyListValues.put("McNairy", "055");
	    countyListValues.put("Macon", "056"); 
	    countyListValues.put("Madison", "057");  
	    countyListValues.put("Marion", "058");  
	    countyListValues.put("Marshall", "059");
	    countyListValues.put("Maury", "060"); 
 	    countyListValues.put("Meigs", "061");  
	    countyListValues.put("Monroe", "062"); 
	    countyListValues.put("Montgomery", "063"); 
	    countyListValues.put("Moore", "064");  
	    countyListValues.put("Morgan", "065");  
	    countyListValues.put("Obion", "066");  
	    countyListValues.put("Overton", "067"); 
	    countyListValues.put("Perry", "068"); 
	    countyListValues.put("Pickett", "069"); 
	    countyListValues.put("Polk", "070"); 
	    countyListValues.put("Putnam", "071"); 
	    countyListValues.put("Rhea", "072"); 
	    countyListValues.put("Roane", "073"); 
	    countyListValues.put("Robertson", "074"); 
	    countyListValues.put("Rutherford", "075");
	    countyListValues.put("Scott", "076");  
	    countyListValues.put("Sequatchie", "077"); 
	    countyListValues.put("Sevier", "078");  
	    countyListValues.put("Shelby", "079");
	    countyListValues.put("Smith", "080"); 	
	    countyListValues.put("Stewart", "081");
	    countyListValues.put("Sullivan", "082");
	    countyListValues.put("Sumner", "083");
	    countyListValues.put("Tipton", "084"); 
	    countyListValues.put("Trousdale", "085"); 
//	    countyListValues.put("Unicoi", "086"); //not currently implemented on the official site
	    countyListValues.put("Union", "087"); 
	    countyListValues.put("Van Buren", "088");
	    countyListValues.put("VanBuren", "088"); 
	    countyListValues.put("Warren", "089"); 
	    countyListValues.put("Washington", "090"); 
	    countyListValues.put("Wayne", "091"); 
	    countyListValues.put("Weakley", "092"); 
	    countyListValues.put("White", "093"); 
	    countyListValues.put("Williamson", "094");
	    countyListValues.put("Wilson", "095");
	}

	
	public void setServerID(int ServerID){
		super.setServerID(ServerID);
		setSpecificCounty(getDataSite().getCountyName());
	}
	
	public TNGenericCountyAO(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		
	}

	protected void setSpecificCounty(String cntyName)
	{
		specificCnty = cntyName;
	}
	
	protected String getSpecificCounty()
	{
		return countyListValues.get(specificCnty);
	}
	
	public TSServerInfo getDefaultServerInfo() {
	    TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;
        
        if (msiServerInfoDefault == null)
        {
            msiServerInfoDefault = new TSServerInfo(1);

            msiServerInfoDefault.setServerAddress( "www.assessment.state.tn.us" );
            msiServerInfoDefault.setServerLink( "http://www.assessment.state.tn.us/ParcelList.asp" );
            msiServerInfoDefault.setServerIP( "www.assessment.state.tn.us" );

			HashMap<String, String> radioValues = new HashMap<String, String>();
			radioValues.put( "Owner", "Owner" );
			radioValues.put( "Property Address", "PropertyAddress" );
			radioValues.put( "Parcel ID", "ParcelID" );
			radioValues.put( "Subdivision", "Subdivision" );
			radioValues.put( "Classification", "Classification" );
			radioValues.put( "Sale Date", "SaleDate" );
            
            //search county
            {
                simTmp = SetModuleSearchByName( 
                        13, 
                        msiServerInfoDefault, 
                        TSServerInfo.NAME_MODULE_IDX, 
                        "/ParcelList.asp", 
                        TSConnectionURL.idPOST, 
                        "", "txtOwnerName");
                simTmp.setSearchType("CS");
                
                String controlMap = "";
            	String group = "";
            	String parcel = "";
                if (hasPin()) {
                	String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
                	Matcher ma1 = Pattern.compile("([^-]+)-([^-]+)").matcher(pin);
                	Matcher ma2 = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(pin);
                	Matcher ma3 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{3}\\.\\d{2})").matcher(pin);
                	Matcher ma4 = Pattern.compile("([^-]+)-([^-]*)-\\1-([^-]+)-[^-]*-[^-]+").matcher(pin);
                	Matcher ma5 = Pattern.compile("([^\\s]+) ([^\\s]*) ([^\\s]+) [^\\s]+").matcher(pin);
                	Matcher ma6 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{5})(\\d{3})").matcher(pin);
                	if (ma1.matches()) {			//PIN from NB with dashes without group
                		controlMap = ma1.group(1);
                		parcel = ma1.group(2);
                	} else if (ma2.matches()) {		//PIN from NB with dashes with group
                		controlMap = ma2.group(1);
                		group = ma2.group(2);
                		parcel = ma2.group(3);
                	} else if (ma3.matches()) {		//PIN from NB without dashes
                		controlMap = ma3.group(1);
                		group = ma3.group(2);
                		parcel = ma3.group(3);
                	} else if (ma4.matches()) {		//PIN from AO
                		controlMap = ma4.group(1);
                		group = ma4.group(2);
                		parcel = ma4.group(3);
                	} else if (ma5.matches()) {		//PIN from TR without si
                		controlMap = ma5.group(1);
                		group = ma5.group(2);
                		parcel = ma5.group(3);
                		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
                	} else if (ma6.matches()) {		//PIN from TR with si
                		controlMap = ma6.group(1);
                		group = ma6.group(2);
                		parcel = ma6.group(3);
                		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
                	}
                	
                }
                
                try
                {
                    PageZone searchCounty = new PageZone("root", "Search County", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(250),HTMLObject.PIXELS , true);
                    searchCounty.setBorder(true);

                    HTMLControl lastName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "", "Last Name", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
                    lastName.setHiddenParam(true);
                    searchCounty.addHTMLObject( lastName );
                    simTmp.getFunction( 0 ).setLoggable(true);
                    
                    HTMLControl txtOwnerName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtOwnerName", "Owner Name", 1, 1, 1, 1, 30, null, simTmp.getFunction( 1 ), searchId );
                    txtOwnerName.setJustifyField( true );
                    txtOwnerName.setRequiredExcl( true );
                    txtOwnerName.setFieldNote( "e.g.: SMITH J or SMITH JOHN" );
                    searchCounty.addHTMLObject( txtOwnerName );
                    
					HTMLControl txtPropertyAddress = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtPropertyAddress", "Property Address", 1, 1, 2, 2, 30, null, simTmp.getFunction( 2 ), searchId );
					txtPropertyAddress.setJustifyField( true );
					txtPropertyAddress.setRequiredExcl( true );
					txtPropertyAddress.setFieldNote( "e.g.: MAIN ST 1200" );
					searchCounty.addHTMLObject( txtPropertyAddress );
                    
                    HTMLControl txtControlMap = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtControlMap", "Control Map", 1, 1, 3, 3, 20, null, simTmp.getFunction( 3 ), searchId );
                    txtControlMap.setJustifyField( true );
                    txtControlMap.setRequiredExcl( true );
                    txtControlMap.setFieldNote( "e.g.: 012 or 100A" );
                    txtControlMap.setDefaultValue(controlMap);
                    searchCounty.addHTMLObject( txtControlMap );

                    HTMLControl txtGroup = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtGroup", "Group", 1, 1, 4, 4, 20, null, simTmp.getFunction( 4 ), searchId );
                    txtGroup.setJustifyField( true );
                    txtGroup.setRequiredExcl( true );
                    txtGroup.setFieldNote( "e.g.: B or AA" );
                    txtGroup.setDefaultValue(group);
                    searchCounty.addHTMLObject( txtGroup );
                    
                    HTMLControl txtParcel = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtParcel", "Parcel", 1, 1, 5, 5, 20, null, simTmp.getFunction( 5 ), searchId );
                    txtParcel.setJustifyField( true );
                    txtParcel.setRequiredExcl( true );
                    txtParcel.setFieldNote( "e.g.: 025.00" );
                    txtParcel.setDefaultValue(parcel);
                    searchCounty.addHTMLObject( txtParcel );

                    HTMLControl txtSubdivisionName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtSubdivisionName", "Subdivision Name", 1, 1, 6, 6, 30, null, simTmp.getFunction( 6 ), searchId );
                    txtSubdivisionName.setJustifyField( true );
                    txtSubdivisionName.setRequiredExcl( true );
                    txtSubdivisionName.setFieldNote( "e.g.: RIVER OAKS" );
                    searchCounty.addHTMLObject( txtSubdivisionName );
                    
                    //Classification
					HTMLControl classification = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Class", "Classification:", 1, 1, 7, 7, 15,""/*"ALL CLASSES"*/, simTmp.getFunction( 7 ), searchId );
					classification.setJustifyField( true );
					classification.setRequiredExcl( true );
					searchCounty.addHTMLObject( classification );
                    
					simTmp.getFunction(7).setHtmlformat(
					"<select NAME=\"Class\"><OPTION VALUE=\"\">ALL CLASSES" +
					"<OPTION VALUE=\"00\"> 00 - RESIDENTIAL" +
					"<OPTION VALUE=\"01\"> 01 - COUNTY" +
					"<OPTION VALUE=\"02\"> 02 - CITY" +
					"<OPTION VALUE=\"03\"> 03 - STATE" +
					"<OPTION VALUE=\"04\"> 04 - FEDERAL" +
					"<OPTION VALUE=\"05\"> 05 - RELIGIOUS" +
					"<OPTION VALUE=\"06\"> 06 - ED/SCI/CHARITABLE" +
					"<OPTION VALUE=\"07\"> 07 - SAP UTILITY" +
					"<OPTION VALUE=\"08\"> 08 - COMMERCIAL" +
					"<OPTION VALUE=\"09\"> 09 - INDUSTRIAL" +
					"<OPTION VALUE=\"10\"> 10 - FARM" +
					"<OPTION VALUE=\"11\"> 11 - AGRICULTURAL" +
					"<OPTION VALUE=\"12\"> 12 - FOREST" +
					"<OPTION VALUE=\"13\"> 13 - OPEN SPACE" +
					"<OPTION VALUE=\"14\"> 14 - HOMEBELT" +
					"<OPTION VALUE=\"15\"> 15 - LOCAL UTILITY" +
					"</select>");
					simTmp.getFunction(7).setName("Classification:");
					simTmp.getFunction(7).setParamName("Class");
					simTmp.getFunction(7).setParamType(TSServerInfoFunction.idSingleselectcombo);
//					simTmp.getFunction(7).setDefaultValue(""/*"ALL CLASSES"*/);
					
					HTMLControl txtBegSaleDate = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtBegSaleDate", "From", 1, 1, 8, 8, 20, null, simTmp.getFunction( 8 ), searchId );
                    txtBegSaleDate.setJustifyField( true );
                    txtBegSaleDate.setRequiredExcl( true );
                    txtBegSaleDate.setFieldNote( "e.g.: mm/dd/yyyy" );
                    searchCounty.addHTMLObject( txtBegSaleDate );
                    
                    HTMLControl txtEndingSaleDate = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "txtEndingSaleDate", "Through", 1, 1, 9, 9, 20, null, simTmp.getFunction( 9 ), searchId );
                    txtEndingSaleDate.setJustifyField( true );
                    txtEndingSaleDate.setRequiredExcl( true );
                    txtEndingSaleDate.setFieldNote( "e.g.: mm/dd/yyyy" );
                    searchCounty.addHTMLObject( txtEndingSaleDate );
                    
                    //Sort Options
					HTMLControl orderBy = new HTMLControl( HTMLControl.HTML_RADIO_BUTTON, "SortOptions", "Sort Selection by:", 1, 1, 10, 10, 100, radioValues, simTmp.getFunction( 10 ), searchId);
					orderBy.setJustifyField( true );
					orderBy.setRequiredExcl( true );
					orderBy.setHorizontalRadioButton(true);
					orderBy.setDefaultRadio( "Owner" );
					searchCounty.addHTMLObject( orderBy );

					HTMLControl countyList = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "countylist", "", 1, 1, 1, 1, 20, getSpecificCounty(), simTmp.getFunction( 11 ), searchId );
					countyList.setHiddenParam( true );
					searchCounty.addHTMLObject( countyList );
					
                    HTMLControl submit = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "submit1", "", 1, 1, 11, 11, 30, "     SEARCH     ", simTmp.getFunction( 12 ), searchId );
                    submit.setHiddenParam( true );
                    searchCounty.addHTMLObject( submit );
                    
                    simTmp.setModuleParentSiteLayout( searchCounty );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                
                simTmp.getFunction( 0 ).setSaKey( "" );
                simTmp.getFunction( 1 ).setSaKey( SearchAttributes.OWNER_LFM_NAME );
                
                simTmp.getFunction( 2 ).setParamName("txtPropertyAddress");
                simTmp.getFunction( 2 ).setSaKey(SearchAttributes.P_STREETNAME_SUFFIX_UNIT_NO);

                simTmp.getFunction( 3 ).setParamName("txtControlMap");
                simTmp.getFunction( 3 ).setSaKey( SearchAttributes.LD_PARCELNO_MAP );
                
                simTmp.getFunction( 4 ).setParamName("txtGroup");
                simTmp.getFunction( 4 ).setSaKey( SearchAttributes.LD_PARCELNO_GROUP );
                
                simTmp.getFunction( 5 ).setParamName("txtParcel");
                simTmp.getFunction( 5 ).setSaKey(SearchAttributes.LD_PARCELNO_PARCEL);

                simTmp.getFunction( 6 ).setParamName("txtSubdivisionName");
                simTmp.getFunction( 6 ).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
                
                simTmp.getFunction( 12 ).setHiddenParam("submit1", "     SEARCH     ");
            }            
            
            msiServerInfoDefault.setupParameterAliases();
            setModulesForAutoSearch( msiServerInfoDefault );
            
        }
        
        return msiServerInfoDefault;
	}

	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{

			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr align=left",
				"</table>",
				linkStart,
				action);
		}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException
	{
			String sTmp = "";
			String rsResponse = Response.getResult();
			String initialResponse = rsResponse;
			int istart = -1, iend = -1;
			
			switch (viParseID)
			{
				case ID_SEARCH_BY_NAME :
				    if (rsResponse.matches("(?is).*THERE\\s*ARE\\s*NO\\s*RECORDS\\s*MATCHING\\s*YOUR\\*SELECTION.*")
				    		|| rsResponse.matches("(?is).*Requested\\s*operation\\s*requires\\s*a\\s*current\\s*record.*")
				    		|| rsResponse.matches("(?is).*Microsoft\\s*VBScript\\s*runtime.*")
				    		|| rsResponse.matches("(?is).*Script\\s*timed\\s*out.*")
				    		|| rsResponse.matches("(?is).*Invalid\\s*Search.*")
				    		|| rsResponse.matches("(?is).*Object\\s*Moved.*"))
				    {
						Response.getParsedResponse().setError("No results found");
						logger.error("ParseResponse END: Results not found!");
						return;
				    }
		            
					istart = rsResponse.indexOf("<form name=\"ParcelList\"");
		            
					if (istart!=-1)
					{
						iend  = rsResponse.indexOf("</form>", istart);
					}
					
					if ((istart!=-1) && (iend!=-1))
					{
					    iend = iend + "</form>".length();
					    rsResponse = rsResponse.substring(istart, iend);
					    
					    int t_start =  rsResponse.indexOf("<table");
					    int t_end  =  rsResponse.indexOf("</table>", t_start);
					    t_end       += "</table>".length();
					    
					    if (t_start!= -1 && t_end !=-1)
					    {
					    	rsResponse = rsResponse.substring(0, t_start) + rsResponse.substring(t_end, rsResponse.length());
					    }
					    
					    Matcher pdMatcher = pdPattern.matcher(rsResponse);
					    if (pdMatcher.find())
					    	searchedParcelDetail = pdMatcher.group(1);
					}
					
					rsResponse = rsResponse.replaceAll("\\s*<!--[^>]+-->", "");//delete the comments
					rsResponse = rsResponse.replaceAll("(?is)<\\s*tr\\s+align=left[^>]*>", "<tr align=left>");
					rsResponse = rsResponse.replaceAll("(?is)<\\s*td[^>]+>\\s*<\\s*a\\s+[^>]+>\\s*<\\s*font[^>]+>[<>a-z\\s]*map[<>/a-z\\s]*<\\s*/font\\s*>\\s*</a>\\s*</td>", "<td><b>MAP</b></td>");
					
					sTmp = CreatePartialLink(TSConnectionURL.idPOST);//rsResponse = rsResponse.replaceAll ("ParcelDetail4.asp", "http://www.assessment.state.tn.us/ParcelDetail4.asp");
					
					StringBuffer input = new StringBuffer(rsResponse);

					Matcher mt_mSystem = p_mSystem.matcher(input);
					Matcher mt_mCounty = p_mCounty.matcher(input);
					Matcher mt_CountyNumber = p_CountyNumber.matcher(input);
					Matcher mt_CountyName = p_CountyName.matcher(input);
					Matcher mt_TaxYear = p_TaxYear.matcher(input);
//					Matcher mt_mradParcelID = p_mradParcelID.matcher(input);
//					mradParcelID = mt_CountyNumber.group(1);(?is)<\s*input[^>]+name\s*=\s*[\"]?mradParcelID[\"]?\s+value\s*=\s*\"([a-zA-Z0-9\s]+)\"[^>]*>

					String mSystem = "";
					String mCounty = "";
					String CountyNumber = "";
					String CountyName = "";
					String TaxYear = "";
					
					if (mt_mSystem.find(1))
					{
						mSystem = mt_mSystem.group(1);
					}
					if (mt_mCounty.find(1))
					{
						mCounty = mt_mCounty.group(1);
					}
					if (mt_CountyNumber.find(1))
					{
						CountyNumber = mt_CountyNumber.group(1);
					}
					if (mt_CountyName.find(1))
					{
						CountyName = mt_CountyName.group(1);
					}
					if (mt_TaxYear.find(1))
					{
						TaxYear = mt_TaxYear.group(1);
					}
				
					Matcher mt_radio = p_radio.matcher(input);
					int start_pos = 1;
					
					while (mt_radio.find(start_pos))
					{
						String str_radio = mt_radio.group(0);
						String mradParcelID = mt_radio.group(1);
						
						int aux = start_pos; 
						start_pos = rsResponse.indexOf(str_radio, aux);
						
						String link_str = "<a href=\""+sTmp+"/"+searchedParcelDetail+/*ParcelDetail4.asp*/"&mradParcelID="+
						mradParcelID + "&mSystem=" + mSystem +
						"&mCounty="+mCounty+"&CountyNumber="+CountyNumber+"&CountyName="
						+CountyName+"&TaxYear="+TaxYear+"\">View</a>"; 
						
						rsResponse = rsResponse.substring(0, start_pos) + link_str +
						rsResponse.substring(start_pos+str_radio.length(), rsResponse.length());
						
						start_pos += link_str.length();
						
						if ( start_pos >= rsResponse.length() )
						{
							start_pos = rsResponse.length() - 2;
						}
						mt_radio = p_radio.matcher(input);
						input       = new StringBuffer(rsResponse);
					}
					
					rsResponse = rsResponse.replaceFirst("(?is)[^a-z]+form[^<]+<\\s*center[^>]*>","");
					rsResponse = rsResponse.replaceFirst("(?is)\\s*<\\s*/td\\s*>\\s*<\\s*/tr\\s*>\\s*<\\s*/table\\s*>\\s*<\\s*input.*","");
					
					parser.Parse(
						Response.getParsedResponse(),
						rsResponse,
						Parser.PAGE_ROWS,
						getLinkPrefix(TSConnectionURL.idPOST),
						TSServer.REQUEST_SAVE_TO_TSD);
					break;
				case ID_DETAILS :
					if (!rsResponse.matches("(?is).*Property\\s*Owner\\s*and\\s*Mailing\\s*Address.*") ||
							rsResponse.matches("(?is).*Invalid\\s*Search.*") ||
							rsResponse.matches("(?is).*Object\\s*Moved.*"))
				    {
						Response.getParsedResponse().setError("Final Page not found!");
						logger.error("ParseResponse END: Final Page not found!");
						return;
				    }
					
					istart = -1;
					iend = -1;
					StringBuffer sb    = new StringBuffer (rsResponse);
					Pattern p_start     = Pattern.compile("(?is)<\\s*table\\s+[^>]+95[^>]+>.*property\\s+owner\\s+and\\s+mailing");
					Pattern p_end      = Pattern.compile("(?is)<p><table border=\\\"0\\\" width=\\\"640\\\" id=\\\"table1\\\"");
					Matcher mt_start = p_start.matcher(sb);
					Matcher mt_end  = p_end.matcher(sb);

					if (mt_start.find(1) && mt_end.find(1))
					{
						String str_start = mt_start.group(0);
						String str_end  = mt_end.group(0);
						
						istart = rsResponse.indexOf(str_start);
						iend  = rsResponse.indexOf(str_end);
					}
					
					if (istart!=-1 && iend!=-1)
					{
						rsResponse = rsResponse.substring(istart, iend);
					}
					
					rsResponse = rsResponse.replaceAll("(?is)bgcolor\\s*=\\s*[\\\"]?[#]ffffc0[\\\"]?", " ");//StringUtils.toFile("/Desktop/Task1_ev/debug2.txt", rsResponse.toString());
	                
					//get detailed document addressing code
	                String query    = Response.getQuerry();
	                String keyCode  = getFileNameFromLink(query);
	                keyCode         = keyCode.substring(0, keyCode.indexOf(".html"));
					
					if ((!downloadingForSave))
					{
						String qry = Response.getRawQuerry();
						qry = "dummy=" + keyCode + "&" + qry;
						String originalLink = sAction + "&" + qry;
						String sSave2TSDLink =
							getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

						//if (FileAlreadyExist(keyCode + ".html") ) 
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","ASSESSOR");
						keyCode = getKeyCodeFromResponse(rsResponse);
						if(isInstrumentSaved(keyCode, null, data))
						{
						    rsResponse += CreateFileAlreadyInTSD();
						}
						else 
						{
						    rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
							mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.NO_PARSE);
					} 
					else
					{//for html
						msSaveToTSDFileName = keyCode + ".html";

						Response.getParsedResponse().setFileName(
							getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

						if (rsResponse.indexOf("Property")!=-1)
						{
						    parser.Parse(Response.getParsedResponse(),
									rsResponse,
									Parser.PAGE_DETAILS);
						}
						else
						{
						    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
						}
						
					}
					break;
				case ID_GET_LINK :
					ParseResponse(sAction, Response, ID_DETAILS);
					break;
				case ID_SAVE_TO_TSD :
					if (sAction.equals("/PropertySearch.aspx"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					else
					{// on save
						downloadingForSave = true;
						ParseResponse(sAction, Response, ID_DETAILS);
						downloadingForSave = false;
					}
					break;
				default:
					break;

			}
		}
	
	private String getKeyCodeFromResponse(String rsResponse) {
		String result = "";
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = parser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "95%"));
			for (int i = 0; i < tableList.size(); i++) {
				if(tableList.elementAt(i).toPlainTextString().contains("Property Location")) {
					NodeList mainTableList = tableList.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if(mainTableList.size() > 0) {
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
						TableRow[] rows = mainTable.getRows();
						for (TableRow tableRow : rows) {
							String rowAsString = tableRow.toPlainTextString();
							TableColumn[] columns = tableRow.getColumns();
							if(columns.length > 2) {
								for (int j = 0; j < columns.length; j++) {
									rowAsString = columns[j].toPlainTextString();
									if( rowAsString.contains("Ctrl Map:") ) {
										result += columns[++j].toPlainTextString().trim() + "-";
									} else if ( rowAsString.contains("Map:") ) {
										result += columns[++j].toPlainTextString().trim() + "-";
									} else if ( rowAsString.contains("Grp:") ) {
										result += columns[++j].toPlainTextString().trim() + "-";
									} else if ( rowAsString.contains("Parcel:") ) {
										result += columns[++j].toPlainTextString().trim() + "-";
									} else if ( rowAsString.contains("PI:") ) {
										result += columns[++j].toPlainTextString().trim() + "-";
									} else if ( rowAsString.contains("S/I:") ) {
										result += columns[++j].toPlainTextString().trim();
									}
								}
								
							}
						}
					}
					break;
				}
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getiing keyCode", e);
		}
		return result.replaceAll("&nbsp;", "");
	}

	public ServerResponse SearchBy(
			TSServerInfoModule module,
			Object sd)
			throws ServerResponseException
		{
			module.getFunction(11).setDefaultValue( getSpecificCounty() );
			module.getFunction(11).setParamValue( getSpecificCounty() );
			
			return super.SearchBy(module, sd);
		}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

		boolean emptySubdiv = "".equals( sa.getAtribute( SearchAttributes.LD_SUBDIV_NAME ) );
		TSServerInfoModule m = null;
		FilterResponse adressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , m );
		String str = DBManager.getCountyForId(new Long (sa.getAtribute( SearchAttributes.P_COUNTY )).longValue()).getName();
		if (str.contains("&nbsp;") || str.contains(" "))
		{
			str = str.replace("&nbsp;", "");
			str = str.replaceAll("\\s+", "");
		}
		String cntyCode = countyListValues.get(str);
		
		if(hasPin())
		{//parcel
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(1).setSaKey("");// 1. FirstName
			m.getFunction(2).setSaKey("");// 2. PropertyAddress
			m.getFunction(6).setSaKey("");// 6. SubdivisionName
			
			boolean found = false;
			String controlMap = "";
        	String group = "";
        	String parcel = "";
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
        	Matcher ma1 = Pattern.compile("([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma2 = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma3 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{3}\\.\\d{2})").matcher(pin);
        	Matcher ma4 = Pattern.compile("([^-]+)-([^-]*)-\\1-([^-]+)-[^-]*-[^-]+").matcher(pin);
        	Matcher ma5 = Pattern.compile("([^\\s]+) ([^\\s]*) ([^\\s]+) [^\\s]+").matcher(pin);
        	Matcher ma6 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{5})(\\d{3})").matcher(pin);
        	Matcher ma7 = Pattern.compile("(\\d+)-(\\d{3}[A-Z]?)-([A-Z])-(\\d{3}[A-Z]?)-(\\d{3}\\.\\d{2})").matcher(pin);
        	
        	if (ma1.matches()) {			//PIN from NB with dashes without group
        		controlMap = ma1.group(1);
        		parcel = ma1.group(2);
        		found = true;
        	} else if (ma2.matches()) {		//PIN from NB with dashes with group
        		controlMap = ma2.group(1);
        		group = ma2.group(2);
        		parcel = ma2.group(3);
        		found = true;
        	} else if (ma3.matches()) {		//PIN from NB without dashes
        		controlMap = ma3.group(1);
        		group = ma3.group(2);
        		parcel = ma3.group(3);
        		found = true;
        	} else if (ma4.matches()) {		//PIN from AO
        		controlMap = ma4.group(1);
        		group = ma4.group(2);
        		parcel = ma4.group(3);
        		found = true;
        	} else if (ma5.matches()) {		//PIN from TR without si
        		controlMap = ma5.group(1);
        		group = ma5.group(2);
        		parcel = ma5.group(3);
        		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
        		found = true;
        	} else if (ma6.matches()) {		//PIN from TR with si
        		controlMap = ma6.group(1);
        		group = ma6.group(2);
        		parcel = ma6.group(3);
        		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
        		found = true;
        	} else if (ma7.matches()) { //form of Alternate PIN from PRI
        		controlMap = ma7.group(2);
        		group = ma7.group(3);
        		parcel = ma7.group(5);
        		found = true;
        	}
			
			if (found)
	    	{
		    	m.getFunction(3).setData (controlMap);
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, controlMap);
		    	
		    	m.getFunction(4).setData (group);
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, group);

		    	m.getFunction(5).setData (parcel);
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);
	    	}
			
			m.getFunction(10).setDefaultValue("Owner");// 10. SortOptions
			m.getFunction(11).setDefaultValue(cntyCode);// 11. countylist
			
			l.add(m);
		}
		
		if(hasStreet())
		{//address
			
			if(hasStreetNo()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();

				m.getFunction(2).setParamValue(sa.getAtribute( SearchAttributes.P_STREETNAME ) + "% " + 
						sa.getAtribute( SearchAttributes.P_STREETNO ));
				
				
				m.getFunction(10).setDefaultValue("Owner");// 10. SortOptions
				
				m.getFunction(11).setDefaultValue(cntyCode);// 11. countylist

				m.addFilter(adressFilter);
				m.addFilter(nameFilterHybrid);
		    	l.add(m);
			}			
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(1).setSaKey("");// 1. FirstName
			m.getFunction(3).setSaKey("");// 3. Control Map
			m.getFunction(4).setSaKey("");// 4. Group
			m.getFunction(5).setSaKey("");// 5. Parcel ID
			m.getFunction(6).setSaKey("");// 6. SubdivisionName

			m.getFunction(2).setSaKey(SearchAttributes.P_STREETNAME);// 2. PropertyAddress
			m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE);
			
			m.getFunction(10).setDefaultValue("Owner");// 10. SortOptions
			m.getFunction(11).setDefaultValue(cntyCode);// 11. countylist

			m.addFilter(adressFilter);
			m.addFilter(nameFilterHybrid);
	    	l.add(m);
		}

		if( !emptySubdiv )
		{//address
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(1).setSaKey("");// 1. FirstName
			m.getFunction(2).setSaKey("");// 2. PropertyAddress
			m.getFunction(3).setSaKey("");// 3. Control Map
			m.getFunction(4).setSaKey("");// 4. Group
			m.getFunction(5).setSaKey("");// 5. Parcel ID

			m.getFunction(6).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
			m.getFunction(6).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
			
			m.getFunction(10).setDefaultValue("Owner");// 10. SortOptions
			m.getFunction(11).setDefaultValue(cntyCode);// 11. countylist

			m.addFilter(adressFilter);
			m.addFilter(nameFilterHybrid);
	    	l.add(m);
		}

		if( hasOwner() )
        {//owner
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			m.getFunction(10).setDefaultValue("Owner");// 10. SortOptions
			m.getFunction(11).setDefaultValue(cntyCode);// 11. countylist
			
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , m );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			m.addFilter(adressFilter);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			//m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L F M;;", "L F;;", "L f;;", "L M;;", "L m;;"});
		
			m.addIterator(nameIterator);
			
			l.add(m);
        }

		serverInfo.setModulesForAutoSearch(l);
}

	protected String getFileNameFromLink(String url)
	{
		String keyCode = "File";
		String mradParcelID = org.apache.commons.lang.StringUtils.substringBetween(
				url,
				"mradParcelID=",
				"&");
		String parts[] = mradParcelID.split(" ");
		
		if(parts.length>0 && parts[0].length()>4)
		{
			keyCode = parts[0].substring(4) + " ";
			
			for (int i=1 ; i < parts.length-1 ; i++)
			{
				if(parts[i].startsWith("000"))
				{
					keyCode += "000 "+" "+parts[i].substring(4)+" ";
				}
				else
					keyCode += parts[i]+" "; 
			}//			keyCode = keyCode.replaceAll("\\.+", " ");
			
			keyCode = keyCode.trim();
			keyCode = keyCode.replaceAll("\\s+", "_");
		}
		
		return keyCode+".html";
	}

}
