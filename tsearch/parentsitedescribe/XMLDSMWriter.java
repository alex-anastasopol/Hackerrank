package ro.cst.tsearch.parentsitedescribe;

import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
public class XMLDSMWriter {
	private String nameFile = "";

	public XMLDSMWriter(String nameFile) {
		super();
		this.nameFile = nameFile;

	}

	private String getServerAdress(String serverAdress) {
		if ((serverAdress != null)
				&& (serverAdress.compareToIgnoreCase("") != 0)) {
			return "\n\t<SERVERADRESS>" + serverAdress + "</SERVERADRESS>";
		} else {
			return "";
		}
	}


	private String getGenericSite(String genericSite) {
		if ((genericSite != null)
				&& (genericSite.compareToIgnoreCase("") != 0)) {
			return "\n\t<GENERICSITE>" + genericSite + "</GENERICSITE>";
		} else {
			return "";
		}
	}


	private String getServerIp(String serverIp) {
		if ((serverIp != null) && (serverIp.compareToIgnoreCase("") != 0)) {
			return "\n\t<SERVERIP>" + serverIp + "</SERVERIP>";
		} else {
			return "";
		}
	}

	private String getServerLink(String serverLink) {
		if ((serverLink != null) && (serverLink.compareToIgnoreCase("") != 0)) {
			return "\n\t<SERVERLINK>" + serverLink + "</SERVERLINK>";
		} else {
			return "";
		}
	}

	private String getModules(String serverAdress, String serverIp,
			String serverLink, String genericSite) {
		if(serverAdress==null){
			serverAdress="";
		}
		if(serverIp==null){
			serverIp="";
		}
		if(serverLink==null){
			serverLink="";
		}
		if(genericSite==null){
			genericSite="";
		}
		
		if (((serverAdress.compareTo("") != 0) ||(genericSite.compareTo("") != 0) ||  (serverIp.compareTo("") != 0) || (serverLink
				.compareTo("") != 0))) {
			return "\n<MODULE>" + serverAdress + serverIp + serverLink+genericSite
			+ "</MODULE>";
		} else {
			return "";
		}
	}

	private String getFunctionCont(String functionCont) {
		if (functionCont.compareTo("") != 0) {
			return "\n\t\t<FunctionsCount>" + functionCont + "</FunctionsCount>";
		} else {
			return "";
		}

	}

	private String getModuleIndex(String moduleIndex) {
		if (moduleIndex.compareTo("") != 0) {
			return "\n\t\t<moduleIndex>" + moduleIndex + "</moduleIndex>";
		} else {
			return "";
		}
	}
	
	private String getModuleOrder(String moduleOrder) {
		if (StringUtils.isNotEmpty(moduleOrder)) {
			return "\n\t\t<moduleOrder>" + moduleOrder + "</moduleOrder>";
		} else {
			return "";
		}
	}
	
	private String getSearchType(String searchType) {
		if (StringUtils.isNotEmpty(searchType)) {
			return "\n\t\t<searchType>" + searchType + "</searchType>";
		} else {
			return "";
		}
	}

	private String getDestinationPage(String destinationPage) {
		if (destinationPage.compareTo("") != 0) {
			return "\n\t\t<destinationPage>" + destinationPage + "</destinationPage>";
		} else {
			return "";
		}

	}

	private String getDestinationMethod(String destinationMethod) {
		if (destinationMethod.compareTo("") != 0) {
			return "\n\t\t<destinationMethod>" + destinationMethod
			+ "</destinationMethod>";
		} else {
			return "";
		}
	}

	private String getSetName(String getName) {
		if (getName.compareTo("") != 0) {
			return "\n\t\t<setName>" + getName + "</setName>";
		} else {
			return "";
		}

	}

	private String getParcelId(String parcelId) {
		if (parcelId.compareTo("") != 0) {
			return "\n\t\t<setParcelId>" + parcelId + "</setParcelId>";
		} else {
			return "";
		}
	}

	private String getSetKey(String key) {
		if (key.compareTo("") != 0) {
			return "\n\t\t<setKey>" + key + "</setKey>";
		} else {
			return "";
		}
	}

