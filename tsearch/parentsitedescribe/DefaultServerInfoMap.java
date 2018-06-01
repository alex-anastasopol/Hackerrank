package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;




public class DefaultServerInfoMap implements Cloneable{
	
	private int 		moduleNumber = 0;
	private String 	infoServerAdress = "";
	private String 	infoServerIp = "";
	private String 	infoRemoteAdress = "";
	private String 	infoRemoteIp = "";

	private String 	serverAdress = "";
	private String 	serverIp = "";
	private String 	serverLink = "";
	private String   genericSite="";
	private LinkedList <ModuleXMLMap> modules;
		
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
	public DefaultServerInfoMap toEscape(){
		DefaultServerInfoMap tmp=this;
		tmp.setGenericSite(escapeString(tmp.getGenericSite()));
		tmp.setServerAdress(escapeString(tmp.getServerAdress()));
		//tmp.setInfoServerIp(escapeString(tmp.getServerIp()));
		tmp.setServerLink(escapeString(tmp.getServerLink()));
		tmp.setServerIp(escapeString(tmp.getServerIp()));
		 LinkedList <ModuleXMLMap> mod=tmp.modules;
		 tmp.modules=new LinkedList <ModuleXMLMap>();
		for(int i=0;i<mod.size();i++){
			tmp.modules.add(mod.get(i).toEscape());
		}
		return tmp;
	}

	
	public DefaultServerInfoMap(int moduleNumber, String serverAdress, String serverIp, String serverLink, LinkedList<ModuleXMLMap> modules) {
		super();
		this.moduleNumber = moduleNumber;
		this.serverAdress = serverAdress;
		this.serverIp = serverIp;
		this.serverLink = serverLink;
		this.modules = modules;
	}
	public   DefaultServerInfoMap cloneCopy(){
		try {
			return(DefaultServerInfoMap) this.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	return null;
	}
	public void replace(DefaultServerInfoMap ser){
		this.setGenericSite("");
		this.setServerAdress(ser.getServerAdress());
		this.setServerIp(ser.getServerIp());
		this.setServerLink(ser.getServerLink());
		for(int i=0;i<ser.modules.size();i++){
			ModuleXMLMap mod=new ModuleXMLMap();
			mod.replace(ser.getOneModule(i));
			this.addModule(mod);
			mod=null;
		}
	}
	public DefaultServerInfoMap mergeModulue(DefaultServerInfoMap conv1){
		DefaultServerInfoMap generic =new DefaultServerInfoMap();
		DefaultServerInfoMap conv =new DefaultServerInfoMap();
		generic.replace(conv1);
		
		conv.replace(this);
		
		if(conv!=null){
				if("".equalsIgnoreCase(conv.getServerAdress())){
					conv.setServerAdress(generic.getServerAdress());
				}
				if("".equalsIgnoreCase(conv.getServerIp())){
					conv.setServerIp(generic.getServerIp());
				}
				if("".equalsIgnoreCase(conv.getServerLink())){
					conv.setServerLink(generic.getServerLink());
				}
				if(generic.getModuleNumber()>0){
					ModuleXMLMap modul1=null;
					ModuleXMLMap modul2=null;
					
					FunctionMap functionMap1=null;
					PageZoneMap zoneMap1=null;
					FunctionMap functionMap2=null;
					PageZoneMap zoneMap2=null;

					for(int i=0;i<conv.getModuleNumber();i++){
						modul1=conv.getOneModule(i);
						modul2=null;
						for(int j=0;j<generic.getModuleNumber();j++){
							if(modul1.getFunctionMap().getModuleIndex()==generic.getOneModule(j).getFunction().getModuleIndex()){
								modul2=generic.getOneModule(j);
								generic.removeModule(i);
							}
						}
						if(modul2!=null){
							functionMap1=modul1.getFunction();
							zoneMap1=modul1.getZoneMap();
							functionMap2=modul2.getFunction();
							zoneMap2=modul2.getZoneMap();
							functionMap1.setFunction(functionMap2);
							zoneMap1.setZone(zoneMap2);
									
						}
					}
					for(int i=0;i<generic.getModules().size();i++){
						conv.addModule(generic.getOneModule(i));
					}
				}
		}
		return conv;
		
	}
	
	public DefaultServerInfoMap() {
		super();
		modules=new LinkedList<ModuleXMLMap>();
	}
	
	public LinkedList<ModuleXMLMap> getModules() {
		return modules;
	}
	
	public ModuleXMLMap getOneModule(int index) {
		return modules.get(index);
	}
	
	public void setModules(LinkedList<ModuleXMLMap> modules) {
		this.modules = modules;
	}
	
	public void setOneModule(ModuleXMLMap modules) {
		this.modules.addLast( modules);
	}
	
	public String getServerAdress() {
		return serverAdress;
	}
	
	public void setServerAdress(String serverAdress) {
		
		this.serverAdress = serverAdress;
	}
	
	
	public int getModuleNumber() {
		return moduleNumber;
	}
	

	public void setModuleNumber(int moduleNumber) {
		this.moduleNumber = moduleNumber;
	}

	public String getServerIp() {
		return serverIp;
	}
	
	public void setServerIp(String serverIp) {
			this.serverIp = serverIp;
	}
	
	public String getServerLink() {
		return serverLink;
	}
	
	public void setServerLink(String serverLink) {
		this.serverLink = serverLink;
	}

	
	//adaugare modul
	public void addModule(ModuleXMLMap modul){
		this.modules.add(modul);
		++moduleNumber;
	}
	
	//stergere modul
	public Object removeModule(int index){
		--moduleNumber;
		return this.modules.remove(index);	
		
	}
	//stergere dupa continut
	/*
	public Object removeModule(String name){
	int i,j=-1;
	for(i=0;i<this.modules.size();i++){
		if(name.compareTo(this.modules.get(i).zoneMap.name)==0){
			j=i;
		}
	}
	if(j>=0){
		return removeModule(j);
		}
	else{
		return null;
		}
	}*/
	public boolean isValideModule(){
		boolean test=true;
		if((this. moduleNumber <1)||(this.serverAdress == null)||(this.serverIp == null)||(serverLink == null)||
				(this.serverAdress.compareTo("")==0)||(this.serverIp.compareTo("")==0)||(this.serverLink.compareTo("")==0)){
			test=false;
		}
		for(int i=0;i<this.moduleNumber;i++){
		if(modules.get(i).isValideModule()==false){
			test=false;
			}
		}
		return test;
	}
	/*
	public boolean equals(modules modul,int i){
		if(this.modules.get(i).fuctionMap.name.compareTo(modul.get()  ) )
		
	}*/

	public String getGenericSite() {
		return genericSite;
	}

	public void setGenericSite(String genericSite) {
		this.genericSite = genericSite;
	}
	public String getInfoRemoteAdress() {
		return infoRemoteAdress;
	}
	public void setInfoRemoteAdress(String infoRemoteAdress) {
		this.infoRemoteAdress = infoRemoteAdress;
	}
	public String getInfoRemoteIp() {
		return infoRemoteIp;
	}
	public void setInfoRemoteIp(String infoRemoteIp) {
		this.infoRemoteIp = infoRemoteIp;
	}
	public String getInfoServerAdress() {
		return infoServerAdress;
	}
	public void setInfoServerAdress(String infoServerAdress) {
		this.infoServerAdress = infoServerAdress;
	}
	public String getInfoServerIp() {
		return infoServerIp;
	}
	public void setInfoServerIp(String infoServerIp) {
		this.infoServerIp = infoServerIp;
	}

	//m
	
}