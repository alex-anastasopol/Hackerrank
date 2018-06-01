package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

public class TNHamiltonTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	private boolean downloadingForSave = false;
	
    	
	public TNHamiltonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
	    super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@SuppressWarnings("rawtypes")
	private ATSConn makeconnection(String link,String qry,int type,HashMap params,HashMap props)
	{
		ATSConn atc=new ATSConn(666,link,type,params,props,searchId,miServerID);
		atc.setEncQuerry(qry);
		atc.doConnection();
		return atc;
	}
	/*
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = null;
		TSServerInfoModule simTmp = null;
		PageZone root = null;
		
		// SET SERVER
		if (msiServerInfoDefault == null) {
			
			//number of search modules
			msiServerInfoDefault = new TSServerInfo(3);
			//set Address
			msiServerInfoDefault.setServerAddress("www2.hamiltontn.gov");
			//set link  /TrusteeInquiry/AppFolder/PropertySearch.aspx
			msiServerInfoDefault.setServerLink("http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertySearch.aspx");
			//set IP
			msiServerInfoDefault.setServerIP("www2.hamiltontn.gov");
			
			try
			{
			
				root = new PageZone("root", "", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(600),HTMLObject.PIXELS , false);
				root.setBorder(false);
				
				
				{ //SET EACH SEARCH
					//Search by name
					simTmp = SetModuleSearchByName(7, msiServerInfoDefault, 
							TSServerInfo.NAME_MODULE_IDX, 
							"/TrusteeInquiry/AppFolder/PropertySearch.aspx", 
							TSConnectionURL.idPOST, "", "ctl00$MainContent$txtLName");

					PageZone searchByName = new PageZone("Search by Name", "Search by Name", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null,HTMLObject.PIXELS ,true);
					
					HTMLControl name = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$txtLName", "Name", 1, 1, 1, 1, 30,  null, simTmp.getFunction( 1 ), searchId );
					name.setFieldNote( "Names should be entered in Last Name SPACE First Name format" );
					name.setRequiredExcl( true );
					name.setJustifyField( true );
					simTmp.getFunction( 1 ).setSaKey(SearchAttributes.OWNER_FULL_NAME);
					
					HTMLControl stateCtl = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 1, 1, 30, 
							"YaBPACnXlZ1hBhhAbBlVtJclgCGzFPlirFMeVpdc4eYkJmmFatKqYFa0XFly+4vOrwDpJjEL8a7MC7hb4iCU7JbKi9bzadjp5NcdftBXNjf99JeGeXD0FMn4jmZl4kRbT3qHFAHF/p52vO9gUml24I90Vi36pmRJhpqgH/4Xej4lPknGKO8qWOjDCkPzKS3vYvhL7mt0jhNpiYiKxM+9vHZk3VHs/aFtZBNWMoV9jEVACAp4XhZOrgLGaQuwjgt6Pnhje649FzBLcKnsaiE36CTb1YOy26tfLHOJTjy7AmHqAX5Q4p25qqmpzoFOKlYYbz6bjxaa6K4ZQWDn20zaMpQGBWKaTNp7/KGW4YPRDAm/FXgaEex09SxTw56z2aG8FBA7mHB1lDsWWcDABsdIvG5W6qW9FTL9a21rDdC8PYR0zbibHhfFm+vEMMCkOUhXgKwKVugMg4oRtkVK30vBAyokmnhqpBcrvtKgqfO8a5BBDbxaUT482SR6VqrS4rLaJXsAcJb8KszK2kfJLnvr6ECxmHjQFVRKDsBtC1idlNX4nwZPd5R4fJ/qxNceifaTxUIjLRVPmgmc0Q/z2O+lr6B04n4cITIxj0YgzfKBlX+W/xILu7py7ULtyizuHDE2phJP5i9i0xo4q5nlC+oKO4eRMng0HHCNRPYAVQAr8wZaq+ZvvUAlQUdP17V4lSiRUXSgIftbAcw4xg3VwBdSJfi4zW0peh5QRQWjuMHTR7LP9fDpzf4eGk1QE+TrZvQIkL23GemMztYMkqzLOb+eqpk6cAeI/ieA0otvEGOidfZj9XgtEAxTZl7BpK+xhXtdLdfm7nu2VQgES8SqYQFit6EWl7XjLP0wvu1OK85E8zfj1OIcg6+E8icY1X+foZt/NskXqiM7+3dD5v0wlWdkV8LJa+noND6WhfoaTg+r+CPKD6WPRY7IFWR15e1v64+S3rXDcYHw44woUiVs3ohpd1zzgIGlHYI0+icJOp3xbnk=", simTmp.getFunction( 2 ), searchId );
					stateCtl.setHiddenParam( true );
					
					HTMLControl searchCtl = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$cmdLName_Search", "", 1, 1, 1, 1, 30,  "Search", simTmp.getFunction( 3 ), searchId );
					searchCtl.setHiddenParam( true );
					
					HTMLControl eventArgument = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 2, 2, 30,  "", simTmp.getFunction( 4 ), searchId );
					eventArgument.setHiddenParam( true );
					eventArgument.setJustifyField( true );
					
                    HTMLControl eventValidation = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTVALIDATION", "", 1, 1, 1, 1, 30,  
                    		"PxdDBMk3VOEGaT3DFL8BeWIyrAMX/KDR+WxNrCw4QPmP5e9WHCi8KksrgXr7XC92uPSIR3e9njmL71r6gNzHug==", simTmp.getFunction( 5 ), searchId );
                    eventValidation.setHiddenParam( true );

                    HTMLControl viewStateEncripted = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATEENCRYPTED", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 6 ), searchId );
                    viewStateEncripted.setHiddenParam( true );
                    
					searchByName.addHTMLObject( name );
					searchByName.addHTMLObject( stateCtl );
					searchByName.addHTMLObject( searchCtl );
					searchByName.addHTMLObject( eventArgument );
                    searchByName.addHTMLObject( eventValidation );
                    searchByName.addHTMLObject( viewStateEncripted );
					
					root.addHTMLObject( searchByName );
									                    
					simTmp.setModuleParentSiteLayout(searchByName);	
				}
				
				
				{ 
					//Search by Property Address
					simTmp = SetModuleSearchByAddress(9, msiServerInfoDefault, 
							TSServerInfo.ADDRESS_MODULE_IDX, 
							"/TrusteeInquiry/AppFolder/PropertySearch.aspx", 
							TSConnectionURL.idPOST, "", "ctl00$MainContent$txtPropAddress");
					
					PageZone searchByAddress = new PageZone("Search by Property Address", "Search by Property Address", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null, HTMLObject.PIXELS ,true);
					
					HTMLControl streetName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$txtPropAddress", "Street Address", 1, 1, 1, 1, 30, null, simTmp.getFunction( 1 ), searchId );
					streetName.setFieldNote( "Street names should be entered WITHOUT street suffix, and must be EXACTLY what is stored on the property record" );
					streetName.setRequiredExcl( true );
					streetName.setJustifyField( true );
                    simTmp.getFunction( 1 ).setSaKey( SearchAttributes.P_STREET_FULL_NAME_EX );
					
					HTMLControl viewState = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 1, 1, 30, 
							"YaBPACnXlZ1hBhhAbBlVtJclgCGzFPlirFMeVpdc4eYkJmmFatKqYFa0XFly+4vOrwDpJjEL8a7MC7hb4iCU7JbKi9bzadjp5NcdftBXNjf99JeGeXD0FMn4jmZl4kRbT3qHFAHF/p52vO9gUml24I90Vi36pmRJhpqgH/4Xej4lPknGKO8qWOjDCkPzKS3vYvhL7mt0jhNpiYiKxM+9vHZk3VHs/aFtZBNWMoV9jEVACAp4XhZOrgLGaQuwjgt6Pnhje649FzBLcKnsaiE36CTb1YOy26tfLHOJTjy7AmHqAX5Q4p25qqmpzoFOKlYYbz6bjxaa6K4ZQWDn20zaMpQGBWKaTNp7/KGW4YPRDAm/FXgaEex09SxTw56z2aG8FBA7mHB1lDsWWcDABsdIvG5W6qW9FTL9a21rDdC8PYR0zbibHhfFm+vEMMCkOUhXgKwKVugMg4oRtkVK30vBAyokmnhqpBcrvtKgqfO8a5BBDbxaUT482SR6VqrS4rLaJXsAcJb8KszK2kfJLnvr6ECxmHjQFVRKDsBtC1idlNX4nwZPd5R4fJ/qxNceifaTxUIjLRVPmgmc0Q/z2O+lr6B04n4cITIxj0YgzfKBlX+W/xILu7py7ULtyizuHDE2phJP5i9i0xo4q5nlC+oKO4eRMng0HHCNRPYAVQAr8wZaq+ZvvUAlQUdP17V4lSiRUXSgIftbAcw4xg3VwBdSJfi4zW0peh5QRQWjuMHTR7LP9fDpzf4eGk1QE+TrZvQIkL23GemMztYMkqzLOb+eqpk6cAeI/ieA0otvEGOidfZj9XgtEAxTZl7BpK+xhXtdLdfm7nu2VQgES8SqYQFit6EWl7XjLP0wvu1OK85E8zfj1OIcg6+E8icY1X+foZt/NskXqiM7+3dD5v0wlWdkV8LJa+noND6WhfoaTg+r+CPKD6WPRY7IFWR15e1v64+S3rXDcYHw44woUiVs3ohpd1zzgIGlHYI0+icJOp3xbnk=", simTmp.getFunction( 2 ), searchId );
					viewState.setHiddenParam( true );

					HTMLControl searchCtl = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$cmdPropAddress_Search", "", 1, 1, 1, 1, 30, "Search", simTmp.getFunction( 3 ), searchId );
					searchCtl.setHiddenParam( true );
					
					HTMLControl lastFocus = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__LASTFOCUS", "", 1, 1, 2, 2, 30, "", simTmp.getFunction( 4 ), searchId );
					lastFocus.setHiddenParam( true );
					
                    HTMLControl eventValidation = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTVALIDATION", "", 1, 1, 1, 1, 30,  
                    		"PxdDBMk3VOEGaT3DFL8BeWIyrAMX/KDR+WxNrCw4QPmP5e9WHCi8KksrgXr7XC92uPSIR3e9njmL71r6gNzHug==", simTmp.getFunction( 5 ), searchId );
                    eventValidation.setHiddenParam( true );
                    
                    HTMLControl eventTarget = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTTARGET", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 6 ), searchId );
                    eventTarget.setHiddenParam( true );
                    
                    HTMLControl eventArgument = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 7 ), searchId );
                    eventArgument.setHiddenParam( true );
                    
                    HTMLControl viewStateEncripted = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATEENCRYPTED", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 8 ), searchId );
                    viewStateEncripted.setHiddenParam( true );
                    
					searchByAddress.addHTMLObject( streetName );
					searchByAddress.addHTMLObject( viewState );		
					searchByAddress.addHTMLObject( searchCtl );
					searchByAddress.addHTMLObject( lastFocus );
					searchByAddress.addHTMLObject( eventTarget );
					searchByAddress.addHTMLObject( eventArgument );
					searchByAddress.addHTMLObject( viewStateEncripted );
					
					root.addHTMLObject( searchByAddress );
					
					simTmp.setModuleParentSiteLayout(searchByAddress);
				}
				
				
				
	            {
	                //parcel id
	                simTmp = SetModuleSearchByParcelNo(10, msiServerInfoDefault, 
	                        TSServerInfo.PARCEL_ID_MODULE_IDX, 
	                        "/TrusteeInquiry/AppFolder/PropertySearch.aspx", 
	                        TSConnectionURL.idPOST, "ctl00$MainContent$txtParcel");

	                PageZone searchByParcelID = new PageZone( "Search by Parcel ID", "Search by Parcel ID", HTMLObject.ORIENTATION_HORIZONTAL, null, null, null, HTMLObject.PIXELS ,true );
	                
					HTMLControl parcelNumber = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$txtParcel", "Parcel", 1, 1, 3, 3, 30, null, simTmp.getFunction( 0 ), searchId );
					parcelNumber.setRequiredExcl( true );
					
					HTMLControl map = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$txtMap", "Map", 1, 1, 1, 1, 30, null, simTmp.getFunction( 1 ), searchId );
					map.setRequiredExcl( true );

					HTMLControl group = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$txtGroup", "Group", 1, 1, 2, 2, 30, null, simTmp.getFunction( 2 ), searchId );
					group.setRequiredExcl( true );
					
					HTMLControl viewState = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATE", "", 1, 1, 4, 4, 30, 
							"YaBPACnXlZ1hBhhAbBlVtJclgCGzFPlirFMeVpdc4eYkJmmFatKqYFa0XFly+4vOrwDpJjEL8a7MC7hb4iCU7JbKi9bzadjp5NcdftBXNjf99JeGeXD0FMn4jmZl4kRbT3qHFAHF/p52vO9gUml24I90Vi36pmRJhpqgH/4Xej4lPknGKO8qWOjDCkPzKS3vYvhL7mt0jhNpiYiKxM+9vHZk3VHs/aFtZBNWMoV9jEVACAp4XhZOrgLGaQuwjgt6Pnhje649FzBLcKnsaiE36CTb1YOy26tfLHOJTjy7AmHqAX5Q4p25qqmpzoFOKlYYbz6bjxaa6K4ZQWDn20zaMpQGBWKaTNp7/KGW4YPRDAm/FXgaEex09SxTw56z2aG8FBA7mHB1lDsWWcDABsdIvG5W6qW9FTL9a21rDdC8PYR0zbibHhfFm+vEMMCkOUhXgKwKVugMg4oRtkVK30vBAyokmnhqpBcrvtKgqfO8a5BBDbxaUT482SR6VqrS4rLaJXsAcJb8KszK2kfJLnvr6ECxmHjQFVRKDsBtC1idlNX4nwZPd5R4fJ/qxNceifaTxUIjLRVPmgmc0Q/z2O+lr6B04n4cITIxj0YgzfKBlX+W/xILu7py7ULtyizuHDE2phJP5i9i0xo4q5nlC+oKO4eRMng0HHCNRPYAVQAr8wZaq+ZvvUAlQUdP17V4lSiRUXSgIftbAcw4xg3VwBdSJfi4zW0peh5QRQWjuMHTR7LP9fDpzf4eGk1QE+TrZvQIkL23GemMztYMkqzLOb+eqpk6cAeI/ieA0otvEGOidfZj9XgtEAxTZl7BpK+xhXtdLdfm7nu2VQgES8SqYQFit6EWl7XjLP0wvu1OK85E8zfj1OIcg6+E8icY1X+foZt/NskXqiM7+3dD5v0wlWdkV8LJa+noND6WhfoaTg+r+CPKD6WPRY7IFWR15e1v64+S3rXDcYHw44woUiVs3ohpd1zzgIGlHYI0+icJOp3xbnk=", simTmp.getFunction( 3 ), searchId );
					viewState.setHiddenParam( true );					
					
					HTMLControl search = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "ctl00$MainContent$cmdMGP_Search", "", 1, 1, 4, 4, 30, "Search", simTmp.getFunction( 4 ), searchId );
					search.setHiddenParam( true );
					
                    HTMLControl eventValidation = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTVALIDATION", "", 1, 1, 1, 1, 30,  
                    		"PxdDBMk3VOEGaT3DFL8BeWIyrAMX/KDR+WxNrCw4QPmP5e9WHCi8KksrgXr7XC92uPSIR3e9njmL71r6gNzHug==", simTmp.getFunction( 5 ), searchId );
                    eventValidation.setHiddenParam( true );

                    HTMLControl eventTarget = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTTARGET", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 6 ), searchId );
                    eventTarget.setHiddenParam( true );
                    
                    HTMLControl eventArgument = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__EVENTARGUMENT", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 7 ), searchId );
                    eventArgument.setHiddenParam( true );
                    
                    HTMLControl viewStateEncripted = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__VIEWSTATEENCRYPTED", "", 1, 1, 1, 1, 30,  
                    		null, simTmp.getFunction( 8 ), searchId );
                    viewStateEncripted.setHiddenParam( true );
                    
					HTMLControl lastFocus = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "__LASTFOCUS", "", 1, 1, 2, 2, 30, "", simTmp.getFunction( 9 ), searchId );
					lastFocus.setHiddenParam( true );
                    
					searchByParcelID.addHTMLObject( parcelNumber );
					searchByParcelID.addHTMLObject( map );
					searchByParcelID.addHTMLObject( group );
					searchByParcelID.addHTMLObject( viewState );
					searchByParcelID.addHTMLObject( search );
					searchByParcelID.addHTMLObject( eventValidation );
					searchByParcelID.addHTMLObject( eventTarget );
					searchByParcelID.addHTMLObject( eventArgument );
					searchByParcelID.addHTMLObject( viewStateEncripted );
					searchByParcelID.addHTMLObject( lastFocus );
					
					
					root.addHTMLObject( searchByParcelID );					
					
					simTmp.setModuleParentSiteLayout(searchByParcelID);	
					
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
	                simTmp.getFunction(1).setParamName("ctl00$MainContent$txtMap");
                    simTmp.getFunction( 1 ).setSaKey( SearchAttributes.LD_PARCELNO_MAP );
	                
	                simTmp.getFunction(2).setName("Group:");
	                simTmp.getFunction(2).setParamName("ctl00$MainContent$txtGroup");
                    simTmp.getFunction( 2 ).setSaKey( SearchAttributes.LD_PARCELNO_GROUP );
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
	}
	*/
    protected String GetInfo(String what)
    {
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
	        	
	            String p = parcelIndex < 0 || parcelIndex >= pid.length ? "" : pid[parcelIndex];
	            if (p.endsWith(".00"))
	            {
	                p = p.substring(0, p.lastIndexOf(".00"));
	            }
	            return p;
	        }
	        
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        
        return "";
    }
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		boolean emptyMap = "".equals( GetInfo("MAP") );
		boolean emptyGroup = "".equals( GetInfo("GROUP") );
		boolean emptyParcel = "".equals( GetInfo("PARCEL") );
		
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		
		
		// search by map/group/parcel
		if( !emptyMap && !emptyGroup && !emptyParcel ){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
	        m.getFunction(0).setSaKey("");
	        m.getFunction( 1 ).setSaKey( "" );
	        m.getFunction( 2 ).setSaKey( "" );
	        m.getFunction(0).setData(GetInfo("PARCEL")); // parcel
	        m.getFunction(1).setData(GetInfo("MAP")); // map
	        m.getFunction(2).setData(GetInfo("GROUP")); // group
	        l.add(m); 
		}
		
        // address search
		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	        m.getFunction( 1 ).setSaKey( SearchAttributes.P_STREET_FULL_NAME_EX );
