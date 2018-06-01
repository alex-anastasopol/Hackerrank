package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


public class TNHamiltonEP extends TSServer {
	
	static final long serialVersionUID = 10000000;
	private boolean downloadingForSave = false;
	
	

	public TNHamiltonEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
	
	    super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	
	}
	/*
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = null;
		TSServerInfoModule simTmp = null;
		
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		int currentYear = cal.get( GregorianCalendar.YEAR );
		int currentMonth = cal.get( GregorianCalendar.MONTH ) + 1;
		if( currentMonth < 10 ) {
			currentYear --;
		}
		
		//PageZone root = null;
		//
		if (msiServerInfoDefault == null) {
			//SET SERVER
			//number of search modules
			msiServerInfoDefault = new TSServerInfo(4);
			//set Address
			msiServerInfoDefault.setServerAddress("propertytax.chattanooga.gov");
			//set link
			msiServerInfoDefault.setServerLink("http://propertytax.chattanooga.gov/");
			//set IP
			msiServerInfoDefault.setServerIP("propertytax.chattanooga.gov");
			
			try
			{
			
				{ //SET EACH SEARCH
					//Search by name
					simTmp = SetModuleSearchByName(5, msiServerInfoDefault, 
							TSServerInfo.NAME_MODULE_IDX, "/cgi-bin/address.idc", 
							TSConnectionURL.idPOST, "", "text_query");
					
					PageZone searchByName = new PageZone("Search by Owner Name", "Search by Owner Name", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null,HTMLObject.PIXELS ,true);
					
					HTMLControl textQuery = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "text_query", "Owner Name", 1, 1, 1, 1, 30,  null, simTmp.getFunction( 1 ), searchId );
					textQuery.setFieldNote( "Just a property owner's last name can be entered, but if the first name is known, enter it second without punctuation" );
					textQuery.setRequiredExcl( true );
					simTmp.getFunction( 1 ).setSaKey(SearchAttributes.OWNER_FULL_NAME);
					
					HTMLControl fieldName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name", "", 1, 1, 1, 1, 30,  "Owner_name_1", simTmp.getFunction( 2 ), searchId  );
					fieldName.setHiddenParam( true );
					
					HTMLControl fieldName1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name1", "", 1, 1, 1, 1, 30,  "Owner", simTmp.getFunction( 3 ), searchId);
					fieldName1.setHiddenParam( true );
					
					HTMLControl taxYear = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "tax_year", "", 1, 1, 1, 1, 30,  String.valueOf( currentYear ), simTmp.getFunction( 4 ), searchId);
					taxYear.setHiddenParam( true );
					
					searchByName.addHTMLObject( textQuery );
					searchByName.addHTMLObject( fieldName );
					searchByName.addHTMLObject( fieldName1 );
					searchByName.addHTMLObject( taxYear );
					
					simTmp.getFunction(2).setHiddenParam("field_name","Owner_name_1");
					simTmp.getFunction(3).setHiddenParam("field_name1","Owner");
					simTmp.getFunction(4).setHiddenParam("tax_year",String.valueOf( currentYear ));
					
					simTmp.setModuleParentSiteLayout(searchByName);	
				}
				
				{
					//address
					simTmp = SetModuleSearchByAddress(5, msiServerInfoDefault, 
							TSServerInfo.ADDRESS_MODULE_IDX, "/cgi-bin/address.idc", 
							TSConnectionURL.idPOST, "", "text_query");
					
					PageZone searchByAddress = new PageZone("Search by Street Name", "Search by Street Name", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null,HTMLObject.PIXELS ,true);
					HTMLControl textQuery = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "text_query", "Street Name", 1, 1, 1, 1, 30,  null, simTmp.getFunction( 1 ), searchId );
					textQuery.setFieldNote( "It is advisable to enter a street name without the street type" );
					textQuery.setRequiredExcl( true );
					simTmp.getFunction( 1 ).setSaKey(SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX);
//                    simTmp.getFunction( 1 ).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);
					
					HTMLControl fieldName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name", "", 1, 1, 1, 1, 30,  "PROPERTY_STREET_NAME", simTmp.getFunction( 2 ), searchId  );
					fieldName.setHiddenParam( true );
					
					HTMLControl fieldName1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name1", "", 1, 1, 1, 1, 30,  "STREET_NAME", simTmp.getFunction( 3 ), searchId);
					fieldName1.setHiddenParam( true );
					
					HTMLControl taxYear = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "tax_year", "", 1, 1, 1, 1, 30,  String.valueOf( currentYear ), simTmp.getFunction( 4 ), searchId);
					taxYear.setHiddenParam( true );
					
					searchByAddress.addHTMLObject( textQuery );
					searchByAddress.addHTMLObject( fieldName );
					searchByAddress.addHTMLObject( fieldName1 );
					searchByAddress.addHTMLObject( taxYear );
					
					
					simTmp.getFunction(2).setHiddenParam("field_name","PROPERTY_STREET_NAME");
					simTmp.getFunction(3).setHiddenParam("field_name1","STREET_NAME");
					simTmp.getFunction(4).setHiddenParam("tax_year",String.valueOf( currentYear ));
					
					simTmp.setModuleParentSiteLayout(searchByAddress);	
				}
				
				{
					//parcel id
					simTmp = SetModuleSearchByParcelNo(5, msiServerInfoDefault, 
							TSServerInfo.PARCEL_ID_MODULE_IDX, "/cgi-bin/both.idc", 
							TSConnectionURL.idPOST, "Parcel_no");
					
					PageZone searchByParcelId = new PageZone("Search by Tax Map Number", "Search by Tax Map Number", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null,HTMLObject.PIXELS ,true);
					
					HTMLControl parcelNo = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "Parcel_no", "Parcel", 1, 1, 3, 3, 30,  null, simTmp.getFunction( 0 ), searchId );
					parcelNo.setRequiredCritical( true );
					
					HTMLControl mapNo = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "Map_no", "Map", 1, 1, 1, 1, 30,  null, simTmp.getFunction( 1 ), searchId );
					mapNo.setRequiredCritical( true );
					
					HTMLControl groupNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Group_no", "Group", 1, 1, 2, 2, 30,  null, simTmp.getFunction( 2 ), searchId  );
//					groupNo.setRequiredCritical( true );
					
					HTMLControl textQuery = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "text_query", "", 1, 1, 1, 1, 30,  "", simTmp.getFunction( 3 ), searchId );
					textQuery.setHiddenParam( true );
					
					HTMLControl fieldName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name", "", 1, 1, 1, 1, 30,  "state_tax_map", simTmp.getFunction( 4 ), searchId );
					fieldName.setHiddenParam( true );
					
					searchByParcelId.addHTMLObject( parcelNo );
					searchByParcelId.addHTMLObject( mapNo );
					searchByParcelId.addHTMLObject( groupNo );
					searchByParcelId.addHTMLObject( textQuery );
					searchByParcelId.addHTMLObject( fieldName );
					
                    SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
                    
                    String condo = sa.getAtribute( SearchAttributes.LD_PARCELNO_CONDO ); 
                    
                    if( !"".equals( condo ) )
                    {
                        simTmp.getFunction( 0 ).setSaKey( SearchAttributes.LD_PARCELNO_PARCEL_CONDO );
                    }
                    else
                    {
                        simTmp.getFunction( 0 ).setSaKey( SearchAttributes.LD_PARCELNO_PARCEL );
                    }
                    
					simTmp.getFunction(1).setName("Map:");
					simTmp.getFunction(1).setParamName("Map_no");
                    simTmp.getFunction( 1 ).setSaKey( SearchAttributes.LD_PARCELNO_MAP );
					
					simTmp.getFunction(2).setName("Group:");
					simTmp.getFunction(2).setParamName("Group_no");
                    simTmp.getFunction( 2 ).setSaKey( SearchAttributes.LD_PARCELNO_GROUP );
					
					simTmp.getFunction(3).setHiddenParam("text_query", "");
					simTmp.getFunction(4).setHiddenParam("field_name", "state_tax_map");
					
					simTmp.setModuleParentSiteLayout(searchByParcelId);
				}
                {
                    //business name
                    simTmp = SetModuleSearchByName(5, msiServerInfoDefault, 
                            TSServerInfo.BUSINESS_NAME_MODULE_IDX, "/cgi-bin/address.idc", 
                            TSConnectionURL.idPOST, "", "text_query");
                    
                    PageZone searchByBusiness = new PageZone("searchByBusiness", "Search by Business Name", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null,HTMLObject.PIXELS ,true);
                    
                    HTMLControl textQuery = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "text_query", "Business Name", 1, 1, 1, 1, 30,  null, simTmp.getFunction( 1 ), searchId );
                    textQuery.setFieldNote( "Just a property owner's last name can be entered, but if the first name is known, enter it second without punctuation" );
                    textQuery.setRequiredExcl( true );
                    simTmp.getFunction( 1 ).setSaKey(SearchAttributes.OWNER_FULL_NAME);
                    
                    HTMLControl fieldName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name", "", 1, 1, 1, 1, 30,  "Owner_name_1", simTmp.getFunction( 2 ), searchId  );
                    fieldName.setHiddenParam( true );
                    
                    HTMLControl fieldName1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "field_name1", "", 1, 1, 1, 1, 30,  "Business", simTmp.getFunction( 3 ), searchId);
                    fieldName1.setHiddenParam( true );
                    
                    HTMLControl taxYear = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "tax_year", "", 1, 1, 1, 1, 30,  String.valueOf( currentYear ), simTmp.getFunction( 4 ), searchId);
                    taxYear.setHiddenParam( true );
                    
                    searchByBusiness.addHTMLObject( textQuery );
                    searchByBusiness.addHTMLObject( fieldName );
                    searchByBusiness.addHTMLObject( fieldName1 );
                    searchByBusiness.addHTMLObject( taxYear );
                    
                    simTmp.getFunction(2).setHiddenParam("field_name","Owner_name_1");
                    simTmp.getFunction(3).setHiddenParam("field_name1","Business");
                    simTmp.getFunction(4).setHiddenParam("tax_year",String.valueOf( currentYear ));
                    
                    simTmp.setModuleParentSiteLayout(searchByBusiness); 
                    
                }
			
			}
			catch( Exception e )
			{
			    e.printStackTrace();
			}
			msiServerInfoDefault.setupParameterAliases();
			setModulesForAutoSearch(msiServerInfoDefault);
		}
		return msiServerInfoDefault;
	}*/
    
	protected String GetInfo(String what) {
		
		try {
	        
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            String parcel = sa.getAtribute(SearchAttributes.LD_PARCELNO);
            parcel = parcel.replaceAll("(\\s+)", " ");
            String pid[] = parcel.split(" ");
            
            int mapIndex = 0, groupIndex = 1, parcelIndex = 2;
            if (pid.length == 2) // lipseste  group
            {
                mapIndex = 0;            
                parcelIndex = 1;
                
                groupIndex = - 1;
            }
            
            if ("MAP".equals(what))
            {
                return pid[mapIndex];
            }else
            if ("GROUP".equals(what))
            {
                return groupIndex == -1 || groupIndex >= pid.length ? "" : pid[groupIndex];
            }else
            if ("PARCEL".equals(what))
            {
                String p = pid[parcelIndex];
                if (p.endsWith(".00"))
                {
                    p = p.substring(0, p.lastIndexOf(".00"));
                }
                return p;
            }
            
		} catch (Exception e) {
    	//	e.printStackTrace();
    	}
            
        return "";
    }
	
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        
		Search s= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = s.getSa();
		
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		
		
		//nu mai fac cautare daca nu am city KANSAS
		if (sa.getAtribute(SearchAttributes.P_CITY).length() > 0 && 
		   !sa.getAtribute(SearchAttributes.P_CITY).toUpperCase().startsWith("CHATTANOOGA")) 
		{
		    serverInfo.setModulesForAutoSearch(l);
		    return;
		}

		TSServerInfoModule m;
		
        // search by map/group/parcel
        if(hasPin()){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
        	l.add(m);
        }
        
        //search by name
        if( hasOwner() ){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        m.clearSaKeys();
	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	        m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
			
			l.add(m);
        }
		
		// search by address
        if(hasStreet()){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	        m.getFunction( 1 ).setSaKey(SearchAttributes.P_STREETNAME);
	        m.getFunction( 0 ).setSaKey( "" );
	        m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
	        l.add(m);
        }
        
		serverInfo.setModulesForAutoSearch(l);				
	}

	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException {

		String sTmp = "", receiptNo = null;			
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;
		int istart=-1,iend=-1;
		HashMap props;
	
		props=new HashMap();
		props.put("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");
		
		switch (viParseID) {
			case ID_SEARCH_BY_NAME :
            case ID_SEARCH_BY_ADDRESS :
			    
			    Matcher m = Pattern.compile("Output = false(?s:.*?)end if").matcher(rsResponce);
				
			    sTmp = CreatePartialLink(TSConnectionURL.idGET);
				String rsRespPrel = "<TABLE BORDER=\"3\" CELLPADDING=\"2\" CELLSPACING=\"0\" BGCOLOR=\"#CECE94\">" +
										"<TR>" +
										"<TD>" +
										"<P ALIGN=\"center\"><B>Tax Map<BR>Number</B>" +
										"</TD>" +
										"<TD>" +
										"<P ALIGN=\"center\"><B>Owner Name</B>" +
										"</TD>" +
										"<TD>" +
										"<P ALIGN=\"center\"><B>Property Address</B>" +
										"</TD>" +
									"</TR>";
				
				while(m.find()) 
				{
					rsRespPrel += "<TR>";
					String s=m.group(), v[]={"","",""};
					Matcher m1=Pattern.compile(">([^>]*?)</A>").matcher(s);
					if (m1.find())
					{

						v[0]=m1.group(1).replaceAll("\\&nbsp;", " ").trim();
						rsRespPrel += "<TD>";
						v[0]=v[0]+"                  ".substring(v[0].length()); // trebuie facut link din el
						try{
							String url = v[0];
							url = java.net.URLEncoder.encode(url, "UTF-8");
							rsRespPrel += "<a href=\"" +sTmp + "/cgi-bin/both.idc&text_query=" + java.net.URLEncoder.encode(v[0], "UTF-8")/*"javascript:submitform('" + v[0]*/ + "\">" + v[0] + "</a></TD>";
							
						}
						catch (UnsupportedEncodingException e) {}
					}
					m1=Pattern.compile(">([^>]*?)</FONT>").matcher(s);
					if (m1.find()&m1.find())
					{
						rsRespPrel += "<TD>";
						v[1]=m1.group(1).replaceAll("\\&nbsp;", " ").trim();
						rsRespPrel += v[1] + "</TD>";
					}
					if (m1.find())
					{
						rsRespPrel += "<TD>";
						v[2]=m1.group(1).replaceAll("\\&nbsp;", " ").trim();
						rsRespPrel += v[2] + "</TD>";
					}
					rsRespPrel += "</TR>";
				}
				
				rsRespPrel += "</TABLE>";
				rsResponce = rsRespPrel;

				parser.Parse(Response.getParsedResponse(), rsResponce, 
						Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), 
						TSServer.REQUEST_SAVE_TO_TSD);
				
				break;			
			case ID_DETAILS :
			case ID_SEARCH_BY_PARCEL :
               
				for (int i = 0; i < record_status_ind.length; i++)
					record_status_ind[i] = 0;
				
				initData();
				
				String prelResponce1 ="";
				istart = rsResponce.indexOf("<h4>&nbsp;&nbsp;Tax Map Number");
				iend = rsResponce.indexOf("</h4>");
				prelResponce1 = rsResponce.substring(istart,iend) + "</h4><br><p>";
				
				istart = rsResponce.indexOf("<table ", istart);
				
				iend = rsResponce.toLowerCase().indexOf("</table>", istart+1);
				prelResponce1 +=  rsResponce.substring(istart,iend) + "</table><br><p>";
					
				prelResponce1 += "<table BORDER=\"3\" CELLPADDING=\"2\" CELLSPACING=\"0\" BGCOLOR=\"#CECE94\">"+
									 "<tr>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Tax Year</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Bill Number</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Payment Date</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Assessed Value</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Assessed Taxes</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Tax Amount Due</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Tax Amount Paid</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Storm<br>"+
									 "Water<br>"+
									 "Assessed</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Storm<br>"+
									 "Water<br>"+
									 "Due</b></td>"+
									 "<td ALIGN=\"center\" VALIGN=\"bottom\"><b>Storm<br>"+
									 "Water<br>"+
									 "Paid</b></td>"+
									 "</tr>";
					
				m = Pattern.compile("<script language=\"VBScript\">(?s:.*?)</script>").matcher(rsResponce);
				while(m.find()) 
				{
					String s=m.group();
					if (s.indexOf("If max_year = 0 then") >= 0)
					{
						// parse data
						Matcher m1=Pattern.compile("Ctr1 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("bill_nbr\\(Ctr1\\) = (\\d+)").matcher(s);
						if (!m1.find())
							continue;
						int bill=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						List l=new ArrayList();
						while (m1.find()) {
							l.add(new Double(m1.group(1).length()>0?m1.group(1):"0"));
						}
						double[] v=new double[l.size()];
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();
						vbs1(year, bill, v);
					}
					else if (s.indexOf("If Ctr2 <> Prev_tax_year_2 then") >= 0) {
						// parse data
						Matcher m1=Pattern.compile("Ctr2 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						List l=new ArrayList();
						while (m1.find()) {
							l.add(new Double(m1.group(1).length()>0?m1.group(1):"0"));
						}
						double[] v=new double[l.size()];
						m1=Pattern.compile("(?m)work_date = \"(.*?) ").matcher(s);
						if (!m1.find())
							continue;
						String d1=m1.group(1);
						
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();
						vbs2(year, v, d1);
					}
					else if (s.indexOf("If Ctr3 <> Prev_tax_year_3 then") >= 0)
					{
						// parse data
						Matcher m1=Pattern.compile("Ctr3 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						List l=new ArrayList();
						while (m1.find()) {
							l.add(new Double(m1.group(1).length()>0?m1.group(1):"0"));
						}
						double[] v=new double[l.size()];
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();
						vbs3(year, v);
					}		
					else if (s.indexOf("If Ctr4 <> Prev_tax_year_4 then") >= 0)
					{
						// parse data
						Matcher m1=Pattern.compile("Ctr4 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						List l=new ArrayList();
						while (m1.find()) {
							l.add(new Double(m1.group(1).length()>0?m1.group(1):"0"));
						}
						m1=Pattern.compile("(?m)work_date = \"(.*?) ").matcher(s);
						if (!m1.find())
							continue;
						String d1=m1.group(1);
						if (!m1.find())
							continue;
						String d2=m1.group(1);
						
						double[] v=new double[l.size()];
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();

						vbs4(year, v, d1, d2);
					}
					else if (s.indexOf("If Ctr5 <> Prev_tax_year_5 then") >= 0)
					{
						// parse data
						Matcher m1=Pattern.compile("Ctr5 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						m1=Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						List l=new ArrayList();
						while (m1.find()) {
							l.add(new Double(m1.group(1).length()>0?m1.group(1):"0"));
						}
						double[] v=new double[l.size()];
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();
						vbs5(year, v);
					}
					else if (s.indexOf("If Ctr6 <> Prev_tax_year_6 then") >= 0) {
						// parse data
						Matcher m1=Pattern.compile("Ctr6 = (\\d{4})").matcher(s);
						if (!m1.find())
							continue;
						int year=Integer.parseInt(m1.group(1));
						
						
						Prev_tax_year_6 = year;
						//record_status_ind[year - starting_year] = 0;
						                  
						m1 = Pattern.compile("(?m)x = \"(.*?)\"").matcher(s);
						if (m1.find()) {
							String x = m1.group(1);
							if (" ".equals(x) || "".equals(x) || "I".equals(x) ) x = "1";
							record_status_ind[year - starting_year] = Double.parseDouble(x);
						}
						/*double[] v=new double[l.size()];
						for (int i = 0 ;i < l.size(); i++ )
							v[i] = ((Double)l.get(i)).doubleValue();
						vbs4(year, v);*/
					}
					
				}
				prelResponce1 += vbsFinalize();

				receiptNo = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("Tax Map Number: &quot;", "&quot;", rsResponce).replaceAll(" ", "");
				rsResponce = prelResponce1;
				
				rsResponce = rsResponce.replaceAll("</tr><td", "</tr><tr><td");
				
                if (rsResponce.indexOf("Tax Map Number: &quot;&quot;")!= -1)
                        return;
				if ((!downloadingForSave)){
				    String qry=Response.getQuerry();
					qry="dummy="+receiptNo+"&"+qry;
					String originalLink = sAction+"&"+qry;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					if (FileAlreadyExist(receiptNo + ".html") ) {
						rsResponce += CreateFileAlreadyInTSD();
					} else {
						rsResponce =  addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
					}
					
                    Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);                    
					
				} else {
					
					//for html
					msSaveToTSDFileName = receiptNo + ".html";
	
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

					msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
															
					parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
				}

				break;
			case ID_GET_LINK :
			case ID_SAVE_TO_TSD :
			    if (sAction.equals("/trustee/inquiry/main.aspx?m=2&b=2") || sAction.equals("/trustee/inquiry/main.aspx?m=2&b=4"))
			        ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);			  
				else
				    if (viParseID == ID_GET_LINK)
				        ParseResponse(sAction, Response, ID_DETAILS);
				    else {// on save
				        downloadingForSave = true;
						ParseResponse(sAction, Response, ID_DETAILS);
						downloadingForSave = false;
					   }
				break;
			default :
				break;
		}
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
           p.splitResultRows(pr, htmlString, pageId, "<TR","</TR", linkStart,  action);
           
           Vector rows = pr.getResultRows();

           rows = pr.getResultRows();
        if (rows.size() > 0)
        {
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0);           
            pr.setResultRows(rows);         
        }
    }
	
    private static int starting_year = 1960;
	private int Max_year = 0;
	private int Min_year = 3000;
	private int Prev_tax_year_1 = 0;
	private int Prev_tax_year_2 = 0;
	private int Prev_tax_year_3 = 0;
	private int Prev_tax_year_4 = 0;
	private int Prev_tax_year_5 = 0;
	private int Prev_tax_year_6 = 0;

	
	private int INTERVAL = 55;
	
	private int  work_date;
	private String  delinquent_payment_date[] = new String[INTERVAL];
	private int  bill_nbr[] = new int[INTERVAL];
	private int  tax_year[] = new int[INTERVAL];	
	private double  assessed_value[] = new double[INTERVAL];
	private double  assessed_taxes[] = new double[INTERVAL];
	private double  tax_amount_paid[] = new double[INTERVAL];
	private double  tax_amount_due[] = new double[INTERVAL];
	private double  acv_amount_credited[] = new double[INTERVAL];
	private double  penalty_amount_paid[] = new double[INTERVAL];
	private double  discount_amount_credited[] = new double[INTERVAL];
	private double  court_cost_paid[] = new double[INTERVAL];
	private double  delq_tax_amount_paid[] = new double[INTERVAL];
	private double  delq_penalty_amount_paid[] = new double[INTERVAL];
	private double  commission_fee_paid[] = new double[INTERVAL];
	private double  overpay_amount_paid[] = new double[INTERVAL];
	private double  delq_overpay_amount_paid[] = new double[INTERVAL];
	private double  assessment_change_amount[] = new double[INTERVAL];
	private double  tax_refund_amount[] = new double[INTERVAL];
	private double  overpayment_refund_amount[] = new double[INTERVAL];
	private double  discount_refund_amount[] = new double[INTERVAL];
	private double  commission_fee_refund_amount[] = new double[INTERVAL];
	private double  tax_change_amount[] = new double[INTERVAL];
	private double  stormwater_double_amt_due[] = new double[INTERVAL];
	private double  stormwater_fee_amt_due[] = new double[INTERVAL];
	private double  stormwater_fee_amt_paid[] = new double[INTERVAL];
	private double  stormwater_int_amt_paid[] = new double[INTERVAL];
	private double  stormwater_fee_refund_amount[] = new double[INTERVAL];
	private double  stormwater_int_refund_amount[] = new double[INTERVAL];
	private double  stormwater_fee_assessed[] = new double[INTERVAL];
	private double  stormwater_interest_compounded[] = new double[INTERVAL];
	private double  second_address_line[] = new double[INTERVAL];
	private double  record_status_ind[] = new double[INTERVAL];
	private double  penalty_refund_amount[] = new double[INTERVAL];
	
	
	private String business_payment_date[] = new String[INTERVAL];
	private String calendar_payment_date[] = new String[INTERVAL];
	
	private void initData() {
		
		delinquent_payment_date = new String[INTERVAL];
		bill_nbr = new int[INTERVAL];
		tax_year = new int[INTERVAL];	
		assessed_value = new double[INTERVAL];
		assessed_taxes = new double[INTERVAL];
		tax_amount_paid = new double[INTERVAL];
		tax_amount_due = new double[INTERVAL];
		acv_amount_credited = new double[INTERVAL];
		penalty_amount_paid = new double[INTERVAL];
		discount_amount_credited = new double[INTERVAL];
		court_cost_paid = new double[INTERVAL];
		delq_tax_amount_paid = new double[INTERVAL];
		delq_penalty_amount_paid = new double[INTERVAL];
		commission_fee_paid = new double[INTERVAL];
		overpay_amount_paid = new double[INTERVAL];
		delq_overpay_amount_paid = new double[INTERVAL];
		assessment_change_amount = new double[INTERVAL];
		tax_refund_amount = new double[INTERVAL];
		overpayment_refund_amount = new double[INTERVAL];
		discount_refund_amount = new double[INTERVAL];
		commission_fee_refund_amount = new double[INTERVAL];
		tax_change_amount = new double[INTERVAL];
		stormwater_double_amt_due = new double[INTERVAL];
		stormwater_fee_amt_due = new double[INTERVAL];
		stormwater_fee_amt_paid = new double[INTERVAL];
		stormwater_int_amt_paid = new double[INTERVAL];
		stormwater_fee_refund_amount = new double[INTERVAL];
		stormwater_int_refund_amount = new double[INTERVAL];
		stormwater_fee_assessed = new double[INTERVAL];
		stormwater_interest_compounded = new double[INTERVAL];
		second_address_line = new double[INTERVAL];
		record_status_ind = new double[INTERVAL];
		penalty_refund_amount = new double[INTERVAL];

		business_payment_date = new String[INTERVAL];
		calendar_payment_date = new String[INTERVAL];
		
	}
	
        /*
         *  v[0] = assessed_value
         *  v[1] = assessed_taxes
         *  v[2] = tax_amount_paid
         *  v[3] = acv_amount_credited
         *  v[4] = penalty_amount_paid
         *  v[5] = stormwater_fee_amt_paid
         *  v[6] = stormwater_int_amt_paid
         *  v[7] = discount_amount_credited
         *  v[8] = overpay_amount_paid
         */
       private void vbs1(int Ctr1, int billAmt, double v[])
       {
       	
    	   if (v == null || v.length == 0)
    		   return;
    	   
			if (Max_year == 0)
				Max_year = Ctr1;		
			Min_year = Ctr1;
			
			tax_year[Ctr1 - starting_year] = Ctr1;
	
			bill_nbr[Ctr1 - starting_year] = billAmt;
	
			double x = v[0];		
			assessed_value[Ctr1 - starting_year] = assessed_value[Ctr1 - starting_year] + x;
			
	
			x = v[1];
			assessed_taxes[Ctr1 - starting_year] = assessed_taxes[Ctr1 - starting_year] + x;
			
	
			if (Ctr1 != Prev_tax_year_1)
			{		
				x = v[2];					
				tax_amount_paid[Ctr1 - starting_year] = tax_amount_paid[Ctr1 - starting_year] + x; 
		
			
				x = v[3];			
				acv_amount_credited[Ctr1 - starting_year] = acv_amount_credited[Ctr1 - starting_year] + x;
		
				x = v[4];			
				penalty_amount_paid[Ctr1 - starting_year] = penalty_amount_paid[Ctr1 - starting_year] + x;
		
				x = v[5];			
				stormwater_fee_amt_paid[Ctr1 - starting_year] = stormwater_fee_amt_paid[Ctr1 - starting_year] + x;
		
				x = v[6];			
				stormwater_int_amt_paid[Ctr1 - starting_year] = stormwater_int_amt_paid[Ctr1 - starting_year] + x;
		
				x = v[7];		
				discount_amount_credited[Ctr1 - starting_year] = discount_amount_credited[Ctr1 - starting_year] + x;	
				
				x = v[8];			
				overpay_amount_paid[Ctr1 - starting_year] = overpay_amount_paid[Ctr1 - starting_year] + x;
		
				Prev_tax_year_1 = Ctr1;
			}					
       }
       
       private void vbs2(int Ctr2, double v[], String delinq_payment_date) {
    	   
    	   if (v == null || v.length == 0)
    		   return;
          	   
    	   if (Ctr2 != Prev_tax_year_2) {
    		   
	    	   double x = v[0];
	    	   court_cost_paid[Ctr2 - starting_year] = court_cost_paid[Ctr2 - starting_year] + x;
	
	    	   x = v[1];
	    	   delq_tax_amount_paid[Ctr2 - starting_year] = delq_tax_amount_paid[Ctr2 - starting_year] + x;
	
	    	   x = v[2];
	    	   delq_penalty_amount_paid[Ctr2 - starting_year] = delq_penalty_amount_paid[Ctr2 - starting_year] + x;
	
	    	   x = v[3];
	    	   commission_fee_paid[Ctr2 - starting_year] = commission_fee_paid[Ctr2 - starting_year] + x;
	
	    	   x = v[4];
	    	   delq_overpay_amount_paid[Ctr2 - starting_year] = delq_overpay_amount_paid[Ctr2 - starting_year] + x;
	
	    	   delinquent_payment_date[Ctr2 - starting_year] = delinq_payment_date;
	    	   
	    	   Prev_tax_year_2 = Ctr2;
    	   }
       }
    	   
       /*
        * v[0] = assessment_change_amount
        * v[1] = tax_change_amount
        */
       private void vbs3(int Ctr3, double v[])
       {       	
    	   if (v == null || v.length == 0)
    		   return;
    	   
			if (Ctr3 != Prev_tax_year_3)
			{
				double x = v[0];
			
				assessment_change_amount[Ctr3 - starting_year] = assessment_change_amount[Ctr3 - starting_year] + x;
				
				x = v[1];
				
				tax_change_amount[Ctr3 - starting_year] = tax_change_amount[Ctr3 - starting_year] + x;
				
				Prev_tax_year_3 = Ctr3;			
			}
       }
       /*
        * v[0] = tax_refund_amount
        * v[1] = penalty_refund_amount
        * v[2] = overpayment_refund_amount
        * v[3] = discount_refund_amount
        * v[4] = commission_fee_refund_amount
        * v[5] = stormwater_fee_refund_amount
        * v[6] = stormwater_int_refund_amount
        * date1 = business_payment_date
        * date2 = calendar_payment_date
        * 
        */
       private void vbs4(int Ctr4, double v[], String date1, String date2)
       {

    	   if (v == null || v.length == 0)
    		   return;

				if (Ctr4 != Prev_tax_year_4)
				{
				double x = v[0];				
				tax_refund_amount[Ctr4 - starting_year] = tax_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[1];				
				penalty_refund_amount[Ctr4 - starting_year] = penalty_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[2];				
				overpayment_refund_amount[Ctr4 - starting_year] = overpayment_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[3];
				discount_refund_amount[Ctr4 - starting_year] = discount_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[4];
				commission_fee_refund_amount[Ctr4 - starting_year] = commission_fee_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[5];
				stormwater_fee_refund_amount[Ctr4 - starting_year] = stormwater_fee_refund_amount[Ctr4 - starting_year] + x;
				
				x = v[6];
				stormwater_int_refund_amount[Ctr4 - starting_year] = stormwater_int_refund_amount[Ctr4 - starting_year] + x;
							
				business_payment_date[Ctr4 - starting_year] = date1;	
							
				calendar_payment_date[Ctr4 - starting_year] = date2;
				
				Prev_tax_year_4 = Ctr4;
				
				}
       }
       
       /*
        * v[0] = stormwater_fee_assessed
        * v[1] = stormwater_interest_compounded
        */
       private void vbs5(int Ctr5, double v[])
       {
		
    	   if (v == null || v.length == 0)
    		   return;
    	   
			if (Ctr5 != Prev_tax_year_5)
			{
				double x = v[0]/**100*/;
		
				if (x < 5)
					x = 0;
								
				stormwater_fee_assessed[Ctr5 - starting_year] = stormwater_fee_assessed[Ctr5 - starting_year] + x;
		
				//x = v[1]*100;		
					
				stormwater_interest_compounded[Ctr5 - starting_year] = stormwater_interest_compounded[Ctr5 - starting_year] + x;
		
				Prev_tax_year_5 = Ctr5;
		
			}
       }
       
       /*
        *  ia toate datele si creaza tabelul cu ele 
        */
       protected String vbsFinalize()
       {
       	String result = "";       	
        DecimalFormat decimalFormat = new DecimalFormat("############.00");
       	for (int i = Max_year;i >= Min_year; i--)
       	{
			if (tax_year[i - starting_year] > 0)
			{
				result +="<td nowrap align='center'><font size='1'>&nbsp;" + tax_year[i - starting_year] + "&nbsp;</font></td>";
				result +="<td nowrap align='center'><font size='1'>&nbsp;" + bill_nbr[i  - starting_year] + "&nbsp;</font></td>";
				assessed_value[i - starting_year] = assessed_value[i - starting_year] - assessment_change_amount[i - starting_year];

					  //if (delinquent_payment_date[i - starting_year] > 0)					  
						 /* if (record_status_ind[i - starting_year] = "1" or record_status_ind(i) = "I") then
						  delinquent_payment_date(i) = "**"
						  end if
						  */
					//	  result+= "<td nowrap align='center'><font size='1'>&nbsp;" + 
					//		  (delinquent_payment_date[i - starting_year]) +
					//		   "&nbsp;</font></td>"; 
					//  else
							   /*if (record_status_ind(i) = "1" or record_status_ind(i) = "I") then
							   business_payment_date(i) = "**"
							   end if
							   */
				
				String paymentDate =  business_payment_date[i- starting_year];
				if(paymentDate==null)
					paymentDate = delinquent_payment_date[i - starting_year];
				if(paymentDate==null)
					paymentDate = "";
				
				result += "<td nowrap align='center'><font size='1'>&nbsp;" + paymentDate + "&nbsp;</font></td>";            
					  
					  result +="<td nowrap align='right'><font size='1'>&nbsp;$" + assessed_value[i- starting_year] + "&nbsp;</font></td>";
					   assessed_taxes[i- starting_year] = assessed_taxes[i- starting_year] - tax_change_amount[i- starting_year];
					   result +="<td nowrap align='right'><font size='1'>&nbsp;$" + assessed_taxes[i- starting_year]/**100*/ + "&nbsp;</font></td>";
					   tax_amount_paid[i- starting_year] = tax_amount_paid[i- starting_year] + delq_tax_amount_paid[i- starting_year] + 
					   										discount_amount_credited[i- starting_year] +  acv_amount_credited[i- starting_year] + 
					   										overpay_amount_paid[i- starting_year] + delq_overpay_amount_paid[i- starting_year] 
					   										- (tax_refund_amount[i- starting_year] + overpayment_refund_amount[i- starting_year] + 
					   										discount_refund_amount[i- starting_year]);
					   										
                       tax_amount_paid[ i - starting_year ] = Double.parseDouble( decimalFormat.format( tax_amount_paid[ i - starting_year ] ) );
                       
					   discount_refund_amount[i- starting_year] = assessed_taxes[i- starting_year] - tax_amount_paid[i- starting_year];
					   tax_amount_due[i- starting_year] = assessed_taxes[i- starting_year] - tax_amount_paid[i- starting_year];
					   
					   /*tax_amount_due[i- starting_year] = tax_amount_due[i- starting_year] * 100;
				       tax_amount_due[i- starting_year] = tax_amount_due[i- starting_year];
				       tax_amount_due[i- starting_year] = tax_amount_due[i- starting_year] / 100;*/
				       //if (record_status_ind[i- starting_year] == 1) {
				    	   //bug 1095
				    	   //tax_amount_due[i- starting_year] = 0;
				       //}
					     
					   //tax_amount_due[i- starting_year] = tax_amount_duei) * 100
					   //tax_amount_due(i) = clng(tax_amount_due(i))
					   //tax_amount_due(i) = tax_amount_due(i) / 100
					   //if (record_status_ind(i) = "1" or record_status_ind(i) = "I") then
					   //tax_amount_due(i) = 0.00
					   //end if
					  result += "<td nowrap align='right'><font size='1'>&nbsp;$" + tax_amount_due[i- starting_year]/**100*/ + "&nbsp;</font></td>";
					  result += "<td nowrap align='right'><font size='1'>&nbsp;$" + tax_amount_paid[i- starting_year]/**100*/ + "&nbsp;</font></td>";

					   stormwater_fee_amt_due[i- starting_year] = stormwater_fee_assessed[i- starting_year] - 
			   													(stormwater_fee_amt_paid[i- starting_year]/**100*/ + 
			   													stormwater_fee_refund_amount[i- starting_year]);	 

//					   stormwater_fee_amt_due(i) = stormwater_fee_amt_due(i) * 100
//					   stormwater_fee_amt_due(i) = clng(stormwater_fee_amt_due(i))
//					   stormwater_fee_amt_due(i) = stormwater_fee_amt_due(i) / 100
         
					   if (stormwater_fee_assessed[i- starting_year] > 0)
						 result += "<td nowrap align='right'><font size='1'>&nbsp;$" + stormwater_fee_assessed[i- starting_year] + "&nbsp;</font></td>";
					   else
						 result += "<td nowrap align='right'><font size='1'>&nbsp;</td>";
					   
         
					   result += "<td nowrap align='right'><font size='1'>&nbsp;$" + stormwater_fee_amt_due[i- starting_year] + "&nbsp;</font></td>";
					   result += "<td nowrap align='right'><font size='1'>&nbsp;$" + stormwater_fee_amt_paid[i- starting_year]/**100*/ + "&nbsp;</font></td>";


						result += "</tr>";
			}
       	}
       	
		result +="</TABLE>";       	
       	return result;
       
       }
}
