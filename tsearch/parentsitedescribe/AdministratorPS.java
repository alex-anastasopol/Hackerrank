package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;

public class AdministratorPS {
	//parola pt a nu se accesa
public void loadCache(){
	//citeste tote fisierele si le scrie in cache
//	private DSMXMLReader DSM=new ServerInfoDSMMap();
}
public void deleteObject(){
	//sterge un obiect DSM din cache 
}

public void  deleteAllObject(){
	//sterge toate obiectele DSM
}
public LinkedList<String>  listCache(){
	
	return DSMXMLReader.getListCache();
	//scrie intr o lista toate numele sisierelor care au fost scrise in cache
}
}