	private String getNameParam(String name) {
		if (name.compareTo("") != 0) {
			return "\n\t\t\t\t<NAME>" + name + "</NAME>";
		} else {
			return "";
		}
	}

	private String getTypeParam(String type) {
		if (type.compareTo("") != 0) {
			return "\n\t\t\t\t<TYPE>" + type + "</TYPE>";
		} else {
			return "";
		}
	}

	private String getValueParam(String value) {
		if (value.compareTo("") != 0) {
			return "\n\t\t\t\t<VALUE>" + value + "</VALUE>";
		}

		else {
			return "";
		}

	}

	private String getParcelParam(String parcel) {
		if (parcel.compareTo("") != 0) {

			//return "\n\t\t\t\t<PARCEL>" + parcel + "</PARCEL>";
			return "";
		} else {
			return "";
		}
	}

	private String getIteratorParam(String iterator) {
		if ((iterator.compareTo("") != 0)&&(iterator.compareToIgnoreCase("-1")!=0)) {
			return "\n\t\t\t\t<ITERATOR>" + iterator + "</ITERATOR>";
		} else {
			return "";
		}
	}

	private String getKeyParam(String key) {
		if ((key.compareTo("") != 0)&&(key!=null)&&(key.compareToIgnoreCase("0")!=0 )) {
			return "\n\t\t\t\t<KEY>" + key + "</KEY>";
		} else {
			return "";
		}
	}

	private String getHidenParamName(String name) {
		if (name.compareTo("") != 0) {
			return "\n\t\t\t\t<hiddenparamname>" + name + "</hiddenparamname>";
		} else {
			return "";
		}

	}

	private String getHidenParamValue(String value) {
		if (value.compareTo("") != 0) {
			return "\n\t\t\t\t<hiddenparamvalue>" + value + "</hiddenparamvalue>";
		} else {
			return "";
		}

	}
	private String getMouleParam(int moule){
		if(moule!=-1){
			return "\n\t\t\t\t<mouleType>"+Integer.toString(moule)+"</mouleType>";
		}
		else{
			return "";
		}
	}
	private String getValidationParam(String valid){
		if(!"".equalsIgnoreCase(valid)){
			return "\n\t\t\t\t<VALIDATION>"+valid+"</VALIDATION>";
		}
		else{
			return "";
		}

	}
	private StringBuffer getParameter(Param p) {
		String name;
		String type;
		String value;
		String parcel;
		String iterator;
		String key;
		String hidenParamName;
		String hidenParamValue;
		String validationValue="";
		StringBuffer temp =new StringBuffer("");
		temp.append("\n\t\t\t<PARAM>");
		name = getNameParam(replace(p.getName()));
		type = getTypeParam(replace(p.getType()));
		value = getValueParam(replace(p.getValue().toString()));
		parcel = getParcelParam(replace(Integer.toString(p.getParcelID())));
		iterator = getIteratorParam(replace(Integer.toString(p.getIteratorType())));
		key = getKeyParam(replace(p.getSaKey()));
		hidenParamName = getHidenParamName(replace(p.getHiddenName()));
		hidenParamValue = getHidenParamValue(replace(p.getHiddenValue()));
		validationValue=getValidationParam(p.getValidationType());
		temp.append(name);
		temp.append(type);
		temp.append(value);
		temp.append( parcel);
		temp.append(iterator);
		temp.append( key);
		temp.append(hidenParamName);
		temp.append( hidenParamValue );
		temp.append( validationValue );
		temp.append( "\n\t\t\t</PARAM>");
		if (temp.toString().compareToIgnoreCase("\n\t\t\t<PARAM>\n\t\t\t</PARAM>") != 0) {
			return temp;
		} else {
			return new StringBuffer("");
		}
	}

	private StringBuffer getParameter(LinkedList<Param> list) {
		if (list.size() > 0) {
			StringBuffer temp =new StringBuffer(); 
			temp.append("\n\t\t<PARAMETRI>");
			for (int i = 0; i < list.size(); i++) {
				temp.append(getParameter(list.get(i)));
			}
			temp.append("\n\t\t</PARAMETRI>");

			return temp;

		} else {
			return new StringBuffer("");
		}
	}

