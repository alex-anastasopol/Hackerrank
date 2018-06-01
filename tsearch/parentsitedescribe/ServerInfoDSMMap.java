package ro.cst.tsearch.parentsitedescribe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.utils.InstanceManager;

public class ServerInfoDSMMap {
	private boolean elseParam=true;
	public void setElseParam(boolean elseParam){
		
	}
	public ServerInfoDSMMap() {
		super();
		// TODO Auto-generated constructor stub
	}
	private String sesionID="";
	
	public static final int MAX_NR_OF_PAGEZONES =70;
public TSServerInfo getServerInfo(String nameFile, long searchId){
 
	SimpleDateFormat sdf= new SimpleDateFormat("MMddyyyy");
	DSMXMLReader  convert =new DSMXMLReader(nameFile);
	DefaultServerInfoMap serverMap=convert.readXML();
	DefaultServerInfoMap serverMap1=null;
	DefaultServerInfoMap serverMap2=null;
	
	
	if((serverMap.getGenericSite()!=null)&&(!"".equalsIgnoreCase(serverMap.getGenericSite()))){
		
		System.err.println("AVEM generic site   "+serverMap.getGenericSite());
		String tmpgen=serverMap.getGenericSite();
		convert =new DSMXMLReader(tmpgen);
		DSMXMLReader convert1=new DSMXMLReader(nameFile);
		
		serverMap1= new DefaultServerInfoMap();
		serverMap2= new DefaultServerInfoMap();
		serverMap= new DefaultServerInfoMap();
		serverMap2.replace(convert1.readXML());
		serverMap1.replace(convert.readXML());
		serverMap.replace(serverMap2.mergeModulue(serverMap1));
		convert1=null;
		convert=null;
		serverMap2=null;
		serverMap1=null;
	}
	
	
	TSServerInfo msiServerInfoDefault = null;
    TSServerInfoModule simTmp = null;
    PageZone page[]= new PageZone[MAX_NR_OF_PAGEZONES];
    	if (msiServerInfoDefault == null){
        
    		msiServerInfoDefault = new TSServerInfo(serverMap.getModules().size());
        msiServerInfoDefault.setServerAddress(serverMap.getServerAdress());
           msiServerInfoDefault.setServerLink( serverMap.getServerLink() );
            msiServerInfoDefault.setServerIP( serverMap.getServerIp());
            
            for(int i=0;i<serverMap.getModules().size();i++){
            	ModuleXMLMap module = new ModuleXMLMap();
            	FunctionMap functionMap = new FunctionMap();
            	PageZoneMap pageZone =new PageZoneMap();
            	module=serverMap.getOneModule(i);
            	functionMap=module.getFunction();
            	pageZone=module.getZoneMap();
            	simTmp = msiServerInfoDefault.ActivateModule(functionMap.getModuleIndex(),functionMap.getFunctionsCount());
            	
            	if("".compareTo(functionMap.getDestinationPage())!=0){
            		simTmp.setDestinationPage(functionMap.getDestinationPage());
            	}
            	
            	int moduleOrder = functionMap.getModuleOrder();
           		simTmp.setModuleOrder(moduleOrder);
            	
           		String searchType = functionMap.getSearchType();
           		if(searchType != null){
           			simTmp.setSearchType(searchType);
           		}
           		
            	if(functionMap.getDestinationMethod()!=0){
            		simTmp.setRequestMethod(functionMap.getDestinationMethod());//2
            	}
            	
            	if(functionMap.getSetParcelId()!=0){
            		simTmp.setParserID(functionMap.getSetParcelId());
            	}
                if(functionMap.getMoule()!=-1){
                	simTmp.setMouleType(functionMap.getMoule());
                }
                if(!functionMap.getVisible()){
                	simTmp.setVisible(functionMap.getVisible());
                }
                simTmp.setVisibleFor(functionMap.getVisibleFor());

 
                LinkedList<Param> list =null;
                LinkedList<Param> listElse =null;
                list=functionMap.getFunctionDefinedMap();
          
                if(!elseParam){
                	listElse=functionMap.getFunctionDefinedElseMap();
                	Param p1=null;
                	Param p2=null;
                	for(int aux1=0;aux1<listElse.size();aux1++){
                		p1=listElse.get(aux1);
                		for(int aux2=0;aux2<list.size();aux2++){
                			p2=list.get(aux2);
                			if(p1.getName().compareToIgnoreCase(p2.getName())==0){
                				list.get(aux2).setHiddenName(p1.getHiddenName());
                				list.get(aux2).setHiddenValue(p1.getHiddenValue());
                				list.get(aux2).setIteratorType(p1.getIteratorType());
                				list.get(aux2).setName(p1.getName());
                				list.get(aux2).setParcelID(p1.getParcelID());
                				list.get(aux2).setSaKey(p1.getSaKey());
                				list.get(aux2).setType(p1.getType());
                				list.get(aux2).setValue(p1.getValue());
                				
                			}
                			
                		}
                	}
                }
                Param para =null;
                for(int j=0;j<list.size();j++){
                	para=list.get(j);
                	if(para.getName()!=null){
                		simTmp.getFunction(j).setName(para.getName()); //it will be displayed in jsp
                	}
                	
                	if(para.getValue()!=null){
                		simTmp.getFunction(j).setParamName(para.getValue());
                	}
                	
                	if(para.getIteratorType()!=-1){
                		simTmp.getFunction(j).setIteratorType(para.getIteratorType());
                	}
                	if("".compareTo(para.getSaKey())!=0){
                		simTmp.getFunction(j).setSaKey(para.getSaKey());
                	}
                	
                   	if("".compareTo(para.getValidationType())!=0){
                		simTmp.getFunction(j).setAreaType(para.getValidationType());
                	}
 
                	if(!(("".compareTo(para.getHiddenName())==0)||("".compareTo(para.getHiddenValue())==0))){
                		simTmp.getFunction(j).setHiddenParam(para.getHiddenName(),para.getHiddenValue());
                	}
 
             	
                	}
            	if("".compareTo(functionMap.getSetKey())!=0){
                		simTmp.setSaObjKey(functionMap.getSetKey());
            	}
               
               //simTmp.getFunction(htmlc.getTssFunction()).
                		
                try
                {// puneu un if pt apelarea diferitilor controleri
               //if( page[i] functionMap.getVisible()) 	
                	if(functionMap.getVisible() || (!functionMap.getVisible() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(functionMap.getVisibleFor()))){
                	page[i] = new PageZone(pageZone.getName(),pageZone.getLabel(),pageZone.getOrientation(), 
                    		pageZone.getAlternativeCSS(), pageZone.getWidth(),pageZone.getHeight(),
                    		pageZone.getTypeOfMeasure() ,pageZone.isRoot(), pageZone.getCustomFormValidation());
                	page[i].setExtraButton(pageZone.getExtraButton());
                	page[i].setFormTarget(pageZone.getFormTarget());
                    page[i].setBorder(pageZone.getBorder());
                    simTmp.setName(pageZone.getName()); //it will be displayed in jsp
                    LinkedList<HtmlControlMap> list2=new LinkedList<HtmlControlMap>();
                    list2=pageZone.getHtmlControlMap();
                    HTMLControl htmlCo[] =new HTMLControl[list2.size()];
                    for (int k=0;k<list2.size();k++){
                    	htmlCo[k]=null;
                    	HtmlControlMap htmlc=new HtmlControlMap();
                    	htmlc=list2.get(k);
                    	if(simTmp.getFunction(htmlc.getTssFunction()).getSaKey().compareToIgnoreCase(SearchAttributes.FROMDATE)==0){
                        	sdf.applyPattern("MMM d, yyyy");
                        	Date end = Util.dateParser3(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getAtribute("FROMDATE"));
                        	if((htmlc.getFieldNote().compareToIgnoreCase("MM")==0)||(htmlc.getFieldNote().compareToIgnoreCase("yyyy")==0)||(htmlc.getFieldNote().compareToIgnoreCase("dd")==0)){
                        		if(htmlc.getFieldNote().compareToIgnoreCase("MM")==0){
                        			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
                	   				cal.setTime( end );
                	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.MONTH ));
                	   					  		
                        		}
                       		if(htmlc.getFieldNote().compareToIgnoreCase("yyyy")==0){
                    			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
            	   				cal.setTime( end );
            	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.YEAR ));
                        			
                        		}
                       		if(htmlc.getFieldNote().compareToIgnoreCase("dd")==0){
                    			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
            	   				cal.setTime( end );
            	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.DAY_OF_MONTH ));
                        			
                        		}

                        	}
                        	else{
                        		sdf.applyPattern(htmlc.getFieldNote());
                        		String dend=sdf.format(end);
                        		htmlc.setDefaultValue(dend);
                        		}
	   					   	}
                    
                    	

