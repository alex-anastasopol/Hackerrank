package ro.cst.tsearch.servers.info.spitHTML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

public class PageZone extends HTMLObject {
	
	protected HashMap childObjects;
	protected boolean isRoot = false;
	
	protected int width = 0, height = 0;
	protected boolean border = true;
	
	protected boolean needsBreak = true;
	protected int typeOfMeasure;
	
	private boolean hasExclFields = false;
	private boolean hasCritFields = false;	
	private String extraButton = "";
	private String formTarget = "";
	private String customValidation = "";
	//	constructor
	
	public PageZone(
			String name,
			String label,
			int orientation,
			String alternateCSS,
			Integer width,
			Integer height,
			int typeOfMeasure,
			boolean isRoot)
	{
		this.orientation = orientation;
		this.label = label;
		this.name = name;
		if (width != null)
		{
			this.width = width.intValue();
		}
		if (height != null)
		{
			this.height = height.intValue();
		}
		
		this.height = 0;
		
		childObjects = new HashMap();
		this.isRoot = isRoot;
		this.typeOfMeasure = typeOfMeasure;
	}
	
	public PageZone(
			String name,
			String label,
			int orientation,
			String alternateCSS,
			Integer width,
			Integer height,
			int typeOfMeasure,
			boolean isRoot, String customValidation) {
		this(name, label, orientation, alternateCSS, width, height, typeOfMeasure, isRoot);
		this.customValidation = customValidation;
	}
	
	public void setBounds(int colFrom, int colTo, int rowFrom, int rowTo){
		// daca colFrom == colTo se va initializa un int[] de dimensiune 1 si va avea valoarea {colFrom}
		// in rest {colFrom, colTo}
		int[] newColIndex = new int[(colFrom == colTo ? 1 : 2)];
		int[] newRowIndex = new int[(rowFrom == rowTo ? 1 : 2)];
		if (colFrom == colTo){
			newColIndex[0] = colFrom;
		} else {
			newColIndex[0] = colFrom;
			newColIndex[1] = colTo;
		}
		if (rowFrom == rowTo){
			newRowIndex[0] = rowFrom;
		} else {
			newRowIndex[0] = rowFrom;
			newRowIndex[1] = rowTo;
		}
		try{
			setColIndex(newColIndex);
			setRowIndex(newRowIndex);
		}catch (FormatException e){
			e.printStackTrace();
		}
	}
	
	
	public class ReqFields
	{
		private HashMap fieldsExcl = new HashMap();
		private HashMap fieldsCrit = new HashMap();
		/**
		 * @return Returns the fieldsCrit.
		 */
		public HashMap getFieldsCrit() {
			return fieldsCrit;
		}
		/**
		 * @param fieldsCrit The fieldsCrit to set.
		 */
		public void setFieldsCrit(HashMap fieldsCrit) {
			this.fieldsCrit = fieldsCrit;
		}
		/**
		 * @return Returns the fieldsExcl.
		 */
		public HashMap getFieldsExcl() {
			return fieldsExcl;
		}
		/**
		 * @param fieldsExcl The fieldsExcl to set.
		 */
		public void setFieldsExcl(HashMap fieldsExcl) {
			this.fieldsExcl = fieldsExcl;
		}
	}
	
	public ReqFields getFields() 
	{
		ReqFields fields = new ReqFields();
		HashMap fieldsExcl = new HashMap();
		HashMap fieldsCrit = new HashMap();
		Iterator i = childObjects.entrySet().iterator();
		while (i.hasNext()) 
		{
			HTMLObject ho = (HTMLObject) ((Entry) i.next()).getValue();
			if (ho instanceof PageZone) 
			{
				fieldsExcl.putAll(((PageZone) ho).getFields().getFieldsExcl());
				fieldsCrit.putAll(((PageZone) ho).getFields().getFieldsCrit());
			} 
			else if (ho instanceof HTMLControl) 
			{
				if (((HTMLControl) ho).isRequiredExcl())
				{
					fieldsExcl.put(((HTMLControl) ho).getAliasName(), ((HTMLControl) ho).getLabel());
				}
				if (((HTMLControl) ho).isRequiredCritical())
				{
					fieldsCrit.put(((HTMLControl) ho).getAliasName(), ((HTMLControl) ho).getLabel());
				}
			}
		}
		fields.setFieldsExcl(fieldsExcl);
		fields.setFieldsCrit(fieldsCrit);
		return fields;
	}
	
