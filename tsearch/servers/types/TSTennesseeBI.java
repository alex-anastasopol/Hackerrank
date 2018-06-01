package ro.cst.tsearch.servers.types;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;

public class TSTennesseeBI extends TSServer
{
	protected static final Category logger= Logger.getLogger(TSTennesseeBI.class);
	
	static final long serialVersionUID = 10000000;
	private boolean mbIsHistory= false;
	private String msLastIndex= "";
	public TSServerInfo getDefaultServerInfo()
	{
		TSServerInfo msiServerInfoDefault= null;
		int i= 0;
		TSServerInfoModule simTmp= null;
		//
		if (msiServerInfoDefault == null)
		{
			//SET SERVER
			//number of search modules
			msiServerInfoDefault= new TSServerInfo(2);
			//set Address
			msiServerInfoDefault.setServerAddress("www.tennesseeanytime.org");
			//set link
			msiServerInfoDefault.setServerLink("http://www.tennesseeanytime.org/soscorp/");
			//set IP
			msiServerInfoDefault.setServerIP("68.152.41.42");
			{ //SET EACH SEARCH
				{ //Search by name /soscorp/sosprog
					simTmp= SetModuleSearchByName(4, msiServerInfoDefault, i++, "/soscorp/sosprog", TSConnectionURL.idPOST, "input_box", "");
					//
					simTmp.getFunction(0).setName("Business Name:");
					//
					simTmp.getFunction(1).setParamName("action");
					simTmp.getFunction(1).setDefaultValue("corp");
					simTmp.getFunction(1).setHiden(true);
					//
					simTmp.getFunction(2).setParamName("selection");
					simTmp.getFunction(2).setDefaultValue("portion");
					simTmp.getFunction(2).setHiden(true);
					//
					simTmp.getFunction(3).setParamName("Submit");
					simTmp.getFunction(3).setDefaultValue("Submit Search");
					simTmp.getFunction(3).setHiden(true);
					//
					simTmp.setReferer("http://www.tennesseeanytime.org/soscorp/");
				}
				{ //Search by parcel Control Number
					simTmp= SetModuleSearchByParcelNo(4, msiServerInfoDefault, i++, "/soscorp/sosprog", TSConnectionURL.idPOST, "input_box");
					//
					simTmp.setName("Secretary of State Control Number:");
					simTmp.getFunction(0).setName("Control Number:");
					//
					simTmp.getFunction(1).setParamName("action");
					simTmp.getFunction(1).setDefaultValue("corp");
					simTmp.getFunction(1).setHiden(true);
					//
					simTmp.getFunction(2).setParamName("selection");
					simTmp.getFunction(2).setDefaultValue("corpID");
					simTmp.getFunction(2).setHiden(true);
					//
					simTmp.getFunction(3).setParamName("Submit");
					simTmp.getFunction(3).setDefaultValue("Submit Search");
					simTmp.getFunction(3).setHiden(true);
					//
					simTmp.setReferer("http://www.tennesseeanytime.org/soscorp/");
				}
			}
			msiServerInfoDefault.setupParameterAliases();
		}
		return msiServerInfoDefault;
	}