//			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}
        
		// name search
		if( hasOwner() ){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        m.clearSaKeys();
	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
//	        m.setIteratorType(ModuleStatesIterator.TYPE_REGISTER_NAME);
	        m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);

			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
	        
	        l.add(m);
		}
        
		serverInfo.setModulesForAutoSearch(l);		
	}

	protected Map<String,String> parseIntermPage(String rsResponce) {
        
		int startIndex = rsResponce.indexOf("<TABLE class=\"StdTable\"");
        int endIndex = rsResponce.indexOf("<table class=\"DGStd\"", startIndex);
        
        String nextPrevSection = rsResponce;
        if ( startIndex > -1 && endIndex > -1 )
        	nextPrevSection = rsResponce.substring( startIndex, endIndex );
        
        int iTmp=rsResponce.indexOf("action=\"")+8;
        iTmp=rsResponce.indexOf('?', iTmp)+1;
        String formAction=rsResponce.substring(iTmp, rsResponce.indexOf('"', iTmp)).replaceAll("&amp;", "&");
        
        startIndex = rsResponce.indexOf("__VIEWSTATE\" value=\"") + 20;
        String viewState = rsResponce.substring(startIndex, rsResponce.indexOf("\" />", startIndex));
        try {
            viewState = URLEncoder.encode(viewState, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        startIndex = rsResponce.indexOf("__EVENTTARGET\" value=\"") + "__EVENTTARGET\" value=\"".length();
        String eventTarget = rsResponce.substring(startIndex, rsResponce.indexOf("\" />", startIndex));
        try {
        	eventTarget = URLEncoder.encode(eventTarget, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        startIndex = rsResponce.indexOf("__EVENTARGUMENT\" value=\"") + "__EVENTARGUMENT\" value=\"".length();
        String eventArgument = rsResponce.substring(startIndex, rsResponce.indexOf("\" />", startIndex));
        try {
        	eventArgument = URLEncoder.encode(eventArgument, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        startIndex = rsResponce.indexOf("__EVENTVALIDATION\" value=\"") + "__EVENTVALIDATION\" value=\"".length();
        String eventValidation = rsResponce.substring(startIndex, rsResponce.indexOf("\" />", startIndex));
        try {
        	eventValidation = URLEncoder.encode(eventValidation, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        startIndex = rsResponce.indexOf("_ctl3:txtName\" type=\"text\" value=\"") + "_ctl3:txtName\" type=\"text\" value=\"".length();
        String txtName = rsResponce.substring(startIndex, rsResponce.indexOf("\" ", startIndex));
        try {
        	txtName = URLEncoder.encode(txtName, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        startIndex = rsResponce.indexOf("_ctl3:txtPropAddress\" type=\"text\" value=\"") + "_ctl3:txtPropAddress\" type=\"text\" value=\"".length();
        String txtPropAddress = rsResponce.substring(startIndex, rsResponce.indexOf("\" ", startIndex));
        try {
            txtPropAddress = URLEncoder.encode(txtPropAddress, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String prev = null, next = null;
        next = nextPrevSection.replaceAll("(?s).*<a([^>]*)>\\[Next Page\\]</a>.*", "$1");
        if ( next.equals(rsResponce) )
            next = null;
        else
        {
            if( next.indexOf( "disabled=\"disabled\"" ) >= 0 )
            {
                next = null;
            }
            else
            {
                next = next.replaceAll("(?s).*__doPostBack\\('(.*)','(.*)'\\).*", "$1");
            }
        }
        
        
        prev = nextPrevSection.replaceAll("(?s).*<a([^>]*)>\\[Previous Page\\]</a>.*", "$1");
        if ( prev.equals(rsResponce) || prev.indexOf("disabled") > -1 )
        	prev = null;
        else {
            if( prev.indexOf( "disabled=\"disabled\"" ) >= 0 )
            {
                
            }
            else
            {
                prev = prev.replaceAll("(?s).*__doPostBack\\('(.*)','(.*)'\\).*", "$1");
            }
        }
        
        Map<String,String> inputs = new HashMap<String,String>();
        
        inputs.put("rsResponce", rsResponce);
        inputs.put("formAction", formAction);
        inputs.put("viewState", viewState);
        inputs.put("eventTarget", eventTarget);
        inputs.put("eventArgument", eventArgument);
        inputs.put("eventValidation", eventValidation);
        inputs.put("txtName", txtName);
        inputs.put("dllPerPage", String.valueOf(200) );
        inputs.put("prev", prev);
        inputs.put("next", next);
        inputs.put( "txtPropAddress", txtPropAddress );
        
        return inputs;
    }

	@SuppressWarnings("deprecation")
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException {

		String sTmp = "", receiptNo = null;			
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;
		int istart=-1,iend=-1, iwhere = -1;
		
		rsResponce = rsResponce.replaceAll("(?s)<!--.*?-->", "");
		rsResponce = rsResponce.replaceAll("-->", "");
		
		ATSConn c;

		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
            case ID_SEARCH_BY_ADDRESS :
                
            	istart = rsResponce.indexOf( "\"ctl00_MainContent_dgrResults\"" );
            	istart = rsResponce.lastIndexOf( "<table" , istart);
            	
            	iend = rsResponce.indexOf( "</div" , istart);
           	
            	if( istart < 0 || iend < 0 ){
            		return;
            	}
            	
            	sTmp = CreatePartialLink( TSConnectionURL.idGET );
            	
            	rsResponce = rsResponce.substring( istart , iend);
            	//http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertyInfo.aspx?pmuid=139041
            	rsResponce = rsResponce.replaceAll( "(?is)<a href='PropertyInfo.aspx\\?pmuid=([^\']*)'><img[^>]*></a>" , "<a href='" +sTmp + "/TrusteeInquiry/AppFolder/PropertyInfo.aspx&pmuid=$1'>View</a>" );
            	
				parser.Parse(Response.getParsedResponse(), rsResponce, 
						Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), 
						TSServer.REQUEST_SAVE_TO_TSD);
				
				break;		
				
			case ID_DETAILS :
			case ID_SEARCH_BY_PARCEL :
				
				if (rsResponce.indexOf("Name 1") > 0 && rsResponce.indexOf("Name 2") > 0) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					return;
				}

				String rcptLink;
				
				istart = rsResponce.indexOf( "Property Details" );
				istart = rsResponce.lastIndexOf( "<table" , istart );
				iend = rsResponce.indexOf( "FooterLeft.gif" );
				iend = rsResponce.lastIndexOf( "</table" ,  iend );
				iend = rsResponce.lastIndexOf( "</table" ,  iend - 1 );				
				
				if (istart == -1 || iend == -1)
				{
					Response.setResult("");
					Response.getParsedResponse().setError(
                    		"<br>You must type the Control Map, Group, and Parcel exactly as they appear on the bill. Alternately, you may supply just the Map.");
					return;
				}
				rsResponce=rsResponce.substring(istart,iend + 8);
				
				//get receipt no
				istart = rsResponce.indexOf( "State Grid" );
				istart = rsResponce.indexOf( "<td", istart );
				istart = rsResponce.indexOf( "<span", istart );
				istart = rsResponce.indexOf( ">", istart );
				iend = rsResponce.indexOf( "</" , istart);
				if( istart >= 0 && iend >= 0 ){
					receiptNo = rsResponce.substring( istart + 1 , iend );
					receiptNo = receiptNo.replaceAll( "\\s" , "");
				}
				
				rsResponce = rsResponce.replaceAll( "(?is)<span[^>]*>" , "" );
				rsResponce = rsResponce.replaceAll( "(?is)</span>" , "" );				
				rsResponce = rsResponce.replaceAll( "(?is)<div[^>]*>" , "" );
				rsResponce = rsResponce.replaceAll( "(?is)</div>" , "" );
				
				rsResponce = rsResponce.replaceAll( "(?is)\\bPrinting\\s+Tips\\b" , "" );
				
				iwhere = 1;
				
				String taxTable = "";
				do {
					
				    istart = rsResponce.indexOf("<a href='", iwhere);
				    iend = rsResponce.indexOf("' class='Link7'>View", istart + "<a href='".length());
				    
				    if( istart != -1 && iend != -1 )
				    {
				        iwhere = iend;
				        istart += "<a href='".length();
				        rcptLink = rsResponce.substring(istart, iend);
				        
				        c = makeconnection("http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/" + rcptLink, "", ATSConnConstants.GET, null, null);
				        
				        rcptLink = c.getResult().toString();
				        
				        istart = rcptLink.indexOf( "/Images/TabEnd.gif" );
				        istart = rcptLink.indexOf( "<tr>" , istart );			        
				        
				        iend = rcptLink.indexOf( "/Images/FooterLeft.gif" );
				        iend = rcptLink.lastIndexOf("</table", iend );
				        
				        if( istart >= 0 && iend >= 0 ){
				        
					        rcptLink = "<table>" + rcptLink.substring( istart , iend + 8 );
					        rcptLink = rcptLink.replaceAll( "(?is)<span[^>]*>" , "" );
					        rcptLink = rcptLink.replaceAll( "(?is)</span>" , "" );
					        rcptLink = rcptLink.replaceAll( "(?is)<div[^>]*>" , "" );
					        rcptLink = rcptLink.replaceAll( "(?is)</div>" , "" );
	
					        String appended = rcptLink;
					        
					        appended = appended.replaceAll( "(?is)<a [^>]*>([^<]*)</a>" , "$1" );
					        
							taxTable += appended;
				        }
						
				    } else
				    	break;
				    
				} while (true);
				
				rsResponce += taxTable;
				
				rsResponce=rsResponce.replaceAll("(?is)<a [^>]*>([^<]*)</a>", "$1");
				rsResponce=rsResponce.replaceAll("<span[^>]*>", "");
				rsResponce=rsResponce.replaceAll("</span>", "");
				rsResponce=rsResponce.replaceAll("</font></FONT>", "</font>");
				rsResponce=rsResponce.replaceAll("&nbsp;", "");
				rsResponce=rsResponce.replaceAll("<td [^>]*>", "<td>");
				rsResponce = rsResponce.replaceAll( "(?is)<div[^>]>" , "");
				rsResponce = rsResponce.replaceAll( "(?is)</div>" , "");				
                //rsResponce = rsResponce.replaceAll( "(?s)\\s</td>\\s+</tr>\\s+<tr>\\s+<td></td>\\s+</tr>\\s+</table>", "" );

				
                //rsResponce = rsResponce.replaceAll( "(?s)</table>\\s+</td>\\s+</tr>\\s+<tr height=\"1\">\\s+<td>\\s+<table", "</table><table" );
                
                receiptNo = receiptNo.replaceAll("&nbsp;", " ");
                
				if ((!downloadingForSave)) {
					
				    String qry=Response.getQuerry();
					qry="dummy="+receiptNo+"&"+qry;
					String originalLink = sAction+"&"+qry;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					if (FileAlreadyExist(receiptNo + ".html") ) {
						rsResponce += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						rsResponce =  addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
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

				if ( Response.getQuerry().contains( "pmuid=" ) )
                {
					ParseResponse(sAction, Response, ID_DETAILS);
                }
				else
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				
			    break;
			    
			case ID_SAVE_TO_TSD :
				
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				
				break;
				
			default :
				break;
		}
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
         
	    int istart = htmlString.indexOf("<tr");
		int iend = htmlString.indexOf("</tr", istart) + "</tr>".length();
		
		String rowEndSeparator = "</table id=\"endIntermediaries\">";
		htmlString = (htmlString.substring(0, istart) + htmlString.substring(iend)).replaceFirst("(?is)</table[^>]*>\\s*$", rowEndSeparator);
		p.splitResultRows(
			pr,
			htmlString,
			pageId,
			"<tr class=\"",
			rowEndSeparator,
			linkStart,
			action);
    }
}