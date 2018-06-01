package ro.cst.tsearch.test.webservices;

import java.rmi.RemoteException;

import com.advantagetitlesearch.ats.title_search.services.ats.AddUserResponse;
import com.advantagetitlesearch.ats.title_search.services.ats.PlaceOrderResponse;

import localhost.title_search.services.Ats.AtsServiceStub;

import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.webservices.AddUser;
import ro.cst.tsearch.webservices.PlaceOrder;

public class TestAtsWebService {
	
	/**
	 * @param args
	 * @throws RemoteException 
	 */
	public static void main(String[] args) throws RemoteException   {
		
		//String fileName = "d:/Work/xml_orders/order.xml";
		//String outputFileName = "d:/Work/xml_orders/status.xml";
		//String link = "http://alpha.advantagetitlesearch.com:9000/title-search/services/Ats";
		//String link = "http://atsdev.advantagetitlesearch.com:9000/title-search/services/Ats";
		//String link = "http://ats01.advantagetitlesearch.com:9000/title-search/services/Ats";
		String link = "http://ats04.advantagetitlesearch.com:9000/title-search/services/Ats";		
		//String link = "http://ats.advantagetitlesearch.com/title-search/loginDispacher?aid=500";
		//String link = "http://atsdev.advantagetitlesearch.com/title-search/loginDispacher?aid=500";
		//String link = "http://atsdev.advantagetitlesearch.com/title-search/loginDispacher?aid=500";
		//Ats ats = new AtsServiceLocator(link).getAts();
		
		//String appId = 		"test";
		//String userName = 	"afwuser";
		//String password = 	"or68tew3da13";
		
		String appId = 		"TitleDesk";
		String userName = 	"Chudson";
		String password = 	"chudson";
		
		//String folder = "d:/work/xml_orders/wayne/wayne_";
		//String folder = "d:/work/xml_orders/macomb/mac_";
		//String folder = "d:/work/xml_orders/bay/bay_";
		//String folder = "d:/work/xml_orders/adduser_";
		
		//for(int i: new int[]{3,11,12,13,14,16,17,18,20}){
		//for(int i=1; i<=20; i++){
		//for(int i: new int[]{3}){
		
		//String inputFileName  = "d:/sample_order.xml" ;
		//String outputFileName = "d:/sample_order.output.xml";
		String inputFileName  = "e:/addUser.xml" ;
		String outputFileName = "e:/addUser.output.xml";
		
		String order = FileUtils.readXMLFile(inputFileName);
		
		
		AtsServiceStub stub = new AtsServiceStub(link);
		stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(60 * 1000 * 2);
		
		PlaceOrder method = new PlaceOrder();
		method.setAppId(appId);
		method.setUserName(userName);
		method.setPassword(password);
		method.setOrder(order);
		PlaceOrderResponse res = stub.placeOrder(method);
		String result = res.getPlaceOrderReturn();
		System.err.println(result);
		
		AddUser method1= new AddUser();
		method.setAppId(appId);
		method.setUserName(userName);
		method.setPassword(password);
		method.setOrder(order);
		AddUserResponse res1 = stub.addUser(method1);
		String result1 = res1.getAddUserReturn();
		
		System.err.println(result1);
		FileUtils.writeTextFile(outputFileName, result1);
			
	}
}
