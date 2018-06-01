package ro.cst.tsearch.servers.functions;

import ro.cst.tsearch.extractor.xml.ResultMap;

public interface ParseDataInterface {
	void parseLegalDescription(String legalDescription, ResultMap resultMap);
	void parseAddress(String address, ResultMap resultMap);
	void parseName(String name, ResultMap resultMap);
	void setSecTwnRng(String text, ResultMap resultMap);
	void setTaxData(String text, ResultMap resultMap);
	void setAppraisalData(String text, ResultMap resultMap);
	void setLot(String text, ResultMap resultMap);
	void setBlock(String text, ResultMap resultMap);
	void setTract(String text, ResultMap resultMap);
	void setUnit(String text, ResultMap resultMap);
	void setPhase(String text, ResultMap resultMap);
	void setCrossRefSet(String text, ResultMap resultMap);
	
	
	
}
