package ro.cst.tsearch.search.name;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import ro.cst.tsearch.parentsitedescribe.DSMXMLReader;
import ro.cst.tsearch.search.name.companyex.CompanyNameMarshal;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class CompanyMarshal {
	public static String filePath="";
	public CompanyMarshal(){
		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		filePath=BaseServlet.REAL_PATH+rbc.getString("exception.companyname.path").trim()+"companyNameException.xml";
	}
	public CompanyNameMarshal unmarshal(){
		JAXBContext jc = null;
		try {
			jc = JAXBContext.newInstance(CompanyNameMarshal.class);
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}
		
		Unmarshaller u=null;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}
		
		try {
			System.out.println("file------------------------  "+filePath);
			return  (CompanyNameMarshal)u.unmarshal(  new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}
	public String marshal(CompanyNameMarshal comp){
		
		Writer wr= new StringWriter() ;
		JAXBContext jaxbContext = null;
		Marshaller marshaller=null;
		try {
			jaxbContext = JAXBContext.newInstance(CompanyNameMarshal.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		try {
			marshaller = jaxbContext.createMarshaller();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		try {
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		} catch (PropertyException e) {
			e.printStackTrace();
		}
		
		try {
			comp.sort();
			marshaller.marshal(comp,wr);
			
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		if(wr==null){
			return "";
		}
		System.out.println("file------------------------  "+filePath);
		StringUtils.toFile(CompanyMarshal.filePath,wr.toString());
		return wr.toString();
	}
}