	public String getFieldsForJS() {
		hasExclFields = false;
		hasCritFields = false;
		String fieldExclNames = "var fieldExclNames_" + getFormIndex() + " = new Array(";
		String fieldExclLabels = "var fieldExclLabels_" + getFormIndex() + " = new Array(";
		String fieldCritNames = "var fieldCritNames_" + getFormIndex() + " = new Array(";
		String fieldCritLabels = "var fieldCritLabels_" + getFormIndex() + " = new Array(";
		ReqFields fields = getFields(); 
		Iterator i = fields.getFieldsExcl().keySet().iterator();
		while (i.hasNext())
		{
			String key = i.next().toString();
			fieldExclNames += "\"" + key + "\", ";
			fieldExclLabels += "\"" + fields.getFieldsExcl().get(key) + "\", ";
			hasExclFields = true;
		}
		
		i = fields.getFieldsCrit().keySet().iterator();
		while (i.hasNext())
		{
			String key = i.next().toString();
			fieldCritNames += "\"" + key + "\", ";
			fieldCritLabels += "\"" + fields.getFieldsCrit().get(key) + "\", ";
			hasCritFields = true;
		}
		
		if (!hasCritFields)
		{
			fieldCritNames += ");";
			fieldCritLabels += ");";
		}
		else
		{
			fieldCritNames = fieldCritNames.substring(0, 
					fieldCritNames.length() - 2) + ");";
			fieldCritLabels = fieldCritLabels.substring(0, 
					fieldCritLabels.length() - 2) + ");";
		}
		if (!hasExclFields)
		{
			fieldExclNames += ");";
			fieldExclLabels += ");";
		}
		else
		{
		fieldExclNames = fieldExclNames.substring(0, 
				fieldExclNames.length() - 2) + ");";
		fieldExclLabels = fieldExclLabels.substring(0, 
				fieldExclLabels.length() - 2) + ");";
		}
		
		
		return fieldExclNames + "\n\n" + fieldExclLabels + "\n\n" + fieldCritNames + "\n\n" + fieldCritLabels;
	}
		
	private String getCheckTextArea(){
		StringBuilder temp = new StringBuilder();
		Object sss[]=childObjects.keySet().toArray();
		temp.append("\n")
				.append("function checkTextArea").append(this.formIndex).append("(){\n")
				.append("\n var listType = new Array();\n")
				.append("\n var listName = new Array();\n")
				.append("var n1=-1;\n")
				.append("var nrModuleIndex=").append(this.formIndex).append(";\n")
				.append("var nrOfParam=").append(sss.length).append(";\n") ;
		
		int k=0;
		int contor=0;
		for(k=0;k<sss.length;k++){
			Object childObject = childObjects.get(sss[k]);
			if(childObject instanceof HTMLControl) {
				HTMLControl hObj = (HTMLControl) childObject;
				hObj.getCurrentTSSiFunc().getParamAlias();
				if(!hObj.isHiddenParam()){
					temp.append("listType[").append(contor).append("]='").append(hObj.getRestriction()).append("';\n");
					temp.append("listName[").append(contor).append("]='").append(hObj.getCurrentTSSiFunc().getParamAlias()).append("';\n");;
					++contor;
				}
				hObj=null;
			}
		}
		
		if(customValidation != null) {
			temp.append(customValidation).append("\n");
		}
		
		temp.append("var nrOfParam=").append(contor).append(";\n");
		temp.append("var i;\n")
				.append("var valid=true;\n")
				.append("var s='';\n")
				.append("var test;\n")
				.append("for(i=0;i<nrOfParam;i++){\n")
				.append("\tif(listType[i]!=''){\n")
				.append("\t\ts=document.getElementById(listName[i]).value;\n")
				.append("\t\tn1 = s.search(new RegExp(listType[i]));\n")
				.append("\t\tif((n1!=-1)&&(listType[i]!='')){\n")
				.append("\t\t\talert('The Value   >>> '+s+'<<<     is not valid');\n")
				.append("\t\tvalid = false;\n")
				.append("\t}\n")
				.append("\t}\n}\n")
				.append("if(valid){\n")
				.append("checkForm(").append(getFormIndex()).append(", '").append(this.formTarget).append("', '").append(this.formIndex).append("');\n")
				.append("}\n");

		temp.append("}\n");
		
		return temp.toString();
	}
	