	private StringBuffer getEParameter(LinkedList<Param> list) {
		if (list.size() > 0) {
			StringBuffer temp = new StringBuffer("");
			temp.append( "\n\t\t<ELSEPARAMETRI>");
			for (int i = 0; i < list.size(); i++) {
				temp.append( getParameter(list.get(i)));
			}
			temp.append("\n\t\t</ELSEPARAMETRI>");
			return temp;
		} else {
			return new StringBuffer("");
		}
	}
	private String getMoule(int moule){
		if(moule!=-1){
			return "\n\t\t<mouleType>"+Integer.toString(moule)+"</mouleType>";
		}
		else{
			return "";
		}
	}
	private String getVisible(boolean isVisible){
		if(isVisible){
			return "<visible>true</visible>";
		}
		else{
			return "<visible>false</visible>";
		}
	}
	
	/**
	 * for visibleFor flag. 
	 */
	private String getVisibleFor(String visibleFor){
		if (StringUtils.isNotEmpty(visibleFor)){
			return ("\n\t\t<visibleFor>" + visibleFor + "</visibleFor>");
		} else {
			return "";
		}
	}
	private StringBuffer getFunction(FunctionMap functionMap) {
		StringBuffer temp = new StringBuffer();
		temp.append( "\n\t<FUNCTION>");
		temp .append(getFunctionCont(replace(Integer.toString(functionMap.getFunctionsCount()))));
		temp.append(getModuleIndex(replace(Integer.toString(functionMap.getModuleIndex()))));
		temp.append(getModuleOrder(replace(Integer.toString(functionMap.getModuleOrder()))));
		temp.append(getSearchType(replace(functionMap.getSearchType())));
		temp.append( getDestinationPage(replace(functionMap.getDestinationPage())));
		temp.append(getDestinationMethod(replace(Integer.toString(functionMap.getDestinationMethod()))));
		temp.append(getSetName(replace(functionMap.getSetName())));
		temp.append(getParcelId(replace(Integer.toString(functionMap.getSetParcelId()))));
		temp.append( getSetKey(replace(functionMap.getSetKey())));
		temp.append(getMoule(functionMap.getMoule()));
		temp.append(getVisible(functionMap.getVisible()));
		temp.append(getVisibleFor(functionMap.getVisibleFor()));
		temp.append( getParameter(functionMap.getFunctionDefinedMap()));
		temp.append( getEParameter(functionMap.getFunctionDefinedElseMap()));
		temp.append("\n\t</FUNCTION>");
		if (temp.toString().compareToIgnoreCase("\n<FUNCTION>\n</FUNCTION>") != 0) {
			return temp;
		} else {
			return new StringBuffer( "");
		}
	}

	private String getZoneName(String name) {
		if (name.compareTo("") != 0) {
			return "\n\t\t<name>" + name + "</name>";
		} else {
			return "";
		}
	}

	private String getZoneLabel(String label) {
		if (label.compareToIgnoreCase("") != 0) {
			return "\n\t\t<label>" + label + "</label>";
		} else {
			return "";
		}
	}

	private String getZoneOrientation(String orientation) {
		if (orientation.compareTo("") != 0) {
			return "\n\t\t<orientation>" + orientation + "</orientation>";
		} else {
			return "";
		}
	}

	private String getZoneAlternative(String alternative) {
		if (alternative == null) {
			return "";
		} else {
			if (alternative.compareTo("") != 0) {
				if (alternative.compareToIgnoreCase("null") == 0) {
					return "\n\t\t<alternativeCSS>null</alternativeCSS>";
				} else {
					return "\n\t\t<alternativeCSS>" + alternative+ "</alternativeCSS>";
				}

			} else {
				return "";
			}
		}
	}
	
