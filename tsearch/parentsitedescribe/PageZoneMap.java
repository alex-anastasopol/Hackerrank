package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;

public class PageZoneMap {
	
	//LinkedList <Param> area;
	private int htmlControlNamber=0;
	private String separator="";
	private String name="";
	private String label="";
	private int orientation=0;
	private String  alternativeCSS=null;
	private Integer width=0;
	private Integer height=0;
	private int typeOfMeasure=0;
	private boolean isRoot=false;
	private boolean border=false;
	private String customFormValidation = "";
	private String extraButton = "";
	private String formTarget = ""; 
	private LinkedList<HtmlControlMap> htmlControlMap=null;

	public void replace(PageZoneMap zone){
	this.setHtmlControlNamber(zone.getHtmlControlNamber());
	this.setSeparator(zone.getSeparator());
	this.setExtraButton(zone.getExtraButton());
	this.setCustomFormValidation(zone.getCustomFormValidation());
	this.setFormTarget(zone.getFormTarget());
	this.setName(zone.getName());
	this.setLabel(zone.getLabel());
	this.setOrientation(zone.getOrientation());
	this.setWidth(zone.getWidth());
	this.setHeight(zone.getHeight());
	this.setTypeOfMeasure(zone.getTypeOfMeasure());
	this.setRoot(zone.isRoot());
	this.setBorder(zone.getBorder());
	for(int i=0;i<zone.htmlControlMap.size();i++){
			HtmlControlMap htmlc = new HtmlControlMap();
			htmlc.replace(zone.htmlControlMap.get(i));
			this.add(htmlc);
			htmlc=null;
		}
	}
	public PageZoneMap toEscape(){
		PageZoneMap tmp=this;
		tmp.separator=escapeString(tmp.separator);
		tmp.name=escapeString(tmp.name);
		tmp.label=escapeString(tmp.label);
		 LinkedList<HtmlControlMap> htm=tmp.htmlControlMap;
		 tmp.htmlControlMap=new LinkedList<HtmlControlMap>();
		 for(int i =0; i<htm.size();i++){
			 tmp.htmlControlMap.add(htm.get(i).toEscape());
			 }
		//tmp.separator=escapeString(tmp.separator);
		return tmp;
	}
	public String escapeString(String escape){

		escape=escape.replaceAll( "&amp;","&");
		escape=escape.replaceAll("&quot;","\"");
		escape=escape.replaceAll("&apos;","'");
		escape=escape.replaceAll( "&lt;","<");
		escape=escape.replaceAll( "&gt;",">");

		
		escape=escape.replaceAll("&", "&amp;");
		escape=escape.replaceAll("\"", "&quot;");
		escape=escape.replaceAll("'", "&apos;");
		escape=escape.replaceAll("<", "&lt;");
		escape=escape.replaceAll(">", "&gt;");
	
		return escape;
	}

	public void setZone(PageZoneMap zone){
		if(zone!=null){
			
			if("".equalsIgnoreCase(this.getName())){
				this.setName(zone.getName());
			}
			
			if("".equalsIgnoreCase(this.getLabel())){
				this.setLabel(zone.getLabel());
			}
			if("".equalsIgnoreCase(this.getSeparator())){
				this.setSeparator(zone.getSeparator());
			}
			if("".equalsIgnoreCase(this.getExtraButton())){
				this.setExtraButton(zone.getExtraButton());
			}
			if(StringUtils.isNotBlank(this.getCustomFormValidation())){
				this.setCustomFormValidation(zone.getCustomFormValidation());
			}
			if("".equalsIgnoreCase(this.getFormTarget())){
				this.setFormTarget(zone.getFormTarget());
			}
			
			if(this.getOrientation()==0){
				this.setOrientation(zone.getOrientation());
			}
			if("".equalsIgnoreCase(this.getAlternativeCSS())){
				this.setAlternativeCSS(zone.getAlternativeCSS());
			}
			if(this.getWidth()==0){
				this.setWidth(zone.getWidth());
			}
			if(this.getHeight()==0){
				this.setHeight(zone.getHeight());
			}
			this.setRoot(zone.isRoot());
			this.setBorder(zone.getBorder());
		}
		LinkedList<HtmlControlMap> htmlc=null;
		HtmlControlMap h1=null;
		HtmlControlMap h2=null;
		htmlc=zone.getHtmlControlMap();
		boolean isHtml=false;
		int marime=this.getHtmlControlMap().size();
		for(int i=0;i<htmlc.size();i++){
			isHtml=false;
			for(int j=0;j<marime;j++){
				if((htmlc.get(i).getName().compareToIgnoreCase(this.getHtmlControlMap().get(j).getName())==0)&&(htmlc.get(i).getLabel().compareToIgnoreCase(this.getHtmlControlMap().get(j).getLabel())==0)){
					isHtml=true;
				}
			}
			
			if(!isHtml){
				htmlc.get(i).setTssFunction(this.htmlControlMap.size());
				this.add(htmlc.get(i));
			}
		}
	}

	
	public PageZoneMap() {
		super();
		
		// TODO Auto-generated constructor stub
		htmlControlMap=new LinkedList<HtmlControlMap>();
	}
	
	public void add(HtmlControlMap htmlControlMap){
		this.htmlControlMap.add(htmlControlMap);
		setHtmlControlNamber(getHtmlControlNamber()+1);
	}
	public PageZoneMap(String name, String label, int orientation, String  alternativeCSS, int width, int typeOfMeasure, boolean isRoot, LinkedList<HtmlControlMap> htmlControlMap) {
		super();
		this.name = name;
		this.label = label;
		this.orientation = orientation;
		this.alternativeCSS = alternativeCSS;
		this.width = width;
		this.typeOfMeasure = typeOfMeasure;
		this.isRoot = isRoot;
		this.htmlControlMap = htmlControlMap;
	}
	public boolean isValideModule(){
		boolean test=true;
		if((name.compareTo("")==0)||(label.compareTo("")==0)){
			test=false;
		}
		if(htmlControlMap==null){
			test=false;
		}
		else{
			if(htmlControlMap.size()<1){
				test=false;
			}
		}
		return test;
	}
	
	public int getHtmlControlNamber(){
		return htmlControlNamber;
	}
	
	public void setHtmlControlNamber(int htmlControlNamber){
		this.htmlControlNamber=htmlControlNamber;
	}
	
	public LinkedList<HtmlControlMap> getHtmlControlMap() {
		return htmlControlMap;
	}

	public void setHtmlControlMap(LinkedList<HtmlControlMap> htmlControlMap) {
		this.htmlControlMap = htmlControlMap;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name =name;
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public int getTypeOfMeasure() {
		return typeOfMeasure;
	}

	public void setTypeOfMeasure(int typeOfMeasure) {
		this.typeOfMeasure = typeOfMeasure;
	}

	public Integer getWidth() {
		return this.width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return this.height;
	}

	public void setHeight(Integer height) {
		
		this.height = height;
	}
	public String getAlternativeCSS(){
		return this.alternativeCSS;
	}
	public void setAlternativeCSS(String alternativeCSS) {
		this.alternativeCSS = alternativeCSS;
	}

	public boolean getBorder() {
		return border;
	}

	public void setBorder(boolean border) {
		this.border = border;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator =separator;
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
	public String getCustomFormValidation() {
		return customFormValidation;
	}
	public void setCustomFormValidation(String customFormValidation) {
		this.customFormValidation = customFormValidation;
	}
	
}