	public String render() throws FormatException {
		
		StringBuffer HTMLData = new StringBuffer();
		
		if (isRoot)
		{
			HTMLData.append("<SCRIPT>\n\n");
			HTMLData.append(getFieldsForJS());
			HTMLData.append(getCheckTextArea());
			HTMLData.append(getCustomJSFunction().toString());
			HTMLData.append("\n\n</SCRIPT>\n\n\n");
		}
		
		String tableTag = "";
		tableTag = "<table id='main_table' " +
					(width != 0 ? " width='" + width + "'":"") +
					(height != 0 ? " height='" + height + "'":"") +
					" cellpadding='0' cellspacing='0' " +
					">";
		if (typeOfMeasure == HTMLObject.PERCENTS){
			tableTag = "<table id='main_table' " +
			" width='" + width + "%'" +
			" height='" + height + "%'" +
			" cellpadding='0' cellspacing='0' " +
			">";
		}
		HTMLData.append(tableTag);
		HTMLData.append("<tbody>" + "\n");
		//	punem headerul tabelei
		if (this.hasHeder())
		{
			HTMLData.append("<tr>" + "\n");
			HTMLData.append("<td class='header' " + 
								" width='100%' valign='middle' " + 
								(orientation == ORIENTATION_VERTICAL ? 
										"colspan=" + childObjects.size() 
										:
										"") + 
								" >" + "\n");
			HTMLData.append(label + "\n");
			HTMLData.append("</td>" + "\n");
			HTMLData.append("</tr>" + "\n");
		}
		//	vedem dupa ce orientare sa punem PageZone-urile
		HTMLData.append(orientation == ORIENTATION_VERTICAL ? "<TR>" + "\n" : "");
		if (childObjects != null)
		{
			
			////////////// Grupam controalerele HTML
			////////////// <<<<<<<<<
			
			HTMLControlCollector hcc = new HTMLControlCollector(childObjects, true, INCLUDE_PAGEZONES);
			if (!hcc.getComponents().isEmpty())
			{
				childObjects.put(new Integer(hcc.hashCode()), hcc);
			}
			
			///////////// >>>>>>>>>>
			////////////////////////////////////////////
			
			//	sortam pagezoneurile
			ArrayList keys = new ArrayList();
			Iterator it = childObjects.keySet().iterator();
			while (it.hasNext()) {
				keys.add(it.next());
			}
			Collections.sort(keys);
			
			Iterator i = keys.iterator();
			while (i.hasNext()) 
			{
				try 
				{
					
					HTMLObject ho = (HTMLObject) childObjects.get(i.next());
					HTMLData.append(
							orientation == ORIENTATION_VERTICAL 
							? 
									"<TD valign='top' " +
									"width='" + ho.getSize() +
									"'>"  + "\n"
									:
									"<TR id = 'tr_"+name+"'><TD valign='top' " +
									( border ? "class='pageZoneData' " : "class='pageZoneDataNoBorder' ") +
									"height='" +
									ho.getSize() + "'>"  + "\n");
					HTMLData.append(ho.render());
					HTMLData.append(orientation == ORIENTATION_VERTICAL ? "</TD>" + "\n":"</TD></TR>" + "\n");
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return "";
				}
				
			}
		}
		HTMLData.append(orientation == ORIENTATION_VERTICAL ? "</TR>" + "\n" : "");
		/*
		if (needsBreak){
			// punem un tr gol pentru completare
			HTMLData.append("<tr><td height='*' class='separationTD'>&nbsp;</td></tr>" + "\n");
		}
		*/
		if (isRoot)
		{
			HTMLData.append("<tr>"+ "\n");
			HTMLData.append("<td align=\"right\" class=\"trteen\">"+ "\n");
			HTMLData.append("<table width='100%'><tr><td>"+ "\n");
			
			if( hasCritFields )
			{
			    HTMLData.append("<font color='red'><b>*</b></font>&nbsp;&nbsp;These Fields Are Required</td>"+ "\n");
			}
			
			HTMLData.append( "<td align='right'>" + "\n");
			//checkForm(" + getFormIndex() + ");
			//checkTextArea()
			if(!StringUtils.isEmpty(extraButton)) {
				if (extraButton.contains("Auto Add")) {
					HTMLData.append("<input type=\"hidden\" name=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT+name+"\" id=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT+name+"\" value=0 />"+ "\n");
				} 
				
				if(extraButton.contains("ButtonAdd")) {
					HTMLData.append("<input type=\"hidden\" name=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT+name+"\" id=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT+name+"\" value=0 />"+ "\n");
					HTMLData.append("<input type=\"button\" class=\"button\" name=\""+extraButton+"\" value=\""+extraButton+"\" onClick=\"javascript:addAditionalFunction('"+name+"');\"/>"+ "\n");
				} 
				
				if(extraButton.contains("AutoRowAdd")){
					HTMLData.append("<input type=\"hidden\" name=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT_ROW+name+"\" id=\""+RequestParams.PARENT_SITE_ADDITIONAL_CNT_ROW+name+"\" value=0 />"+ "\n");
				}
				
				//implement other buttons
			}
			if(StringUtils.isEmpty(formTarget)) {
				formTarget = "_self";
			}
			HTMLData.append("<input type=\"button\" class=\"button\" name=\"Button\" value=\"Submit\" onClick=\"javascript:this.form.target='"+formTarget+"';checkTextArea"+Integer.toString(this.formIndex) +"()\"/>"+ "\n");			
//			HTMLData.append("<input type=\"reset\" class=\"button\" name=\"Reset\" value=\"Reset\" />"+ "\n");
			HTMLData.append("<input type=\"button\" class=\"button\" name=\"Reset\" value=\"Reset\" onClick=\"resetForm(this);\"/>"+ "\n");
			HTMLData.append("<input type=\"button\" class=button  name=\"Clear Form\" value=\"Clear Form\" onClick=\"clearForm();\"/>"+ "\n");
			HTMLData.append("</td></tr></table></td></tr>"+ "\n");
		}
		HTMLData.append("</tbody>" + "\n");
		HTMLData.append("</table>" + "\n");
		
		if( isRoot )
		{
		    HTMLData.append("<br>\n");
		}
		if (HTMLData.toString().matches("(?is).*([&]lt[;]\\s*br\\s*[/]?\\s*[&]gt[;]).*"))
		{
			HTMLData = new StringBuffer (HTMLData.toString().replaceAll("(?is)[&]lt[;]\\s*br\\s*[/]?\\s*[&]gt[;]", "<br>"));
		}
		
		return HTMLData.toString();
		
	}
	