	public TSTennesseeBI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
	{
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	public ServerResponse GetLink(String vsRequest,  boolean vbEncoded)throws ServerResponseException
	{
		
		String action= getParameter("action",vsRequest);
		if ((action) != null && action.equals("history"))
		{
			mbIsHistory= true;
		}
		else
		{
			msLastIndex= getParameter("index",vsRequest);
			mbIsHistory= false;
		}
		getTSConnection().SetReferer("http://www.tennesseeanytime.org/soscorp/results.jsp");
		return super.GetLink(vsRequest, vbEncoded);
	}
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException
	{
		String sTmp= "";
		String sTmp1= "";
		int iTmp, iTmp1;
		ServerResponse stmpResponse;
		String rsResponce= Response.getResult();
		switch (viParseID)
		{
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_PARCEL :
				sTmp= "<table width=\"99%\" border=\"0\" cellspacing=\"6\" cellpadding=\"3\" height=\"380\" align=\"center\"";
				sTmp1= "<div align=\"center\"><font size=\"2\" face=\"Arial, Helvetica, Verdana, sans-serif\">Note:";
				int startPos= 0, finishPos;
				if ((startPos= rsResponce.indexOf(sTmp)) > -1 && (finishPos= rsResponce.indexOf(sTmp1)) > -1)
				{
					//while we still have a property details link then get it's information
					rsResponce= rsResponce.substring(startPos, finishPos);
					iTmp= rsResponce.indexOf("<form ");
					StringBuffer sBuffer= new StringBuffer(rsResponce);
					String sForm;
					while (iTmp > -1)
					{
						//while we still have a property details form then convert it in link
						//get from form until the imput information ends
						iTmp1= sBuffer.indexOf("</form>", iTmp) + 7;
						sForm= sBuffer.substring(iTmp, iTmp1);
						//replace form with a link
						sBuffer.replace(iTmp, iTmp1, createLinkFromForm(sForm, "View"));
						iTmp= sBuffer.indexOf("<form ");
					}
					parser.Parse(Response.getParsedResponse(),sBuffer.toString(),Parser.NO_PARSE);
					
				}
				break;
			case ID_GET_LINK :
			case ID_SAVE_TO_TSD :
				if (!mbIsHistory)
				{
					sTmp= "<form name=\"form1\" method=\"post\" action=\"/soscorp/sosprog\">";
					sTmp1= "<td width=\"64%\"><font color=\"333333\"><font size=\"2\" face=\"Arial, Helvetica, Verdana, sans-serif\">Note:";
					if ((startPos= rsResponce.indexOf(sTmp)) > -1 && (finishPos= rsResponce.indexOf(sTmp1)) > -1)
					{
						rsResponce= rsResponce.substring(startPos, finishPos - 60);
						rsResponce += "</table>";
						rsResponce= rsResponce.replaceAll("<a href=\"", "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET));
						rsResponce= rsResponce.replaceAll("sosprog\\?action", "sosprog&action");
						if (viParseID != ID_SAVE_TO_TSD)
						{
							//add radiobox;
							rsResponce=
								"<input type=\"radio\" name=\""
									+ SAVE_TO_TSD_PARAM_NAME
									+ "\" value=\""
									+ ACTION_TYPE_LINK
									+ "=1&"
									+ msPrmNameLink
									+ "/soscorp/sosprog&Details=Details&action=detail&index="
									+ msLastIndex
									+ "\" Checked >"
									+ rsResponce;
							//add form
							rsResponce= CreateSaveToTSDFormHeader() + rsResponce;
							//end form
							rsResponce += "<hr>" + CreateSaveToTSDFormEnd(viParseID);
						}
						else
						{
							msSaveToTSDResponce= rsResponce + CreateFileAlreadyInTSD();
							sTmp= "";
							sTmp1= msServerID + "&";
							iTmp= rsResponce.indexOf(sTmp1);
							//while there are links on the page follow links
							while (iTmp != -1)
							{
								iTmp += sTmp1.length();
								//make request with the found link
								mbIsHistory= true;
								stmpResponse= FollowLink(rsResponce.substring(iTmp, rsResponce.indexOf("\"", iTmp)), null);
								sTmp += stmpResponse.getParsedResponse().getResponse();
								mbIsHistory= false;
								//add an hr
								sTmp += "<hr>";
								//go to next link
								iTmp= rsResponce.indexOf(sTmp1, iTmp);
							}
							//concat pages
							rsResponce= rsResponce + "<hr>" + sTmp;
							//remove all links
							rsResponce= rsResponce.replaceAll("<a href=[a-zA-Z0-9/=&,\\-_\\(\\)\\<\\>\\?\\.\\'\\s\\\"\\r\\n]+</a>", "");
						}
						//logger.debug("rsResponce = " + rsResponce);
						parser.Parse(Response.getParsedResponse(),rsResponce,Parser.NO_PARSE);
					}
				}
				else
				{
					sTmp= "<form name=\"form1\" method=\"post\" action=\"/soscorp/sosprog\">";
					sTmp1= "<input type=\"submit\" name=\"search\" value=\"Search Again\"";
					if ((startPos= rsResponce.indexOf(sTmp)) > -1 && (finishPos= rsResponce.indexOf(sTmp1)) > -1)
					{
						rsResponce= rsResponce.substring(startPos, finishPos - 100);
						rsResponce += "</table>";
						parser.Parse(Response.getParsedResponse(),rsResponce,Parser.NO_PARSE);
					}
				}
				break;
			default :
				break;
		}
	}
	/**
	 * @see TSInterface#NewSession()
	 */
	public void NewSession()
	{
		getTSConnection().SetCookie("");
		logger.info(toString() + " New Cookie");
	}
	/**
	 * @see TSInterface#SessionExpired()
	 */
	public boolean SessionExpired()
	{
		String cookie= getTSConnection().GetCookie();
		logger.info(toString() + " cookie  :  " + cookie);
		return (cookie.indexOf("JServSessionIdroot") != cookie.lastIndexOf("JServSessionIdroot"));
	}
}