	private String getZoneExtraButton(String extraButton) {
		if (extraButton == null) {
			return "";
		} else {
			if (extraButton.compareTo("") != 0) {
				if (extraButton.compareToIgnoreCase("null") == 0) {
					return "\n\t\t<extraButton>null</extraButton>";
				} else {
					return "\n\t\t<extraButton>" + extraButton+ "</extraButton>";
				}

			} else {
				return "";
			}
		}
	}
	private String getZoneCustomFormValidation(String customFormValidation) {
		if (customFormValidation == null) {
			return "";
		} else {
			if (customFormValidation.compareTo("") != 0) {
				if (customFormValidation.compareToIgnoreCase("null") == 0) {
					return "\n\t\t<customFormValidation>null</customFormValidation>";
				} else {
					return "\n\t\t<customFormValidation>" + StringEscapeUtils.escapeXml(customFormValidation)+ "</customFormValidation>";
				}

			} else {
				return "";
			}
		}
	}
	

	private String getZoneFormTarget(String formTarget) {
		if (formTarget == null) {
			return "";
		} else {
			if (formTarget.compareTo("") != 0) {
				if (formTarget.compareToIgnoreCase("null") == 0) {
					return "\n\t\t<formTarget>null</formTarget>";
				} else {
					return "\n\t\t<formTarget>" + formTarget+ "</formTarget>";
				}

			} else {
				return "";
			}
		}
	}
	private String getZoneWidth(Integer width) {
		if(width==null){
			return "\n\t\t<width>null</width>";
		}
		else{

			return "\n\t\t<width>" + width.toString() + "</width>";

		}
	}



	private String getZoneHeight(Integer height) {
		if(height==null){
			return "\n\t\t<height>null</height>";
		}
		else{
			return "\n\t\t<height>" + height + "</height>";
		}

	}

	private String getZoneTypeOfMeasure(String type) {
		if (type.compareTo("") != 0) {
			return "\n\t\t<typeOfMeasure>" + type + "</typeOfMeasure>";
		} else {
			return "";
		}

	}

	private String getZoneBorder(boolean border) {
		if (border) {
			return "\n\t\t<border>true</border>";
		} else {
			return "\n\t\t<border>false</border>";
		}
	}

	private String getZoneSeparator(String separator) {
		if (separator.compareTo("") != 0) {
			return "\n\t\t<Separator>" + separator + "</Separator>";
		} else {
			return "";
		}

	}

	private String getZoneIsRoot(boolean root) {
		if (root) {
			return "\n\t\t<isRoot>true</isRoot>";
		} else {
			return "\n\t\t<isRoot>false</isRoot>";
		}

	}

	private String getHtmlControl(String control) {
		if (control.compareTo("") != 0) {
			return "\n\t\t\t<controlType>" + control + "</controlType>";
		} else {
			return "";
		}
	}

	private String getHtmlName(String name) {
		if (name.compareTo("") != 0) {
			return "\n\t\t\t<name>" + name + "</name>";
		} else {
			return "";
		}

	}

	private String getHtmlLabel(String label) {
		if (label.compareTo("") != 0) {
			return "\n\t\t\t<label>" + label + "</label>";
		} else {
			return "";
		}

	}

	private String getHtmlColStart(String col) {
		if (col.compareTo("") != 0) {
			return "\n\t\t\t<colStart>" + col + "</colStart>";
		} else {
			return "";
		}
	}

	private String getHtmlColEnd(String col) {
		if (col.compareTo("") != 0) {
			return "\n\t\t\t<colEnd>" + col + "</colEnd>";
		} else {
			return "";
		}

	}

	private String getHtmlRowStart(String row) {
		if (row.compareTo("") != 0) {
			return "\n\t\t\t<rowStart>" + row + "</rowStart>";
		} else {
			return "";
		}

	}

	private String getHtmlRowEnd(String row) {
		if (row.compareTo("") != 0) {
			return "\n\t\t\t<rowEnd>" + row + "</rowEnd>";
		} else {
			return "";
		}
	}

	private String getHtmlSize(String size) {
		if (size.compareTo("") != 0) {
			return "\n\t\t\t<size>" + size + "</size>";
		} else {
			return "";
		}
	}

	private String getHtmlDefaultValue(Object value) {
		if(value==null){
			return "\n\t\t\t<defaultValue>null</defaultValue>";
		}
		if (value.toString().compareTo("") != 0) {
			return "\n\t\t\t<defaultValue>" + replace(value.toString()) + "</defaultValue>";
		} else {
			return "";
		}
	}

