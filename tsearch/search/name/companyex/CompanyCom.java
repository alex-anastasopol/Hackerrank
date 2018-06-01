package ro.cst.tsearch.search.name.companyex;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;

import ro.cst.tsearch.utils.StringUtils;

@XmlRootElement(name = "CompanyCom") 
public class CompanyCom {
private String status;
private String command;
private CompanyObject companyObject;
public String getCommand() {
	return command;
}
public void setCommand(String command) {
	this.command = command;
}
public CompanyObject getCompanyObject() {
	return companyObject;
}
public void setCompanyObject(CompanyObject companyObject) {
	this.companyObject = companyObject;
}
public CompanyCom() {
	super();
}

public static CompanyCom unmarshal(String fileContains){
	JAXBContext jc = null;
	try {
		jc = JAXBContext.newInstance(CompanyCom.class);
	} catch (JAXBException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	Unmarshaller u=null;
	try {
		u = jc.createUnmarshaller();
	} catch (JAXBException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	try {
		return  (CompanyCom)u.unmarshal( new StreamSource( new StringReader( fileContains) ) );
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null;
}

public static String marshal(CompanyCom comp){
	
	 Writer wr= new StringWriter() ;
	JAXBContext jaxbContext = null;
	Marshaller marshaller=null;
	try {
		jaxbContext = JAXBContext.newInstance(CompanyCom.class);
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		marshaller = jaxbContext.createMarshaller();
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	} catch (PropertyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	try {
		marshaller.marshal(comp,wr);
		
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	if(wr==null){
		return "";
	}
	//StringUtils.toFile("/opt/tmp.xml",wr.toString());
	return wr.toString();
}
public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}
}