                    	if(simTmp.getFunction(htmlc.getTssFunction()).getSaKey().compareToIgnoreCase(SearchAttributes.TODATE)==0){
                                 		sdf.applyPattern("MMM d, yyyy");	
                                 		Date start = Util.dateParser3(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getAtribute("TODATE"));
                                       	if((htmlc.getFieldNote().compareToIgnoreCase("MM")==0)||(htmlc.getFieldNote().compareToIgnoreCase("yyyy")==0)||(htmlc.getFieldNote().compareToIgnoreCase("dd")==0)){
	                                    		if(htmlc.getFieldNote().compareToIgnoreCase("MM")==0){
	                                    			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
	                            	   				cal.setTime( start );
	                            	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.MONTH ));
	                            	   					  		
	                                    		}
	                                   		if(htmlc.getFieldNote().compareToIgnoreCase("yyyy")==0){
	                                			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
	                        	   				cal.setTime( start );
	                        	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.YEAR ));
	                                    			
	                                    		}
	                                   		if(htmlc.getFieldNote().compareToIgnoreCase("dd")==0){
	                                			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
	                        	   				cal.setTime( start );
	                        	   			 	htmlc.setDefaultValue("" + cal.get( GregorianCalendar.DAY_OF_MONTH ));
	                                   			}
                                    	}

                                   		else{
                                   			sdf.applyPattern(htmlc.getFieldNote());
                	  						String dstart=sdf.format(start);
                	   						htmlc.setDefaultValue(dstart);
                                   		}
             	   					  	}
                    	if(htmlc.getFieldNote()!=null){
                    		if(htmlc.getFieldNote().compareToIgnoreCase("getSesionID")==0){
                    			IdList.addObject(nameFile,htmlc);
                    			System.err.println("valoare  dupa modificare  "+htmlc.getDefaultValue().toString());
                    			}
                    	}
                    	if(htmlc.getControlType()==1){
                    		htmlCo[k] = new HTMLControl(htmlc.getControlType(),htmlc.getName(), htmlc.getLabel(), htmlc.getColStart(), htmlc.getColEnd(),
                        			htmlc.getRowStart(),htmlc.getRowEnd(),htmlc.getSize(),htmlc.getRadio(), simTmp.getFunction( htmlc.getTssFunction()), searchId );
                    		
                    	}
                    	if((htmlc.getControlType()==2)||(htmlc.getControlType()==4)||(htmlc.getControlType()==5)){
                    		htmlCo[k] = new HTMLControl(htmlc.getControlType(),htmlc.getName(), htmlc.getLabel(), htmlc.getColStart(), htmlc.getColEnd(),
                    			htmlc.getRowStart(),htmlc.getRowEnd(),htmlc.getSize(),htmlc.getDefaultValue(), simTmp.getFunction( htmlc.getTssFunction()), searchId );
                    	}
                    	
                    	if(htmlc.getControlType()==0){
                    		htmlCo[k] = new HTMLControl(htmlc.getControlType(),htmlc.getName(), htmlc.getLabel(), htmlc.getColStart(), htmlc.getColEnd(),
                    			htmlc.getRowStart(),htmlc.getRowEnd(),htmlc.getSize(),htmlc.getDefaultValue(),simTmp.getFunction(htmlc.getTssFunction()).getAreaType(), simTmp.getFunction( htmlc.getTssFunction()), searchId );
                    	}
 	
                   
                    	if((htmlc.getControlType()==6)||(htmlc.getControlType()==3||(htmlc.getControlType()==7))){
                           		htmlCo[k] = new HTMLControl( htmlc.getControlType(),htmlc.getComboValue(), htmlc.getName(), htmlc.getLabel(), htmlc.getColStart(), htmlc.getColEnd(),
                            			htmlc.getRowStart(),htmlc.getRowEnd(),htmlc.getSize(),htmlc.getDefaultValue(), simTmp.getFunction( htmlc.getTssFunction()), searchId );
                                		
                    	}
                    	
                    	htmlCo[k].setHiddenParam(htmlc.getHiddenparam());
                    	
                    	simTmp.getFunction(htmlc.getTssFunction()).setName(htmlc.getLabel());
                    	simTmp.getFunction(htmlc.getTssFunction()).setParamName(htmlc.getName());
                    	simTmp.getFunction(htmlc.getTssFunction()).setHiden(htmlc.getHiddenparam());
                    	simTmp.getFunction(htmlc.getTssFunction()).setControlType(htmlc.getControlType());
                    	simTmp.getFunction(htmlc.getTssFunction()).setComboValue(htmlc.getComboValue());
              
                    	if(htmlc.getHiddenparam()){
                    		if(htmlc.getDefaultValue()!=null){
                    			simTmp.getFunction(htmlc.getTssFunction()).setDefaultValue(htmlc.getDefaultValue().toString());	
                    		} 
                           
                    	} else{
                    		if(htmlc.getDefaultValue()!=null){
                    		simTmp.getFunction(htmlc.getTssFunction()).setDefaultValue(htmlc.getDefaultValue().toString());
                    		}
                      
                    	}
                   	if("".compareTo(htmlc.getFieldNote())!=0){
                   		//if((simTmp.getFunction(htmlc.getTssFunction()).getSaKey().compareToIgnoreCase(SearchAttributes.FROMDATE)==0)||(simTmp.getFunction(htmlc.getTssFunction()).getSaKey().compareToIgnoreCase(SearchAttributes.TODATE)==0)){
                   		//	htmlCo[k].setFieldNote("");
                   		//}
                   		//else{
                   			htmlCo[k].setFieldNote(htmlc.getFieldNote());
                   		//}
                   		
                   		
                    	}
                    	
                   		if(!StringUtils.isEmpty(htmlc.getExtraClass())){
                   			htmlCo[k].setExtraClass(htmlc.getExtraClass());
                   		}
                   		
                   		if(htmlc.isDefaultOnReplicate()){
                   			htmlCo[k].setDefaultOnReplicate(htmlc.isDefaultOnReplicate());
                   		}
                   	
                    	if(htmlc.getValueRequired()){
                    		htmlCo[k].setValueRequired(htmlc.getValueRequired());
                    	}
                    	
                    	if(htmlc.getRequiredExcl()){
                    		htmlCo[k].setRequiredExcl(htmlc.getRequiredExcl());
                    	}
                    	
                    	if(htmlc.getRequiredCritical()){
                    		htmlCo[k].setRequiredCritical(htmlc.getRequiredCritical());
                    	}
                    	
                    	if(htmlc.getHorizontalRadioButton()){
                    		htmlCo[k].setHorizontalRadioButton(htmlc.getHorizontalRadioButton());
                    	}
                    	
                    	if(htmlc.getJustifyField()){
                    		htmlCo[k].setJustifyField(htmlc.getJustifyField());
                    	}
                     	
                    	
                    	if("".compareTo(htmlc.getRadioDefaultChecked())!=0){
                    		htmlCo[k].setDefaultRadio(htmlc.getRadioDefaultChecked());
                    	}
                    	
                    	
                    	if("".compareTo(htmlc.getJSFunction())!=0){
                    		htmlCo[k].setJSFunction(htmlc.getJSFunction());
                    	}
   
          
                     	
                    	if("".compareTo(htmlc.getHtmlString())!=0){
                            simTmp.getFunction(htmlc.getTssFunction()).setHtmlformat(StringEscapeUtils.unescapeHtml(htmlc.getHtmlString()));
                    	}
                  
        
    
                     	page[i].addHTMLObject( htmlCo[k] );
                    }
                    
                    if("".compareTo(pageZone.getSeparator())!=0){
                    	simTmp.setSeparator( pageZone.getSeparator() );
                   }
  
                    simTmp.setModuleParentSiteLayout( page[i]);
                    }
                	else{
                        LinkedList<HtmlControlMap> list2=new LinkedList<HtmlControlMap>();
                        list2=pageZone.getHtmlControlMap();
                        HTMLControl htmlCo[] =new HTMLControl[list2.size()];
                        for (int k=0;k<list2.size();k++){
                        	htmlCo[k]=null;
                        	HtmlControlMap htmlc=new HtmlControlMap();
                        	htmlc=list2.get(k);
                        	if(htmlc.getHiddenparam()){
                                simTmp.getFunction(htmlc.getTssFunction()).setHiden( htmlc.getHiddenparam() );
                                if(htmlc.getDefaultValue()!=null){
                        			simTmp.getFunction(htmlc.getTssFunction()).setDefaultValue(htmlc.getDefaultValue().toString());	
                        		}
                        	}
                        	else{
                        		//simTmp.getFunction(htmlc.getTssFunction()).setDefaultValue("");
                        		if(htmlc.getDefaultValue()!=null){
                            		simTmp.getFunction(htmlc.getTssFunction()).setDefaultValue(htmlc.getDefaultValue().toString());
                            	}
                        	}

                        }
                	}
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
               	}
          }

   serverMap=null;
   serverMap1=null;
   	return msiServerInfoDefault;
   }
public String getSesionID() {
	if(this.sesionID!=null){
		return sesionID;
		}
	else{
		return "";
	}
}
public void setSesionID(String sesionID) {
	if(sesionID!=null){
		if ("".compareTo(sesionID)!=0){
			this.sesionID = sesionID;
		}
	}
}


    	
  	}