	private String getHtmlTssFunction(String function) {
		if (function.compareTo("") != 0) {
			return "\n\t\t\t<tssFunction>" + function + "</tssFunction>";
		} else {
			return "";
		}
	}

	private String getHtmlFieldNote(String note) {
		if (note.compareTo("") != 0) {
			return "\n\t\t\t<FieldNote>" + note + "</FieldNote>";
		} else {
			return "";
		}

	}

	private String getHtmlSelectValue(String value) {
		if (value.compareTo("") != 0) {
			return "\n\t\t\t<SelectValue>" + value + "</SelectValue>";
		} else {
			return "";
		}
	}

	private String getHtmlValueRequired(boolean value) {
		if (value) {
			return "\n\t\t\t<valueRequired>true</valueRequired>";
		} else {
			return "\n\t\t\t<valueRequired>false</valueRequired>";
		}

	}

	private String getHtmlRequiredExcl(boolean required) {
		if (required) {
			return "\n\t\t\t<requiredExcl>true</requiredExcl>";
		} else {
			return "\n\t\t\t<requiredExcl>false</requiredExcl>";
		}
	}

	private String getHtmlRequiredCritical(boolean critical) {
		if (critical) {
			return "\n\t\t\t<requiredCritical>true</requiredCritical>";
		} else {
			return "\n\t\t\t<requiredCritical>false</requiredCritical>";
		}
	}

	private String getHtmlHbutton(boolean button) {
		if (button) {
			return "\n\t\t\t<horizontalRadioButton>true</horizontalRadioButton>";
		} else {
			return "\n\t\t\t<horizontalRadioButton>false</horizontalRadioButton>";
		}

	}

	private String getHtmlJustyField(boolean justy) {
		if (justy) {
			return "\n\t\t\t<justifyField>true</justifyField>";
		} else {
			return "\n\t\t\t<justifyField>false</justifyField>";
		}
	}

	private String getHtmlRadioDefaultChecked(String checked) {
		if (checked.compareTo("") != 0) {
			return "\n\t\t\t<radioDefaultSelection>" + checked + "</radioDefaultSelection>";
		} else {
			return "";
		}

	}

	private String getHtmlJSFunction(String function) {
		if (function.compareTo("") != 0) {
			return "\n\t\t\t<JSFunction>" + function + "</JSFunction>";
		} else {
			return "";
		}

	}
	
	private String getHtmlExtraClass(String extraClass) {
		if(extraClass.compareTo("") != 0) {
			return "\n\t\t\t<extraClass>" + extraClass + "</extraClass>";
		} else {
			return "";
		}
	}
	
	private String getHtmlDefaultOnReplicate(String defaultOnReplicate) {
		if(StringUtils.isNotEmpty(defaultOnReplicate)) {
			return "\n\t\t\t<defaultOnReplicate>" + defaultOnReplicate + "</defaultOnReplicate>";
		} else {
			return "";
		}
	}

	private String getHtmlHidenParam(boolean hiden) {
		if (hiden) {
			return "\n\t\t\t<HiddenParam>true</HiddenParam>";
		} else {
			return "\n\t\t\t<HiddenParam>false</HiddenParam>";
		}

	}
	private String 	getHtmlHtmlString(String html){
		if ((html!=null)&&(!"".equals(html))) {
			return "\n\t\t\t<HtmlString>"+html+"</HtmlString>";
		} else {
			return "";
		}

	}

