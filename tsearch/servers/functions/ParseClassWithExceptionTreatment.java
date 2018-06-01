package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class ParseClassWithExceptionTreatment extends ForwardingParseClass {

	public ParseClassWithExceptionTreatment(ParseInterface parser) {
		super(parser);
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {
		try {
			super.parseAddress(address, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}
	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		try {
			siteParser.parseLegalDescription(legalDescription, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		try {
			siteParser.parseName(name, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setSecTwnRng(String text, ResultMap resultMap) {
		try {
			siteParser.setSecTwnRng(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		try {
			siteParser.setTaxData(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setAppraisalData(String text, ResultMap resultMap) {
		try {
			siteParser.setAppraisalData(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setLot(String text, ResultMap resultMap) {
		try {
			siteParser.setLot(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		try {
			siteParser.setBlock(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setTract(String text, ResultMap resultMap) {
		try {
			siteParser.setTract(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setUnit(String text, ResultMap resultMap) {
		try {
			siteParser.setUnit(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setPhase(String text, ResultMap resultMap) {
		try {
			siteParser.setPhase(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public void setCrossRefSet(String text, ResultMap resultMap) {
		try {
			siteParser.setCrossRefSet(text, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> parseIntermediary = new Vector<ParsedResponse>();
		try {
			parseIntermediary = siteParser.parseIntermediary(serverResponse, response, searchId, format);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, null, "address");
			parseException.printStackTrace();
		}
		return parseIntermediary;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		try {
			siteParser.parseDetails(response, searchId, resultMap);
		} catch (Exception e) {
			ParseException parseException = new ParseException(e, resultMap, "address");
			parseException.printStackTrace();
		}

	}

	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
			String text = pin + "\r\n" + map.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName()) + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			if (StringUtils.isNotEmpty((String) map.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName()))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);
			}

			text = pin + "\r\n" + map.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName()) + "\r\n\r\n\r\n";
			if (StringUtils.isNotEmpty((String) map.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName()))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);
			}

			text = pin + "\r\n" + map.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName()) + "\r\n\r\n\r\n";
			if (StringUtils.isNotEmpty((String) map.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName()))){
				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			}	
			// end test

		}
	}

}
