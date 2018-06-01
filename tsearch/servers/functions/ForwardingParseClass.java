package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.Vector;

import com.stewart.ats.base.document.DocumentI;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

public class ForwardingParseClass implements ParseInterface {

	ParseInterface siteParser;

	public ForwardingParseClass(ParseInterface parser) {
		siteParser = parser;
	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		siteParser.parseLegalDescription(legalDescription, resultMap);
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {
		siteParser.parseAddress(address, resultMap);
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		siteParser.parseName(name, resultMap);
	}

	@Override
	public void setSecTwnRng(String text, ResultMap resultMap) {
		siteParser.setSecTwnRng(text, resultMap);
	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		siteParser.setTaxData(text, resultMap);
	}

	@Override
	public void setAppraisalData(String text, ResultMap resultMap) {
		siteParser.setAppraisalData(text, resultMap);
	}

	@Override
	public void setLot(String text, ResultMap resultMap) {
		siteParser.setLot(text, resultMap);
	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		siteParser.setBlock(text, resultMap);
	}

	@Override
	public void setTract(String text, ResultMap resultMap) {
		siteParser.setTract(text, resultMap);
	}

	@Override
	public void setUnit(String text, ResultMap resultMap) {
		siteParser.setUnit(text, resultMap);
	}

	@Override
	public void setPhase(String text, ResultMap resultMap) {
		siteParser.setPhase(text, resultMap);
	}

	@Override
	public void setCrossRefSet(String text, ResultMap resultMap) {
		siteParser.setCrossRefSet(text, resultMap);
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> parseIntermediary = new Vector<ParsedResponse>();
		try {
			parseIntermediary = siteParser.parseIntermediary(serverResponse, response, searchId, format);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parseIntermediary;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		try {
			siteParser.parseDetails(response, searchId, resultMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public String createPartialLink(MessageFormat format, int actionType){
		return MessageFormat.format(format.toPattern(), actionType);
	}
}