	private String getHtmlDefaultSelection(String defaultSelection){
		if((defaultSelection!=null)&&(!"".equalsIgnoreCase(defaultSelection))){
			return "\n\t\t\t<radioDefaultChecked>"+defaultSelection+"</radioDefaultChecked>";
		}
		else{
			return "";
		}
	}
	private StringBuffer getZoneHtml(HtmlControlMap htmlc) {
		StringBuffer temp=new StringBuffer(""); 
		temp.append( "\n\t\t<HTML>");
		temp.append(getHtmlControl(replace(Integer.toString(htmlc.getControlType()))));
		temp.append(getHtmlName(replace(htmlc.getName())));
		temp.append(getHtmlLabel(replace(htmlc.getLabel())));
		temp.append(getHtmlColStart(replace(Integer.toString(htmlc.getColStart()))));
		temp.append(getHtmlColEnd(replace(Integer.toString(htmlc.getColEnd()))));
		temp.append(getHtmlRowStart(replace(Integer.toString(htmlc.getRowStart()))));
		temp.append(getHtmlRowEnd(replace(Integer.toString(htmlc.getRowEnd()))));
		temp.append(getHtmlSize(replace(replace(Integer.toString(htmlc.getSize())))));
		temp.append(getHtmlDefaultValue(htmlc.getDefaultValue()));
		temp.append(getHtmlTssFunction(replace(Integer.toString(htmlc.getTssFunction()))));
		temp.append(getHtmlFieldNote(replace(htmlc.getFieldNote())));
		temp.append(getHtmlSelectValue(replace(htmlc.getComboValueString())));
		temp.append(getHtmlValueRequired(htmlc.getValueRequired()));
		temp.append(getHtmlRequiredExcl(htmlc.getRequiredExcl()));
		temp.append(getHtmlRequiredCritical(htmlc.getRequiredCritical()));
		temp.append(getHtmlHbutton(htmlc.getHorizontalRadioButton()));
		temp.append(getHtmlJustyField(htmlc.getJustifyField()));
		temp.append( getHtmlRadioDefaultChecked(replace(htmlc.getRadioDefaultChecked())));
		temp.append(getHtmlJSFunction(replace(htmlc.getJSFunction())));
		temp.append(getHtmlHidenParam(htmlc.getHiddenparam()));
		temp.append(getHtmlHtmlString(replace(htmlc.getHtmlString())));
		temp.append(getHtmlExtraClass(htmlc.getExtraClass()));
		temp.append(getHtmlDefaultOnReplicate(htmlc.isDefaultOnReplicate()+""));
		temp.append("\n\t\t</HTML>");
		if (temp.toString().compareToIgnoreCase("\n\t\t<HTML>\n\t\t</HTML>") != 0) {
			return temp;
		} else {
			return new StringBuffer("");
		}
	}

	private StringBuffer getPageZone(PageZoneMap pageZone) {
		StringBuffer temp = new StringBuffer();
		temp.append("\n\t<pagezone>");
		temp.append(getZoneName(replace(pageZone.getName())));
		temp.append(getZoneLabel(replace(pageZone.getLabel())));
		temp.append(getZoneOrientation(replace(Integer.toString(pageZone.getOrientation()))));
		temp.append(getZoneAlternative(replace(pageZone.getAlternativeCSS())));
		temp.append(getZoneExtraButton(replace(pageZone.getExtraButton())));
		temp.append(getZoneFormTarget(replace(pageZone.getFormTarget())));
		temp.append( getZoneWidth(pageZone.getWidth()));
		temp.append( getZoneHeight(pageZone.getHeight()));
		temp.append(getZoneTypeOfMeasure(replace(Integer.toString(pageZone.getTypeOfMeasure()))));
		temp.append(getZoneSeparator(pageZone.getSeparator()));
		temp.append(getZoneBorder(pageZone.getBorder()));
		temp.append(getZoneIsRoot(pageZone.isRoot()));
		temp.append(getZoneCustomFormValidation(pageZone.getCustomFormValidation()));
		LinkedList<HtmlControlMap> list = pageZone.getHtmlControlMap();
		for (int i = 0; i < list.size(); i++) {
			temp.append( getZoneHtml(list.get(i)));
		}
		temp.append( "\n\t</pagezone>");
		if (temp.toString().compareToIgnoreCase("\n\t<pagezone>\n\t</pagezone>") != 0) {
			return temp;
		} else {
			return new StringBuffer("");
		}
	}

