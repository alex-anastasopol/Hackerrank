package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;
import java.util.Vector;

public class FunctionMap {
	private int noParam=0;
	private int FunctionsCount=0;
	private int moduleIndex=0;
	private int moduleOrder=-1;
	private String searchType="";
	private String destinationPage="";
	private int destinationMethod=0;
	private String setName="";
	private int setParcelId=0;
	private boolean visible=false;
	private String visibleFor = ""; 
	private int moule=-1;
	private String setKey="NO_KEY";
	//private String NoParam="";
	//private String StreeParam="";
	private LinkedList<Param> functionDefinedMap=null;
	private LinkedList<Param> functionDefinedElseMap=null;
	
	public void replace(FunctionMap function){
		this.setNoParam(function.getNoParam() );
		this.setFunctionsCount(function.getFunctionsCount());
		this.setModuleIndex(function.getModuleIndex());
		this.setModuleOrder(function.getModuleOrder());
		this.setSearchType(function.getSearchType());
		this.setDestinationPage(function.getDestinationPage());
		this.setDestinationMethod(function.getDestinationMethod());
		this.setSetName(function.getSetName());
		this.setSetParcelId(function.getSetParcelId());
		this.setVisible(function.getVisible());
		this.setVisibleFor(function.getVisibleFor());
		this.setMoule(function.getMoule());
		this.setSetKey(function.getSetKey());
		this.functionDefinedMap=new LinkedList<Param>();
		this.functionDefinedElseMap=new LinkedList<Param>();
		for(int i=0;i<function.functionDefinedMap.size();i++){
			Param p1=new Param();
			p1.replace(function.functionDefinedMap.get(i));
			this.add(p1);
			p1=null;
		}
		for(int i=0;i<function.functionDefinedElseMap.size();i++){
			Param p1=new Param();
			p1.replace(function.functionDefinedElseMap.get(i));
			this.addElse(p1);
			p1=null;
		}

	}

