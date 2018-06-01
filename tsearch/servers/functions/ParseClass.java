package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.stewart.ats.base.document.DocumentI;

import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

public class ParseClass implements ParseInterface{

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseDetails( String response, long searchId, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSecTwnRng(String name, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAppraisalData(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLot(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTract(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		// TODO Auto-generated method stub
		return null;
	}

	public String createPartialLink(MessageFormat format, int actionType){
		return MessageFormat.format(format.toPattern(), actionType);
	}

	
	protected void createDocument(long searchId, ParsedResponse currentResponse, ResultMap resultMap) {
		Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
		DocumentI document = null;
		try {
			document = bridge.importData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentResponse.setDocument(document);
	}
	
	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			if (StringUtils.isNotEmpty((String) map.get("PropertyIdentificationSet.AddressOnServer"))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);
			}

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.LegalDescriptionOnServer") + "\r\n\r\n\r\n";
			if (StringUtils.isNotEmpty((String) map.get("PropertyIdentificationSet.LegalDescriptionOnServer"))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);
			}

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			if (StringUtils.isNotEmpty((String) map.get("PropertyIdentificationSet.NameOnServer"))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			}	
			// end test

		}
	}

	@Override
	public void setUnit(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPhase(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCrossRefSet(String text, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}

	
}