	private StringBuffer getZone(ModuleXMLMap module) {
		StringBuffer function =new StringBuffer();
		StringBuffer tmp =new StringBuffer();
		tmp.append("");
		function.append(getFunction(module.getFunctionMap()));

		StringBuffer page =new StringBuffer(); 
		page.append(getPageZone(module.getZoneMap()));
		if ((function.toString().compareTo("") != 0) || (page.toString().compareTo("") != 0)) {
			tmp.append( "\n<zone>");
			tmp.append(function);
			tmp.append(page);
			tmp.append( "\n</zone>");
			return tmp;
		} else {
			return new StringBuffer("");
		}

	}
	
	private String replace(String replace){
		return StringEscapeUtils.escapeXml(replace);
	}
	
	public String writeString(DefaultServerInfoMap serverM)
	{
		StringBuffer tmpBuffer=new StringBuffer(); 
		tmpBuffer.append("");
		tmpBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tipp>\n");
		tmpBuffer.append(getModules(getServerAdress(replace(serverM.getServerAdress())),getServerIp(replace(serverM.getServerIp())),getServerLink(replace(serverM.getServerLink())),getGenericSite(serverM.getGenericSite())));
		for (int i = 0; i < serverM.getModuleNumber(); i++) {
			tmpBuffer.append(getZone(serverM.getOneModule(i)));
		}
		tmpBuffer.append("\n</tipp>");

		return tmpBuffer.toString();
	}
	
	public boolean alertMail(String nameFiles, String before,String after,String serverAdress,String serverIp, String remoteAdress, String remoteIp){
		Date t=new Date();
		if(nameFiles==null){
			nameFiles="";
		}
		if(!before.equalsIgnoreCase(after) && !URLMaping.INSTANCE_DIR.startsWith("local")){
			EmailClient em=new EmailClient();
			em.addTo(MailConfig.getMailLoggerStatusAddress(),"");
			em.setSubject("~Parent Site Send File~ on " + URLMaping.INSTANCE_DIR + " File name "+nameFiles);
			em.addContent("Modification\n"+t.toString()+"\nServer name: "+URLMaping.INSTANCE_DIR+" \n Server adress:"+serverAdress+" \n Server IP  :"+serverIp+"\n  Remote Adress:  "+remoteAdress+"\n  RemoteIP: "+remoteIp);
			em.addAttachmentContaints("After"+nameFiles, after);
			em.addAttachmentContaints("Before"+nameFiles, before);
			em.sendAsynchronous();
			System.err.println(">>>>>> Send Email Notification changes in ParentSite XML >>>>>");
		}
		return true;
	}
	
	public boolean  writeFile(DefaultServerInfoMap serverM) {
		
		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		if((DSMXMLReader.cache.get(this.nameFile)!=null)&&(serverM!=null)){
			alertMail(this.nameFile,writeString(DSMXMLReader.cache.get(this.nameFile)),writeString(serverM),serverM.getInfoServerAdress(),serverM.getInfoServerIp(),serverM.getInfoRemoteAdress(),serverM.getInfoRemoteIp());
		}
		//DBManager.writeXMLFileContentsToDb( this.nameFile , writeString(serverM).getBytes());
		
		//StringUtils.toFile("/xmltot/"+this.nameFile,writeString(serverM));
		StringUtils.toFile(BaseServlet.REAL_PATH+rbc.getString("parentsite.xml.path").trim()+this.nameFile,writeString(serverM));
		DSMXMLReader.replaceCache(this.nameFile,serverM);

		System.err.println("SCRIU CACHE CU  fisierul   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  "+this.nameFile);
		return true;
	}
	
	
	public void writeFileUpload(String director, String fName){

		String directorTmp=DSMXMLReader.DIRECTOR;
		boolean readbd=DSMXMLReader.readBD;
		DSMXMLReader.readBD=false;
		DSMXMLReader.DIRECTOR=director;
		DSMXMLReader tempReader = new DSMXMLReader(  fName     );
		XMLDSMWriter writer = new XMLDSMWriter(fName);
		writer.writeFile(tempReader.readXML(true));
		DSMXMLReader.DIRECTOR=directorTmp;
		DSMXMLReader.readBD=readbd;
		System.err.println("UPLOAD       FILE    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  "+this.nameFile);
	}
}
