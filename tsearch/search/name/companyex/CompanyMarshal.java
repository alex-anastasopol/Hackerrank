package ro.cst.tsearch.search.name.companyex;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;

import ro.cst.tsearch.parentsitedescribe.DSMXMLReader;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class CompanyMarshal {
	public static String filePath;
	public CompanyMarshal(){
		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		filePath=BaseServlet.REAL_PATH+rbc.getString("exception.companyname.path").trim()+"companyNameException.xml";
		System.out.println(">>>>file path"+filePath);
	}
	public CompanyNameMarshal unmarshal(){
		JAXBContext jc = null;
		try {
			jc = JAXBContext.newInstance(CompanyNameMarshal.class);
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
			return  (CompanyNameMarshal)u.unmarshal(  new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
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
			comp.sort();
			marshaller.marshal(comp,wr);
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(wr==null){
			return "";
		}
		StringUtils.toFile(filePath,wr.toString());
		return wr.toString();
	}
}
