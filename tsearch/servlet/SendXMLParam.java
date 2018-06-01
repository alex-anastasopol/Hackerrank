package ro.cst.tsearch.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.parentsitedescribe.DSMXMLReader;
import ro.cst.tsearch.parentsitedescribe.DefaultServerInfoMap;
import ro.cst.tsearch.parentsitedescribe.FunctionMap;
import ro.cst.tsearch.parentsitedescribe.HtmlControlMap;
import ro.cst.tsearch.parentsitedescribe.ListParam;
import ro.cst.tsearch.parentsitedescribe.ModuleXMLMap;
import ro.cst.tsearch.parentsitedescribe.PageZoneMap;
import ro.cst.tsearch.parentsitedescribe.Param;
import ro.cst.tsearch.parentsitedescribe.XMLDSMWriter;
import ro.cst.tsearch.servers.bean.SearchManager;
import ro.cst.tsearch.servlet.download.DownloadFile;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class SendXMLParam extends HttpServlet{

	private static final long serialVersionUID = 1L;
	
	public void doPost(HttpServletRequest request,
			  HttpServletResponse response)
	throws IOException, ServletException
	{
	    doRequest(request, response);
	}
	
	
	public void doRequest(HttpServletRequest request,
						  HttpServletResponse response)
			throws IOException, ServletException
	  {
		String data="";
		String paramData="";
		HtmlControlMap htmlc=null;
		FunctionMap functionMap;
		PageZoneMap zoneMap;
		String temp="";
		Param p1=null;
		int k;
		ModuleXMLMap module =null;
		boolean test;
		DefaultServerInfoMap serverMap = new DefaultServerInfoMap();	
		ListParam paramList=new ListParam();

			MultipartParameterParser mpp = null;
			
			  if ( request.getContentType() != null &&  request.getContentType().indexOf("multipart/form-data") > -1) {
		  	mpp = new MultipartParameterParser(request);
		  	 File file = (File) mpp.getFileParameters("tsd_file").elementAt(0);
			XMLDSMWriter writer = new XMLDSMWriter(file.getName());
			writer.writeFileUpload(file.getCanonicalPath().substring(0,file.getCanonicalPath().indexOf(file.getName())),file.getName());
			writer=null;
		
			response.sendRedirect( AppLinks.getParentSiteNoSaveHref(  mpp.getMultipartLongParameter(RequestParams.SEARCH_ID) ));
			return;
			  }
			mpp=null;
		     //if(request.getParameter("typeButton").compareToIgnoreCase("SubmitUpload")==0){
		
		//	}

		
		
		
		long searchId = -2;
		try{
			searchId = Long.parseLong( request.getParameter( "searchId" ) );
		} catch( Exception e ){
			e.printStackTrace();
		}
		
	      if("".equalsIgnoreCase(request.getParameter("genericSite"))){
				if (request.getParameter("noModule")==null 
						|| "".equals(request.getParameter("noModule"))
						|| "-1".equals(request.getParameter("noModule"))) {
					
					response.sendRedirect( AppLinks.getParentSiteNoSaveHref( searchId ) );
					return;
				}
	      }
	    
	      
	  	if(request.getParameter("typeButton").compareToIgnoreCase("Download")==0){
	  		String fileName=request.getParameter("nameF");
	  		System.err.println(">>>>>>>>>>>> Vreau Downlad  "+fileName);
	   		    response.setContentType("Content-Type=application/x-xpinstall");
	  		    response.setHeader(
	  		            "Content-Disposition",
	  		            " inline; filename=\""
	  		                + fileName
	  		                + "\"");
	  	
	  		   System.err.println(">>>>>>>>>>>>>>>>>>> DownLoad file      "+fileName);
	  		 DSMXMLReader reader = new DSMXMLReader(request.getParameter("nameFile"));
	  		 XMLDSMWriter writer = new XMLDSMWriter(request.getParameter("nameFile"));
	  		String fileContents=writer.writeString(reader.readXML());
	  		
	  		    ByteArrayInputStream bais = new ByteArrayInputStream( fileContents.getBytes() );
	  		    response.setContentLength( fileContents.getBytes().length );
	  			IOUtil.copy(bais, response.getOutputStream());
	  			  		
	  		
	  		 		return;
	  	}
	  	
	  	
		if(request.getParameter("typeButton").compareToIgnoreCase("Cancel")==0){
			response.sendRedirect( AppLinks.getParentSiteNoSaveHref( searchId ) );
			return;
		}
				
		
		for (Enumeration e = request.getParameterNames() ;  e.hasMoreElements() ;) {
        	temp=e.nextElement().toString();
        	if((temp!=null)&&(!"".equals(temp))){
        		//System.err.println(">>>>>>>>>>>>>>>>>param "+temp+" value   "+request.getParameter(temp));
	        	 paramList.add(temp,request.getParameter(temp));
	           	}
        }
		
        serverMap.setServerLink( request.getParameter("serverLink") );
        
        serverMap.setInfoRemoteAdress( request.getParameter("remote_adr"));
        serverMap.setInfoRemoteIp( request.getParameter("remote_ip"));
        serverMap.setInfoServerAdress( request.getParameter("server_adr"));
        serverMap.setInfoServerIp( request.getParameter("server_ip"));
        
		serverMap.setServerIp(request.getParameter("serverIp")) ;
		serverMap.setServerAdress(request.getParameter("serverAdres"));
		
		paramData=request.getParameter("genericSite");
		if((paramData!=null)&&(!"".equalsIgnoreCase(paramData))){
				serverMap.setGenericSite(paramData);
					System.err.println("avem generin");
			}
		serverMap.setModuleNumber(1+Integer.parseInt(request.getParameter("noModule"))) ;
		for (int i=0;i<serverMap.getModuleNumber();i++){
			functionMap=new FunctionMap();
			zoneMap= new  PageZoneMap();
			module =new ModuleXMLMap();
			
			data="FunctionsCount"+Integer.toString(i);
			paramData=request.getParameter(data);
			//modificare lista
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setFunctionsCount(Integer.parseInt(paramData));
				}
			
			data="moduleIndex"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setModuleIndex(Integer.parseInt(paramData ));
			}

			data = "moduleOrder"+Integer.toString(i); 
			paramData = request.getParameter(data);
			if (StringUtils.isNotEmpty(paramData)){
				functionMap.setModuleOrder(Integer.parseInt(paramData));
			}

			data = "searchType"+Integer.toString(i); 
			paramData = request.getParameter(data);
			if (StringUtils.isNotEmpty(paramData)){
				functionMap.setSearchType(paramData);
			}
			
			data="destinationPage"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setDestinationPage(paramData );
			}

			data="destinationMethod"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setDestinationMethod(Integer.parseInt(paramData));
			}

			data="setName"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setSetName(paramData );
				
			}
			data="setParcelID"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setSetParcelId(Integer.parseInt(paramData ));
			}
			data="setKey"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setSetKey(paramData );
			}
			
			data="setVisible"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				//System.err.println(">>>>>>>>>>>>>>>> DATA>>>>>>>>>>>>>>>>>>>>>> "+data);
				if(paramData.compareToIgnoreCase("true")==0){
					functionMap.setVisible(true);
				}
				else{
					functionMap.setVisible(false);
				}
				
			}
			data="setVisibleFor"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				
				if (paramData.compareToIgnoreCase("comadmin") == 0){
					functionMap.setVisibleFor(SearchManager.COMMADMIN);
				} else if(paramData.compareToIgnoreCase("tscadmin") == 0){
					functionMap.setVisibleFor(SearchManager.TSCADMIN);
				} else if(paramData.compareToIgnoreCase("tsadmin") == 0){
					functionMap.setVisibleFor(SearchManager.TSADMIN);
				} else if(paramData.compareToIgnoreCase("alladmin") == 0){
					functionMap.setVisibleFor(SearchManager.ALL_ADMIN);
				} else if(paramData.compareToIgnoreCase("all") == 0){
					functionMap.setVisibleFor(SearchManager.ALL);
				}			
			}
			data="setMoule"+Integer.toString(i);
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				functionMap.setMoule(Integer.parseInt(paramData ));
			}

			
			data="P_" +Integer.toString(i)+"_"+Integer.toString(0)+"_TYPE";
			paramData=request.getParameter(data);
			test=paramData!=null;

			k=0;
			while(test){
				p1=new Param();
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_NAME";
				//System.err.println(">>>>>>>>>>>>>>>>Parameter>>>>>>>>>>>>>>>>>>>>>> ");
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setName(paramData);
				}
				
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_TYPE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setType(paramData);
				}
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_VALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setValue(paramData);
				}
				
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_ITERATOR";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setIteratorType(Integer.parseInt(paramData));
				}
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_KEY";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setSaKey(paramData);
				}
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_VALID";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setValidationType(paramData);
				}

				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HIDDENNAME";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setHiddenName(paramData);
				}
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HIDDENVALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setHiddenValue(paramData);
				}
				functionMap.add(p1);
				
				++k;
				
				data="P_" +Integer.toString(i)+"_"+Integer.toString(k)+"_TYPE";
				paramData=request.getParameter(data);
				test=paramData!=null;
				
			}

			data="EP_" +Integer.toString(i)+"_"+Integer.toString(0)+"_TYPE";
			paramData=request.getParameter(data);
			test=paramData!=null;

			k=0;
			while(test){
				p1=new Param();
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_NAME";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setName(paramData);
				}
				
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_TYPE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setType(paramData);
				}
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_VALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setValue(paramData);
				}
				
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_ITERATOR";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setIteratorType(Integer.parseInt(paramData));
				}

				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_KEY";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setSaKey(paramData);
				}
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HIDDENNAME";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setHiddenName(paramData);
				}
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HIDDENVALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					p1.setHiddenValue(paramData);
				}
				functionMap.addElse(p1);
				
				++k;
				
				data="EP_" +Integer.toString(i)+"_"+Integer.toString(k)+"_NAME";
				paramData=request.getParameter(data);
				test=paramData!=null;
				
				
			}

