package ro.cst.tsearch.parentsitedescribe;


public class ModuleXMLMap {
	public ModuleXMLMap() {
		super();
		functionMap=new FunctionMap();
		zoneMap = new PageZoneMap();
	}

	//private boolean predefinedFunction=false;
	//private int functionDefined=0;
	private FunctionMap functionMap;
	private PageZoneMap zoneMap;
	//private String nameFunctinMap="";
	//private LinkedList<Param> functionDefinedMap=null;
	//public 
	public ModuleXMLMap toEscape(){
		ModuleXMLMap tmp=this;
		tmp.functionMap=tmp.functionMap.toEscape();
		tmp.zoneMap=tmp.zoneMap.toEscape();
		return tmp;
	}
	public String escapeString(String escape){

		escape=escape.replaceAll("&", "&amp;");
		escape=escape.replaceAll("\"", "&quot;");
		escape=escape.replaceAll("'", "&apos;");
		escape=escape.replaceAll("<", "&lt;");
		escape=escape.replaceAll(">", "&gt;");
	
		return escape;
	}
	public void replace(ModuleXMLMap mod){
		FunctionMap function= new FunctionMap();
		function.replace(mod.getFunction());
	
		PageZoneMap zone=new PageZoneMap();
		zone.replace(mod.getZoneMap());
		
		this.addFunction(function);
		this.addZone(zone);
		zone=null;
		function=null;
	}
	public void addFunction(FunctionMap functionMap){
		this.functionMap=functionMap;
	}
	public FunctionMap getFunction(){
		return this.functionMap;
	}
	public void  remove(FunctionMap functionMap){
		this.functionMap=null;
	}
	
	public void addZone(PageZoneMap zone1Map){
	

		this.zoneMap=zone1Map;
	}
	
	public void remove(PageZoneMap zoneMap){
		this.zoneMap=null;
	}
	
	/*public Param remove(int i){
		setFunctionDefined(getFunctionDefined()+1);
		return functionDefinedMap.remove(i);
		
	}
	public Param remove(Param pe){
		int j=-1;
		for(int i=0;i<functionDefinedMap.size();i++)
			{
			if(pe.equal(functionDefinedMap.get(i))){
				j=i;
			}
			}
		if(j>-1){
			remove(j);
			return pe;
		}
		else{
			return pe;
		}
	}
	*/
	public boolean isValideModule(){
	boolean test=true;
	if(zoneMap==null){
		test=false;
		}
	
	if(!zoneMap.isValideModule()){
		test=false;
	}
		
	return test;
	}




	public ModuleXMLMap( FunctionMap functionMap, PageZoneMap zoneMap) {
		super();
		//this.predefinedFunction = predefinedFunction;
		this.functionMap = functionMap;
		this.zoneMap = zoneMap;
//		this.functionDefinedMap = functionDefinedMap;
	}


/*

	public LinkedList<Param> getFunctionDefinedMap() {
		return functionDefinedMap;
	}




	public void setFunctionDefinedMap(LinkedList<Param> functionDefinedMap) {
		this.functionDefinedMap = functionDefinedMap;
	}
*/
	

	public FunctionMap getFunctionMap() {
		return functionMap;
	}


	public void setFunctionMap(FunctionMap functionMap) {
		this.functionMap = functionMap;
	}


/*

	public boolean isPredefinedFunction() {
		return predefinedFunction;
	}




	public void setPredefinedFunction(boolean predefinedFunction) {
		this.predefinedFunction = predefinedFunction;
	}

*/


	public PageZoneMap getZoneMap() {
		return zoneMap;
	}




	public void setZoneMap(PageZoneMap zoneMap) {
		this.zoneMap = zoneMap;
	}



/*
	public int getFunctionDefined() {
		return functionDefined;
	}




	public void setFunctionDefined(int functionDefined) {
		this.functionDefined = functionDefined;
	}
*/
}

