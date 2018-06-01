package ro.cst.tsearch.test.webservices;

import ro.cst.tsearch.utils.FileUtils;
import com.stewart.orderproduction.ATS.AtsToGateway.AtsToGatewayStub;

public class EcorGatewayClientTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		AtsToGatewayStub  stub =  new AtsToGatewayStub("http://ordertest.stewart.com/ecg/ats/atstogateway.asmx");
		int timeout = 3 * 60 * 1000;
		stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout * 1000);
		
		AtsToGatewayStub.Submit method = new AtsToGatewayStub.Submit();
		method.setAgentID("AFWATSDB");
		method.setUserID("test");
		method.setPassword("password");
		String data = FileUtils.readXMLFile("c:/input.xml");
		data = data.replaceFirst("(?i)(<[0-9a-z]+>)", "$1<searchId>" + 2021 + "</searchId>");
		data = data.replaceAll("(?i)<\\?xml.*?\\?>","");
		// add another XML layer
		data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ATS2AIMfields><atsFile>" + 
				data + 
				"</atsFile></ATS2AIMfields>";
		method.setData(data);
		
		AtsToGatewayStub.SubmitResponse res = stub.submit(method);
		FileUtils.writeTextFile("c:/output.xml", res.getSubmitResult());
	}

}