	public void addHTMLObject(HTMLObject hObj) 
			throws FormatException{
		
		if (hObj instanceof PageZone
				&& checkSizes((PageZone) hObj)) 
		{
			hObj.setParent(this);
			childObjects.put(new Integer(childObjects.size() + 1), hObj);
		} 
		else if (!(hObj instanceof PageZone))
		{
			hObj.setParent(this);
			childObjects.put(hObj.getName(), hObj);
		}
		
	}
	
	public void addHTMLObjectMulti(HTMLObject... objs) throws FormatException{
		for(HTMLObject obj: objs){
			addHTMLObject(obj);
		}
	}
	
	public boolean hasHeder() {
		if (label != null
				&& label.length() > 0)
		{
			return true;
		}
		return false;
	}
	
	protected boolean checkSizes(PageZone pageToBeInserted) {
		if (childObjects == null
				|| childObjects.size() == 0)
		{
			return true;
		}
		Iterator i = childObjects.entrySet().iterator();
		int sumPercents = 0;
		while (i.hasNext()) 
		{
			try
			{
				HTMLObject ho = (HTMLObject) i.next();
				sumPercents += ho.getSize();
			}
			catch (Exception e)
			{
				return true;
			}
		}
		
		if (sumPercents + pageToBeInserted.getSize() > 100)
		{
			return false;
		}
		else 
		{
			return true;
		}
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeightSize() {
		return height;
	}

	/**
	 * @param height The height to set.
	 */
	public void setHeightSize(int height) {
		this.height = height;
	}

	/**
	 * @return Returns the width.
	 */
	public int getWidthSize() {
		return width;
	}

	/**
	 * @param width The width to set.
	 */
	public void setWidthSize(int width) {
		this.width = width;
	}

	/**
	 * @return Returns the isRoot.
	 */
	public boolean isRoot() {
		return isRoot;
	}

	/**
	 * @param isRoot The isRoot to set.
	 */
	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	/**
	 * @return Returns the border.
	 */
	public boolean isBorder() {
		return border;
	}

	/**
	 * @param border The border to set.
	 */
	public void setBorder(boolean border) {
		this.border = border;
	}

	public boolean isNeedsBreak() {
		return needsBreak;
	}

	public void setNeedsBreak(boolean needsBreak) {
		this.needsBreak = needsBreak;
	}

	public int getTypeOfMeasure() {
		return typeOfMeasure;
	}

	public void setTypeOfMeasure(int typeOfMeasure) {
		this.typeOfMeasure = typeOfMeasure;
	}
	
	public Collection<HTMLControl> getHtmlControls() 
	{
		Collection<HTMLControl> allHtmlControls = new Vector<HTMLControl>();
		Iterator i = childObjects.entrySet().iterator();
		while (i.hasNext()) 
		{
			HTMLObject ho = (HTMLObject) ((Entry) i.next()).getValue();
			if (ho instanceof HTMLControl) 
			{
				allHtmlControls.add((HTMLControl) ho);
			}
		}
		return allHtmlControls;
	}

	public String getExtraButton() {
		return extraButton;
	}

	public void setExtraButton(String extraButton) {
		this.extraButton = extraButton;
	}

	public String getFormTarget() {
		return formTarget;
	}

	public void setFormTarget(String formTarget) {
		this.formTarget = formTarget;
	}

	private StringBuilder customJSFunction = new StringBuilder("");
	
	public StringBuilder getCustomJSFunction() {
		return customJSFunction;
	}

	public void setCustomJSFunction(StringBuilder customJSFunction) {
		this.customJSFunction = customJSFunction;
	}

	public String getCustomValidation() {
		return customValidation;
	}

	public void setCustomValidation(String customValidation) {
		this.customValidation = customValidation;
	}

	
}