	public FunctionMap toEscape() {
		FunctionMap tmp = this;
		tmp.destinationPage = tmp.destinationPage;
		tmp.setKey = escapeString(tmp.setKey);
		tmp.setName = escapeString(tmp.setName);
		LinkedList<Param> pElseMap = tmp.functionDefinedElseMap;
		LinkedList<Param> pMap = tmp.functionDefinedMap;
		tmp.functionDefinedElseMap = new LinkedList<Param>();
		tmp.functionDefinedMap = new LinkedList<Param>();
		for (int i = 0; i < pElseMap.size(); i++) {
			tmp.functionDefinedElseMap.add(pElseMap.get(i).toEscape());
		}
		for (int j = 0; j < pMap.size(); j++) {
			tmp.functionDefinedMap.add(pMap.get(j).toEscape());
		}
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
	public LinkedList<Param> getFunctionDefinedMap() {
		return functionDefinedMap;
	}
	public void setFunctionDefinedMap(LinkedList<Param> functionDefinedMap) {
		this.functionDefinedMap = functionDefinedMap;
	}
	
	public void add(Param para){
		
			functionDefinedMap.addLast(para);
			++noParam;
			//this.functionDefinedMap.add(param);
	}
	public LinkedList<Param> getFunctionDefinedElseMap() {
		return functionDefinedElseMap;
	}
	public void addElse(Param para){
		
		functionDefinedElseMap.addLast(para);
		//this.functionDefinedMap.add(param);
}
	public void add(LinkedList<Param> listParam){
		this.functionDefinedMap=listParam;
	}
	
	
	
public void setFunction(FunctionMap function){
	Param p1=null;
	Param p2=null;

		
		if("".equalsIgnoreCase(this.getDestinationPage())){
			this.setDestinationPage(function.getDestinationPage());
		}
		if(this.getDestinationMethod()>=0){
			this.setDestinationMethod(function.getDestinationMethod());
		}
		if(this.setParcelId>0){
			this.setSetParcelId(function.getSetParcelId());
		}
		if("NO_KEY".equalsIgnoreCase(this.getSetKey())){
			this.setSetKey(function.getSetKey());
		}
		//this.setVisible(function.getVisible());
		LinkedList<Param> listParam=function.getFunctionDefinedMap();
		int marime=this.getFunctionDefinedMap().size();
		boolean isParam=false;
		for(int i=0;i<listParam.size();i++){
			isParam=false;
			for(int j=0;j<marime;j++){
				if((listParam.get(i).getName().compareToIgnoreCase(this.getFunctionDefinedMap().get(j).getName())==0)&&(listParam.get(i).getValue().compareToIgnoreCase(this.getFunctionDefinedMap().get(j).getValue())==0)){
					isParam=true;
					//p1=listParam.get(i);
					//p2=this.getFunctionDefinedMap().get(j);
					//p2.replace(p1);
				}
				}
			if(!isParam){
				p1=listParam.get(i);
				p2=new Param();
				p2.replace(p1);
				this.add(p2);
				p1=null;
				p2=null;
			}
		}
		
 listParam=function.getFunctionDefinedElseMap();
		isParam=false;
		for(int i=0;i<listParam.size();i++){
			isParam=false;
			for(int j=0;j<this.getFunctionDefinedMap().size();j++){
				if((listParam.get(i).getName().compareToIgnoreCase(this.getFunctionDefinedElseMap().get(j).getName())==0)&&(listParam.get(i).getValue().compareToIgnoreCase(this.getFunctionDefinedElseMap().get(j).getValue())==0)){
					isParam=true;
					p1=null;
					p2=null;
		//			p1=listParam.get(i);
		//			p2=this.getFunctionDefinedElseMap().get(j);
		//			p2.replace(p1);
				}
				}
			if(!isParam){
				this.addElse(listParam.get(i));
			}
		}
		
		p1=null;
		p2=null;
		listParam=null;
	}
public void addElse(LinkedList<Param> listParam){
		
		this.functionDefinedElseMap=listParam;
		//this.functionDefinedMap.add(param);
}
	
	public Param remove(int index){
		--noParam;
		return this.functionDefinedMap.remove(index);
	}
	
	public void removeAll(){
		for (int i=0;i<this.functionDefinedMap.size();i++)
		{
		remove(0);
		}
		noParam=0;
	} 
	
	public Param remove(String name){
		int j=-1;
		for(int i=0;i<this.functionDefinedMap.size();i++){
			if(name.compareToIgnoreCase(this.functionDefinedMap.get(i).getName())==0){
				j=i;
			}
		}
		if(j>=0)
			{
			--noParam;
			return remove(j);
			}
		else{
			return null;
		}
	}
	
	public int getDestinationMethod() {
		return destinationMethod;
	}
	
	public void setDestinationMethod(int destinationMethod) {
		this.destinationMethod = destinationMethod;
	}
	
	public String getDestinationPage() {
		return destinationPage;
	}
	
	public void setDestinationPage(String destinationPage) {
		this.destinationPage = destinationPage;
	}
	
	public int getFunctionsCount() {
		this.FunctionsCount=this.functionDefinedMap.size()+1;
		return FunctionsCount;
	}
	
	public void setFunctionsCount(int functionsCount) {
		FunctionsCount = functionsCount;
	}
	
	public int getModuleIndex() {
		return moduleIndex;
	}
	
	public void setModuleIndex(int moduleIndex) {
		this.moduleIndex = moduleIndex;
	}
	
	/*
	public String getNoParam() {
		return NoParam;
	}
	public void setNoParam(String noParam) {
		NoParam = noParam;
	}
	public String getStreeParam() {
		return StreeParam;
	}
	public void setStreeParam(String streeParam) {
		StreeParam = streeParam;
	}*/
	
	public FunctionMap() {
		super();
		noParam=0;
		FunctionsCount=0;
		moduleIndex=0;
		String destinationPage="";
		destinationMethod=0;
		setName="";
		setParcelId=0;
		visible=true;
		moule=-1;
		setKey="NO_KEY";
		
		functionDefinedMap=new LinkedList<Param>();
		functionDefinedElseMap=new LinkedList<Param>();
		// TODO Auto-generated constructor stub
	}
	
	public FunctionMap(int functionsCount, int moduleIndex, String destinationPage, int destinationMethod, LinkedList<Param> param) {
		super();
		this.FunctionsCount = functionsCount;
		this.moduleIndex = moduleIndex;
		this.destinationPage = destinationPage;
		this.destinationMethod = destinationMethod;
		this.functionDefinedMap=param;
	}
	public String getSetKey() {
		return setKey;
	}
	public void setSetKey(String setKey) {
		this.setKey =setKey;
	}
	public String getSetName() {
		return setName;
	}
	public void setSetName(String setName) {
		this.setName = setName;
	}
	public int getSetParcelId() {
		return setParcelId;
	}
	public void setSetParcelId(int setParcelId) {
		this.setParcelId = setParcelId;
	}
	public int getNoParam() {
		return noParam;
	}
	public void setNoParam(int noParam) {
		this.noParam = noParam;
	}
	public int getMoule() {
		return moule;
	}
	public void setMoule(int moule) {
		this.moule = moule;
	}
	public boolean getVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/**
	 * sets a module to be visible only for some categories of users
	 */
	public void setVisibleFor(String visibleFor) {
		this.visibleFor = visibleFor;
	}
	
	public String getVisibleFor() {
		return visibleFor;
	}
	
	public int getModuleOrder() {
		return moduleOrder;
	}
	
	public void setModuleOrder(int moduleOrder) {
		this.moduleOrder = moduleOrder;
	}
	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}
	public String getSearchType() {
		return searchType;
	}

}
