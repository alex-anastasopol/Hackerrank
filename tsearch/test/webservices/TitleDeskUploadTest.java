package ro.cst.tsearch.test.webservices;

import java.io.File;
import java.rmi.RemoteException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import com.stewart.titledesk.webservice.ATSReceiveStub;
import com.stewart.titledesk.webservice.ATSReceiveStub.ReceiveATSFileResponse;

public class TitleDeskUploadTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws  RemoteException {

		ATSReceiveStub  stub = new ATSReceiveStub("https://www.etitledesk.com/Longbow/webservices/atsReceive.asmx");
		ATSReceiveStub.ReceiveATSFile method = new ATSReceiveStub.ReceiveATSFile();
		
		stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(30 * 1000);
		
		method.setAppID("TitleDesk");
		method.setUsername("ATSuser");
		method.setPassword("[8FO6W~M,0");
		method.setOrderID("50074");
		method.setFile(new DataHandler(new FileDataSource(new File("c:\\request.xml"))));
		method.setFilename("request.xml");
		
		ReceiveATSFileResponse response = stub.receiveATSFile(method);	
		System.err.println(response.getReceiveATSFileResult());		
	
	}

}
