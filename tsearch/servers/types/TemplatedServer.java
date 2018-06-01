package ro.cst.tsearch.servers.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;

enum ErrorTypes {
	/**
	 *Used to define an error thats from the source server.
	 */
	SERVER_ERROR,
	/**
	 *Used for the message from the server that has no results.
	 */
	NO_RESULTS_ERROR,
	/**
	 * Server failed to return a valid response
	 */
	NO_RESPONSE,
	/**
	 * The response is ok.
	 */
	NO_ERROR,
	/**
	 * The search has to be refined in order to return results.
	 */
	TOO_MANY_RESULTS_ERROR
}

class ServerErrorMessage {
	private ErrorTypes errorType;
	private Set<String> serverErrorMessages = new LinkedHashSet<String>();
	private Set<String> noResultsMessages = new LinkedHashSet<String>();
	private Set<String> noResponseMessages = new LinkedHashSet<String>();
	private Set<String> tooManyResultsMessages = new LinkedHashSet<String>();

	public Set<String> getServerErrorMessages() {
		return serverErrorMessages;
	}

	public void addServerErrorMessage(String serverErrorMessages) {
		this.serverErrorMessages.add(serverErrorMessages);
	}

	public Set<String> getNoResultsMessages() {
		return noResultsMessages;
	}

	public void addNoResultsMessages(String noResultsMessages) {
		this.noResultsMessages.add(noResultsMessages);
	}

	public Set<String> getNoResponseMessages() {
		return noResponseMessages;
	}

	public void addTooManyResultsMessages(String noResultsMessages) {
		this.tooManyResultsMessages.add(noResultsMessages);
	}

	public Set<String> getTooManyMessages() {
		return tooManyResultsMessages;
	}

	public void addNoResponseMessages(String noResponseMessages) {
		this.noResponseMessages.add(noResponseMessages);
	}

	private Map<ErrorTypes, HashMap<String, String>> errorMap = new HashMap<ErrorTypes, HashMap<String, String>>();

	public ErrorTypes getErrorType(String serverResponse) {
		ErrorTypes result = ErrorTypes.NO_ERROR;
		if (findMessageListInString(serverResponse, serverErrorMessages)) {
			result = ErrorTypes.SERVER_ERROR;
		}
		if (findMessageListInString(serverResponse, noResponseMessages)) {
			result = ErrorTypes.NO_RESPONSE;
		}
		if (findMessageListInString(serverResponse, noResultsMessages)) {
			result = ErrorTypes.NO_RESULTS_ERROR;
		}
		if (findMessageListInString(serverResponse, tooManyResultsMessages)) {
			result = ErrorTypes.TOO_MANY_RESULTS_ERROR;
		}
		return result;
	}

	private boolean findMessageListInString(String serverResponse, Set<String> messageList) {
		for (String message : messageList) {
			if (serverResponse.contains(message)) {
				return true;
			}
		}
		return false;
	}

	// public void addServerError(ErrorTypes errorType, String serverMessage,
	// String atsMessage) {
	// if (errorMap.containsKey(errorType)) {
	// HashMap<String, String> currentErrorType = errorMap.get(errorType);
	// currentErrorType.put(serverMessage, atsMessage);
	// } else {
	// HashMap<String, String> newErrorMessage = new HashMap<String, String>();
	// newErrorMessage.put(serverMessage, atsMessage);
	// errorMap.put(errorType, newErrorMessage);
	// }
	// }

	// public String getErrorType(String serverMessage){
	// }
	//	
	// public List<Str> getServerErrorValues(ErrorTypes errorType){
	// }

}

/**
 * 
 * @author l
 * 
 *         Extend this class. Use this constructor {@link TemplatedServer#TemplatedServer(String, String, String, String, long, int)}. In the
 *         constructor call:
 * 
 *         {@link #setIntermediaryCases(int[] intermediary_cases)}, in this
 *         array the module indexes from ParentSiteEditor is maintained, in
 *         order to identify if the request is for an intermediary result.
 *         {@link #setDetailsCases(int[] intermediary_cases)}, set the module
 *         indexes that return a detail page.
 *         {@link #setIntermediaryMessage(String )}, the message which
 *         identifies if the result is an intermediary one.
 *         {@link #setDETAIL_MESSAGE(String )}, the string which identifies if
 *         the result is a DETAIL page.
 * 
 * 
 */
public class TemplatedServer extends TSServer {

	private static final long serialVersionUID = 1L;
	
	public TemplatedServer(long searchId) {
		super(searchId);
		init();
	}

	public TemplatedServer(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		init();
	}

	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	private final static ServerErrorMessage errorMessages = new ServerErrorMessage();

	private int[] INTERMEDIARY_CASES;

	private static int[] DETAILS_CASES = new int[] { ID_DETAILS };

	private static int[] LINK_CASES = new int[] { ID_GET_LINK };

	private static int[] SAVE_CASES = new int[] { ID_SAVE_TO_TSD };

	public int[] getINTERMEDIARY_CASES() {
		return INTERMEDIARY_CASES;
	}

