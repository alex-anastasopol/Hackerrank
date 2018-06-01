package ro.cst.tsearch.connection.http2;


public class COArapahoeTR extends AdvancedTemplateSite {
	
	public COArapahoeTR() {
		mainParameters = new String[2];
		mainParameters[0] = "__VIEWSTATE";
		mainParameters[1] = "__EVENTVALIDATION" ;
		mainParametersKey = "search.params";
		formName =  "form1";
		
		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[4];
		targetArgumentParameters[0] = "__VIEWSTATE";
		targetArgumentParameters[1] = "__EVENTVALIDATION" ;
		targetArgumentParameters[2] = "__EVENTTARGET";
		targetArgumentParameters[3] = "__EVENTARGUMENT" ;
		
	}


}