//numele trebuie sa dispara si devine pageZone+i
			//data="PZ_" +Integer.toString(i)+"_NAME";
			//if(paramList.isInList(data)){
				zoneMap.setName("PageZone"+Integer.toString(i));
		//	}
			data="PZ_" +Integer.toString(i)+"_LABEL";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setLabel(paramData);
			}

			data="PZ_" +Integer.toString(i)+"_SEPARATOR";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setSeparator(paramData);
			}

			data="PZ_" +Integer.toString(i)+"_ORIENTATION";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setOrientation(Integer.parseInt(paramData));
			}
			data="PZ_" +Integer.toString(i)+"_EXTRABUTTON";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setExtraButton(paramData);
			}
			
			data="PZ_" +Integer.toString(i)+"_CUSTOM_FORM_VALIDATION";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setCustomFormValidation(paramData);
			}
			
			data="PZ_" +Integer.toString(i)+"_FORMTARGET";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				zoneMap.setFormTarget(paramData);
			}

			data="PZ_" +Integer.toString(i)+"_ALTERNATIVECSS";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				if(paramData.compareToIgnoreCase("null")==0){
					zoneMap.setAlternativeCSS(null);
				}
				else{
					zoneMap.setAlternativeCSS(paramData);
				}
			}
			data="PZ_" +Integer.toString(i)+"_WIDTH";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				if(paramData.compareToIgnoreCase("null")==0){
					zoneMap.setWidth(null);
				}
				else{
					zoneMap.setWidth(new Integer(paramData));
				}
			}
			data="PZ_" +Integer.toString(i)+"_HEIGHT";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				if(paramData.compareToIgnoreCase("null")==0){
					zoneMap.setHeight(null);
				}
				else{
					zoneMap.setHeight(new Integer(Integer.parseInt(paramData)));
				}
			}
			
			data="PZ_" +Integer.toString(i)+"_ISROOT";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				if(paramData.compareToIgnoreCase("true")==0){
					zoneMap.setRoot(true);
				}
				else{
					zoneMap.setRoot(false);
				}
			}
			data="PZ_" +Integer.toString(i)+"_BORDER";
			paramData=request.getParameter(data);
			if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
				if(paramData.compareToIgnoreCase("true")==0){
					zoneMap.setBorder(true);
				}
				else{
					zoneMap.setBorder(false);
				}
			}
			
			//de completat html controler
			data="H_" +Integer.toString(i)+"_"+Integer.toString(0)+"_CONTROLTYPE";
			paramData=request.getParameter(data);
			test=paramData!=null;
			k=0;
			while(test){
				htmlc=new HtmlControlMap();
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_CONTROLTYPE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setControlType(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_NAME";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setName(paramData);
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_LABEL";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setLabel(paramData);
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_CELSTART";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setColStart(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_CELEND";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setColEnd(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_ROWSTART";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setRowStart(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_ROWEND";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setRowEnd(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_SIZE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setSize(Integer.parseInt(paramData));
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_TSSFUNCTION";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setTssFunction(Integer.parseInt(paramData));
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_DEFAULTVALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("null")!=0){
							htmlc.setDefaultValue(paramData);
						}
					else{
						htmlc.setDefaultValue(null);
					}
					
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_SELECTVALUE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setComboValue(paramData);
				}

				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_FIELDNOTE";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setFieldNote(paramData);
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_VALUEREQUIRED";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setValueRequired(true);
					}
					else{
						htmlc.setValueRequired(false);
					}
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_REQUIREDEXCL";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setRequiredExcl(true);
					}
					else{
						htmlc.setRequiredExcl(false);
					}
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_REQUIREDCRITICAL";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setRequiredCritical(true);
					}
					else{
						htmlc.setRequiredCritical(false);
					}
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HORIZONTALRADIOBUTTON";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setHorizontalRadioButton(true);
					}
					else{
						htmlc.setHorizontalRadioButton(false);
					}
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_JUSTIFYFIELD";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setJustifyField(true);
					}
					else{
						htmlc.setJustifyField(false);
					}
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_RADIODEFAULTCHECKED";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setRadioDefaultChecked(paramData);
				}
	
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_JSFUNCTION";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setJSFunction(paramData);
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_EXTRACLASS";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setExtraClass(paramData);
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_DEFAULT_ON_REPLICATE";
				paramData=request.getParameter(data);
				if(StringUtils.isNotEmpty(paramData) && "on".equalsIgnoreCase(paramData)){
					htmlc.setDefaultOnReplicate(true);
				}
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HIDDENPARAM";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					if(paramData.compareToIgnoreCase("true")==0){
						htmlc.setHiddenparam(true);
					}
					else{
						htmlc.setHiddenparam(false);
						}
				}
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_DEFAULTSELECTION";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setRadioDefaultChecked(paramData);
				}

				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_HTMLS";
				paramData=request.getParameter(data);
				if((paramData!=null) &&(!"".equalsIgnoreCase(paramData))){
					htmlc.setHtmlString(paramData);
				}
				
				++k;
				
				data="H_" +Integer.toString(i)+"_"+Integer.toString(k)+"_CONTROLTYPE";
				paramData=request.getParameter(data);
				test=paramData!=null;
				zoneMap.add(htmlc);
				
			}
			functionMap.setFunctionsCount(k);
			module.addFunction(functionMap);
			module.addZone(zoneMap);
			serverMap.setOneModule(module);
		}
	  System.err.print("AM terminat nr module"+serverMap.getModuleNumber());
	  XMLDSMWriter writer = new XMLDSMWriter(paramList.getParameter("nameFile"));
	  writer.writeFile(serverMap.toEscape());
	
	  response.sendRedirect( AppLinks.getParentSiteNoSaveHref( searchId ) );
	
}
}