	/**
	 * Sets the possible intermediary cases for this site. Always add
	 * {@link TSServer} @see ID_INTERMEDIARY.
	 * 
	 * @param intermediary_cases
	 */
	public void setIntermediaryCases(int[] intermediary_cases) {
		INTERMEDIARY_CASES = intermediary_cases;
	}

	public int[] getDETAILS_CASES() {
		return DETAILS_CASES;
	}

	public void setDetailsCases(int[] details_cases) {
		DETAILS_CASES = details_cases;
	}

	public int[] getLINK_CASES() {
		return LINK_CASES;
	}

	public void setLINK_CASES(int[] link_cases) {
		LINK_CASES = link_cases;
	}

	public int[] getSAVE_CASES() {
		return SAVE_CASES;
	}

	public void setSAVE_CASES(int[] save_cases) {
		SAVE_CASES = save_cases;
	}

	public void init() {
		setMessages();
	}

	boolean downloadingForSave = false;


	private String[] DETAILS_MESSAGES = new String[1];

	public String getDetailMessage() {
		return DETAILS_MESSAGES[0];
	}
	
	public String[] getDetailsMessages() {
		return DETAILS_MESSAGES;
	}

	public void setDetailsMessage(String details_message) {
		DETAILS_MESSAGES[0] = details_message;
	}
	
	public void setDetailsMessages(String [] details_message) {
		DETAILS_MESSAGES = details_message;
	}

	private static String[] INTERMEDIARY_MESSAGE = new String[1];
	
	public static CharSequence getIntermediaryMessage() {
		return INTERMEDIARY_MESSAGE[0];
	}
	
	public static String[] getIntermediaryMessages() {
		return INTERMEDIARY_MESSAGE;
	}

	public static void setIntermediaryMessage(CharSequence intermediary_message) {
		if (INTERMEDIARY_MESSAGE == null || INTERMEDIARY_MESSAGE.length <1){
			INTERMEDIARY_MESSAGE = new String[1];
		}
		INTERMEDIARY_MESSAGE[0] = (String) intermediary_message;
	}
	
