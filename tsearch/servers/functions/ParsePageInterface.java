package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.Vector;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

public interface ParsePageInterface {
	Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format);
	void parseDetails(String response, long searchId, ResultMap resultMap);
}
