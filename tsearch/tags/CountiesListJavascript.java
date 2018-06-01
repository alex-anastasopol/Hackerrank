package ro.cst.tsearch.tags;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.GenericTag;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;


public class CountiesListJavascript extends GenericTag {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(CountiesListJavascript.class);
   
	private int[] reportCounty = {-1};	
	public void setReportCounty(String s) {
		reportCounty = Util.extractArrayFromString(s);
	}	
	public String getReportCounty() {
		return Util.getStringFromArray(reportCounty);
	}

	private int[] reportState = {-1};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	
	private int selectSize = 5;
	public void setSelectSize(String s){
		selectSize = Integer.parseInt(s);
	}

	public int getSelectSize(){
		return selectSize;
	}	
	
	private boolean selectMultiple = true;
	public void setSelectMultiple(String s){
		selectMultiple= (s.toUpperCase().equals("TRUE") || s.toUpperCase().equals("YES"))?true:false;
	}

	public boolean getSelectMultiple(){
		return selectMultiple;
	}	
	
	private int statesOffset = 1;
    public int getStatesOffset() {
		return statesOffset;
	}
	public void setStatesOffset(int statesOffset) {
		this.statesOffset = statesOffset;
	}
	
	private int allStatesOptionIndex = 0;
	public int getAllStatesOptionIndex() {
		return allStatesOptionIndex;
	}
	public void setAllStatesOptionIndex(int allStatesOptionIndex) {
		this.allStatesOptionIndex = allStatesOptionIndex;
	}
	
	private boolean noCountiesOption = false;
	public boolean hasNoCountiesOption() {
		return noCountiesOption;
	}
	public void setNoCountiesOption(boolean noCountiesOption) {
		this.noCountiesOption = noCountiesOption;
	}
	
	public int doStartTag() {
        try {
            //logger.debug("Debug: SelectTag#doStartTag start " + selectName );
            initialize();
    		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
    		UserAttributes ua = currentUser.getUserAttributes();
                        
    		loadAttribute(RequestParams.REPORTS_COUNTY);
    		loadAttribute(RequestParams.REPORTS_STATE);
    		
    		logger.info("countyId=" + Util.getStringFromArray(reportCounty) + " state=" + Util.getStringFromArray(reportState));

    		StringBuffer sb = new StringBuffer(3000);		
                     
            GenericState allStates[]=DBManager.getAllStates(); 
    		Collection<CountyWithState> countiesOfState;
            
    		sb.append("<SCRIPT LANGUAGE=JavaScript TYPE='text/javascript'> \n" +
					    " function stateWithCounties(sId,cList,isSel) {\n" + 
						" this.stateId = sId;\n " + 
						" this.countyList = cList; \n" + 
						" this.isSelected = isSel; \n" +
						" }\n" +
						"statesWithCounties = new Array(" +
						"");

     		 for(GenericState state: allStates) {
    			 if (ua.isAllowedState(BigDecimal.valueOf(state.getId()))){
    				 countiesOfState = DBManager.getAllCountiesForState((int)(state.getId()));
    				 sb.append(
     			    	"new stateWithCounties(" +
     			    	state.getId() +
     			    	",\""
     			     );
    				 for (CountyWithState county: countiesOfState) {
    					 if (ua.isAllowedCounty(county.getCountyId())){
    						 sb.append(
    								 "<option value=" +
    								 county.getCountyId() +
    								 (Util.isValueInArray(county.getCountyId(), reportCounty) ? " selected " : "") +
    								 ">"  +
    								 county.getStateAbrv() +
    								 ", " +
    								 county.getCountyName()+
    								 "</option>"
    						 );
    					 }
    				 }
    				 sb.append('\"');
    				 sb.append(
    						"," +
    						( (Util.isValueInArray((int)state.getId(), reportState) || Util.isValueInArray(-1, reportState) ) ? "true" : "false")
    						);
    				 sb.append("),");
    			 }
    		 }
     		sb.setCharAt(sb.length()-1, ')');
     		sb.append(";\n");
     		sb.append(
     			"function loadCountySelectOptions(selectOnlyFirstOption) { \n" +
	     		"	var x = \"\"; \n" +		
	     		"   x = \"<select name=\\\""+ RequestParams.REPORTS_COUNTY +"\\\" size=\\\""+ selectSize +"\\\" "+ (selectMultiple?"multiple":"") +" style=\\\"width: 120px\\\"  " +
	     				"onblur=\\\"javascript:reportCountySelectOnBlur();\\\"  onfocus=\\\"javascript:reportCountySelectOnFocus();\\\"  onchange=\\\"javascript:reportCountySelectOnChange();\\\"  >" +
	     				(noCountiesOption?"<option" + (Util.isValueInArray(-2, reportCounty)  ? " selected ":""  ) + " value='-2' >No Counties</option>":"") +
	     				"<option" + (Util.isValueInArray(-1, reportCounty)  ? " selected ":""  ) + " value='-1' >All Counties</option>\";" +
	     		" 	var noSelStates = 0; " +
	     		"	for(i = 0; i < statesWithCounties.length; i++){ \n" +
	     		"		if(statesWithCounties[i].isSelected == true) { \n" +
	     		"           noSelStates++;" +
	     		"			x = x + statesWithCounties[i].countyList; \n" +
	     		"			} \n" +
	     		"		} \n" +
	     		"	x = x + \"</select>\"; \n" +
	     		"   if (noSelStates == 1){" +
	     		"       var reportsState = document.getElementsByName('"+ RequestParams.REPORTS_STATE + "')[0];" +
	     		" 		var st = reportsState.options[reportsState.selectedIndex]; " +
	     		"		var repl = st.text + ', '; " +
	     		"       var re = new RegExp(repl, 'gi');" +
	     		"		x = x.replace(re, '');" +
	     		"   }    " +
	     		"	document.getElementById('countiesSelectSpan').innerHTML = x; \n" +
     			"   var reportsCounty = document.getElementsByName('"+ RequestParams.REPORTS_COUNTY + "')[0]; \n" +
	     		"   if( selectOnlyFirstOption ==true ) reportsCounty.selectedIndex = 0; \n" +
	     		" } \n"
	     		);
     		sb.append(
     			"  function filterCountySelectOptions() { \n" +
     			"  var statesOffset = "+statesOffset+"; \n" +
     			"  var reportsState = document.getElementsByName('"+ RequestParams.REPORTS_STATE + "')[0];  \n" +
     			"		if(reportsState.selectedIndex == -1 ||  reportsState.options["+allStatesOptionIndex+"].selected == true) { \n" +
     			"			for(i = 0; i < statesWithCounties.length; i++){ \n" +
     			"				statesWithCounties[i].isSelected = true; \n" +
     			"  			} \n" +
     			"			return true; \n" +
     			"			} \n" +
     			"		for (i=statesOffset;i<reportsState.options.length;i++) { \n" +
     			"			statesWithCounties[i-statesOffset].isSelected = reportsState.options[i].selected; \n" +
     			"		} \n" +
     			"	} \n"
     		);
    		 sb.append("</SCRIPT>");
    		 
            pageContext.getOut().print(sb.toString());
			//logger.debug("Debug: SelectTag#doStartTag done " + selectName);
            return(SKIP_BODY);
        }
        catch (Exception e) {
            e.printStackTrace();
			logger.error(this.getClass().toString()+"#doStartTag Exception in Tag " + this.getClass().toString()); 
        }
               
        return(SKIP_BODY);
    }
}