	public static void setIntermermediaryMessages(CharSequence [] intermediary_message) {
		INTERMEDIARY_MESSAGE = (String[]) intermediary_message;
	}

	
	public MessageFormat createPartialLinkFormat() {
		String partialLink = CreatePartialLink(Integer.MAX_VALUE);
		String replace = partialLink.replace("ActionType="+Integer.MAX_VALUE , "ActionType={0}");
		MessageFormat messageFormat = new MessageFormat(replace);
		return messageFormat;
	}
	
	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {

		// step 1 - check to see if there are any errors
		boolean isError = isError(response);
		if (!isError) {
			String serverResult = response.getResult();

			if (isInArray(viParseID, INTERMEDIARY_CASES) != Integer.MIN_VALUE) {
				try{
					serverResult = clean(serverResult);
				}catch (Exception e){
					e.printStackTrace();
				}
				response.setResult(serverResult);
				intermediary(response, serverResult);
			}

			if (isInArray(viParseID, SAVE_CASES) != Integer.MIN_VALUE || isInArray(viParseID, DETAILS_CASES) != Integer.MIN_VALUE) {
				CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
				Search search = currentInstance.getCrtSearchContext();
				if (isInArray(viParseID, DETAILS_CASES) != Integer.MIN_VALUE || search.getSearchType()==Search.AUTOMATIC_SEARCH){
					try{
						serverResult = cleanDetails(serverResult);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				
				serverResult = addInfoToDetailsPage(response,serverResult, viParseID);
				response.setResult(serverResult);
				//test the line below is for testing purposes; it should be commented when it's committed
				if(Utils.isJvmArgumentTrue("debugForATSProgrammer")){
//					parseAndFillResultMap(response, serverResult, new ResultMap());
				}

				String accountNumber = getAccountNumber(serverResult);
				String filename = accountNumber + ".html";
				if (isInArray(viParseID, DETAILS_CASES) != Integer.MIN_VALUE) {
					serverResult = detailsCasesParse(action, response, viParseID, serverResult, accountNumber);
				}else {
					saveCasesParse(response, serverResult, filename);
				}
			}
			
			if (isInArray(viParseID, getLINK_CASES()) != Integer.MIN_VALUE) {
				if (checkIfResultContainsMessage(serverResult,INTERMEDIARY_MESSAGE)){
					ParseResponse(action, response, ID_INTERMEDIARY);
				}else if (checkIfResultContainsMessage(serverResult, DETAILS_MESSAGES)) {
					ParseResponse(action, response, ID_DETAILS);
				}
			} 
		}
	}

	protected final boolean checkIfResultContainsMessage(String serverResult, String [] messageArray) {
		boolean contains =false;
		for  (int i=0; i< messageArray.length && !contains; i++  ) {
			 contains = serverResult.contains(messageArray[i]);	
		}
		return contains;
	}

	protected void saveCasesParse(ServerResponse response, String serverResult, String filename) {
		smartParseDetails(response, serverResult);
		msSaveToTSDFileName = filename;
		response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		response.getParsedResponse().setResponse(serverResult);

		msSaveToTSDResponce = serverResult + CreateFileAlreadyInTSD();
	}

	protected String detailsCasesParse(String action, ServerResponse response, int viParseID, String serverResult, String accountNumber) {
		String originalLink = setOriginalLink(action, response);
		String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

		HashMap<String, String> data = putAdditionalData(serverResult);
		
		if (isInstrumentSaved(accountNumber, null, data)) {
			serverResult += CreateFileAlreadyInTSD();
		} else {
			mSearch.addInMemoryDoc(sSave2TSDLink, serverResult);
			serverResult = addSaveToTsdButton(serverResult, sSave2TSDLink, viParseID);
		}
		
//					if (serverResult.contains(INTERMEDIARY_MESSAGE)) {
			response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
			response.getParsedResponse().setResponse(serverResult);
//					} 
		
//					viParseID = ID_INTERMEDIARY;
		return serverResult;
	}

	protected String setOriginalLink(String action, ServerResponse response) {
		return action + "&" + response.getRawQuerry();
	}

	/**
	 * 
	 * @param serverResult 
	 * @return
	 */
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		return null;
	}

	/**
	 * This method needs to be implemented and should return a unique id that
	 * should identify a document. (e.g. intrument number, parcel id, etc)
	 * 
	 * @param serverResult
	 * @return
	 */
	protected String getAccountNumber(String serverResult) {
		throw new RuntimeException("Implement me!!!!");
	}
	
	protected void setMessages() {
		throw new RuntimeException("Must implement!!!");
	}


	/**
	 * Method that deals with the basic procedure that involves the construction
	 * of a ParsedResponse object with multiple results. This should not be
	 * implemented.
	 * 
	 * @param response
	 * @param serverResult
	 */
	protected void intermediary(ServerResponse response, String serverResult) {
		StringBuilder outputTable = new StringBuilder();
		Collection<ParsedResponse> intermediary = smartParseIntermediary(response, serverResult, outputTable);

		if (StringUtils.isEmpty(outputTable.toString())){
			outputTable.append(serverResult);
		}
		if (intermediary.size() > 0) {
			response.getParsedResponse().setResultRows(new Vector<ParsedResponse>(intermediary));
			response.getParsedResponse().setOnlyResponse(outputTable.toString());
			response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		}
	}

	/**
	 * Helper method that checks if a certain id is found in a specific
	 * String[].
	 * 
	 * @param viParseID
	 * @param intermediaryCases
	 * @return
	 */
	public int isInArray(int viParseID, int[] intermediaryCases) {
		if (!(intermediaryCases == null || intermediaryCases.length == 0)){
			for (int i : intermediaryCases) {
				if (viParseID == i)
					return i;
			}
		}
		return Integer.MIN_VALUE;
	}

	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {
		return serverResult;
	}

	// error message from the server
	// no results found
	// no response
	/**
	 * This method checks the response for error messages. The error messages
	 * should be set with getErrorMessages and set accordingly to
	 * ServerErrorMessages class.
	 */
	public boolean isError(ServerResponse response) {
		String serverResult = response.getResult();
		ErrorTypes error = errorMessages.getErrorType(serverResult);

		if (error == ErrorTypes.SERVER_ERROR) {
			response.setError(serverResult);
		}
		if (error == ErrorTypes.NO_RESULTS_ERROR) {// data not found
			response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
		}
		if (error == ErrorTypes.TOO_MANY_RESULTS_ERROR) {
			response.getParsedResponse().setError("<font color=\"red\">Refine the search. Too many results.</font>");
		}

		return (error != ErrorTypes.NO_ERROR);
	}

	/**
	 * Helps set the error messages in setMessages method.
	 * 
	 * @return
	 */
	public static ServerErrorMessage getErrorMessages() {
		return errorMessages;
	}

	/**
	 * Cleans the intermediary response.
	 * 
	 * @param response
	 * @return
	 */
	protected String clean(String response) {
		return response;
	}

	/**
	 * Cleans the details response.
	 * 
	 * @param response
	 * @return
	 */
	protected String cleanDetails(String response) {
		return response;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		return super.parseAndFillResultMap(response, detailsHtml, map);
	}
	
	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String pin = "" + map.get("PropertyIdentificationSet.ParcelID");
			String text = pin + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";
			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "address.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.LegalDescriptionOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);

			text = pin + "\r\n" + map.get("PropertyIdentificationSet.NameOnServer") + "\r\n\r\n\r\n";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name.txt", text);
			// end test

		}
	}
	
	@Override
	public boolean lastAnalysisBeforeSaving(ServerResponse serverResponse){
		
		if (this.isAssessorOrTaxServer() && numberOfYearsAllowed > 1){
			List<Integer> listWithYears = new ArrayList<Integer>();
			
			Vector<ParsedResponse> rows = serverResponse.getParsedResponse().getResultRows();
			for (ParsedResponse parsedResponse : rows) {
				int year = parsedResponse.getTaxYear();
				listWithYears.add(year);
			}
			
			if (listWithYears.size() > 0){
				Set<Integer> checkForDuplicates = new HashSet<Integer>(listWithYears);
				if (listWithYears.size() > checkForDuplicates.size()){
					return true;
				}
			}
		}
		
		return false;
	}
	
}