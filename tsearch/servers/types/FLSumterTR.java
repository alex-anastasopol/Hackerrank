package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;

public class FLSumterTR extends FLGenericGovernmaxTR {
	
	private static final long serialVersionUID = 6525099447955957961L;

	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String linkText = getLinkText(row);
			return linkText.matches("(?is)[^A-Z0-9-]*([A-Z0-9-]{9,})[^-].*");
		}
	};
	
	public FLSumterTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}
	
	@Override
	public void smartParseForOldSites(ParsedResponse pr, ResultMap resultMap, String htmlString, int parserId){
		if(parserId == Parser.PAGE_DETAILS) {
			try {
			
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(htmlString, null);
				NodeList nodeList = htmlParser.parse(null);
				
				NodeList tables = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), false);
				String ownerInfo = "";
				
				for(int i = 0; i < tables.size(); i++) {
					TableTag table = (TableTag) tables.elementAt(i);
				
					if(table.toPlainTextString().indexOf("Mailing Address") > -1){
						
						TableTag table1 = (TableTag) table.getChildren()
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "600"), true)
							.elementAt(0);
						ownerInfo = table1.getRows()[2].toPlainTextString().replaceAll("&nbsp;", "");
						Pattern namePattern = Pattern.compile("(?is)\\s*Mailing\\s+Address(.*)Property Address");
						Matcher nameMatcher = namePattern.matcher(ownerInfo);
						if(nameMatcher.find()) {
							ownerInfo = nameMatcher.group(1).trim().replaceAll("\\s*\n\\s*", "\n");
							break;
						}
					}
				}
				
				resultMap.put("tmpOwnerName", ownerInfo);
//				GenericFunctions2.partyNamesFLSumterTR(resultMap, searchId);
				resultMap.remove("tmpOwnerName");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
/*
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.FLGenericGovernmaxTR#setModulesForAutoSearch(ro.cst.tsearch.servers.info.TSServerInfo)
 * 
 * Search by Owner
 * 
Type = POST   URL=http://fl-sumter-taxcollector.governmax.com/collectmax/search_collect.asp?go.x=1
........... HEADER PARAMETERS ............
Accept-Language           en-us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
site           collect_search
sid           C6F35618513F40878376D16EB3EF373F
paidflag           %
owner           Smith James
listview           Detail
l_nm           owner
go           GO
name_search           STARTSWITH
 * 
Type = POST   URL=http://fl-sumter-taxcollector.governmax.com/collectmax/search_collect.asp?go.x=1
........... HEADER PARAMETERS ............
Accept-Language           en-us
Referer           http://fl-sumter-taxcollector.governmax.com/collectmax/list_collect_v5-5.asp?sid=C6F35618513F40878376D16EB3EF373F&l_nm=owner&l_wc=%7cowner=SMITH%7cname_search=STARTSWITH%7clistview=DETAIL%7cpaidflag=%25&owner=SMITH&name_search=STARTSWITH&listview=DETAIL&paidflag=%25
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
site           collect_search
sid           C6F35618513F40878376D16EB3EF373F
paidflag           %
owner           Smith James
listview           Detail
l_nm           owner
go           GO
name_search           STARTSWITH
 * 

 * 
 * 
 * 
 * 16:43:29.921[907ms][total 907ms] Status: 302[Object moved]
POST http://fl-sumter-taxcollector.governmax.com/collectmax/search_collect.asp?go.x=1 Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[360] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmax.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmax.com/collectmax/search_collect.asp?l_nm=owner&site=collect_search&sid=DCAE9AC006B646929673F62E63A23006]
   Post Data:
      owner[Smith+James]
      name_search[STARTSWITH]
      listview[Detail]
      paidflag[%25]
      go[GO]
      site[collect_search]
      l_nm[owner]
      sid[DCAE9AC006B646929673F62E63A23006]
   Response Headers:
      Date[Fri, 13 Jun 2008 13:43:33 GMT]
      Server[Microsoft-IIS/6.0]
      Location[list_collect_v5-5.asp?sid=DCAE9AC006B646929673F62E63A23006&l_nm=owner&l_wc=|owner=SMITH+JAMES|name_search=STARTSWITH|listview=DETAIL|paidflag=%25&owner=SMITH+JAMES&name_search=STARTSWITH&listview=DETAIL&paidflag=%25]
      Content-Length[360]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:43:33 GMT]
      Cache-Control[private]

16:43:30.828[2015ms][total 2015ms] Status: 200[OK]
GET http://fl-sumter-taxcollector.governmax.com/collectmax/list_collect_v5-5.asp?sid=DCAE9AC006B646929673F62E63A23006&l_nm=owner&l_wc=|owner=SMITH+JAMES|name_search=STARTSWITH|listview=DETAIL|paidflag=%25&owner=SMITH+JAMES&name_search=STARTSWITH&listview=DETAIL&paidflag=%25 Load Flags[LOAD_DOCUMENT_URI  LOAD_REPLACE  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[-1] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmax.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmax.com/collectmax/search_collect.asp?l_nm=owner&site=collect_search&sid=DCAE9AC006B646929673F62E63A23006]
   Response Headers:
      Connection[close]
      Date[Fri, 13 Jun 2008 13:43:35 GMT]
      Server[Microsoft-IIS/6.0]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:43:33 GMT]
      Cache-Control[private]
 * 
 * 
Search by Account No

16:47:39.796[0ms][total 0ms] Status: pending[]
GET http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=account&form=searchform&formelement=0&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A Load Flags[VALIDATE_NEVER  LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[unknown] Mime Type[unknown]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmax.com/COLLECTMAX/list_collect_v5-5.asp?sid=CFAEAA9DA41C46BFAC87868E39A3AA2A&l_nm=owner&l_wc=|owner=WALTON|name_search=STARTSWITH|listview=DETAIL|paidflag=%25&owner=WALTON&name_search=STARTSWITH&listview=DETAIL&paidflag=%25]


16:47:44.390[656ms][total 656ms] Status: 302[Object moved]
POST http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?go.x=1 Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[326] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=account&form=searchform&formelement=0&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Post Data:
      account[ZG07-072-087]
      listview[Detail]
      paidflag[%25]
      go[GO]
      site[collect_search]
      l_nm[account]
      sid[CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Date[Fri, 13 Jun 2008 13:47:47 GMT]
      Server[Microsoft-IIS/6.0]
      Location[list_collect_v5-5.asp?sid=CFAEAA9DA41C46BFAC87868E39A3AA2A&l_nm=account&l_wc=|account=ZG07%2D072%2D087|listview=DETAIL|paidflag=%25&account=ZG07%2D072%2D087&listview=DETAIL&paidflag=%25]
      Content-Length[326]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:47:47 GMT]
      Cache-Control[private]


16:47:45.046[250ms][total 250ms] Status: 302[Object moved]
GET http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/list_collect_v5-5.asp?sid=CFAEAA9DA41C46BFAC87868E39A3AA2A&l_nm=account&l_wc=|account=ZG07%2D072%2D087|listview=DETAIL|paidflag=%25&account=ZG07%2D072%2D087&listview=DETAIL&paidflag=%25 Load Flags[LOAD_DOCUMENT_URI  LOAD_REPLACE  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[271] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=account&form=searchform&formelement=0&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Date[Fri, 13 Jun 2008 13:47:47 GMT]
      Server[Microsoft-IIS/6.0]
      Location[tab_collect_mvptaxV5.4.asp?t_nm=collect_mvptax&l_cr=1&t_wc=|parcelid=2520%2E0000+++++++++++|year=2007&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
      Content-Length[271]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:47:47 GMT]
      Cache-Control[private]


16:47:45.296[297ms][total 297ms] Status: 200[OK]
GET http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/tab_collect_mvptaxV5.4.asp?t_nm=collect_mvptax&l_cr=1&t_wc=|parcelid=2520%2E0000+++++++++++|year=2007&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A Load Flags[LOAD_DOCUMENT_URI  LOAD_REPLACE  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[875] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=account&form=searchform&formelement=0&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Date[Fri, 13 Jun 2008 13:47:48 GMT]
      Server[Microsoft-IIS/6.0]
      Content-Length[875]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:47:48 GMT]
      Cache-Control[private]

16:47:46.640[3250ms][total 3250ms] Status: 200[OK]
GET http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/tab_collect_mvptaxV5.4.asp?wait=done&t_nm=collect%5Fmvptax&l_cr=1&t_wc=%7Cparcelid%3D2520%2E0000+++++++++++%7Cyear%3D2007&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[-1] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/tab_collect_mvptaxV5.4.asp?t_nm=collect_mvptax&l_cr=1&t_wc=|parcelid=2520%2E0000+++++++++++|year=2007&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Connection[close]
      Date[Fri, 13 Jun 2008 13:47:52 GMT]
      Server[Microsoft-IIS/6.0]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:47:49 GMT]
      Cache-Control[private]
 *
Search by GEO Number



1/////////////////////////////////////
Type = POST   URL=http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp
........... HEADER PARAMETERS ............
Accept-Language           en-us
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
geo_number           251922
site           collect_search
sid           A8D7D22DF4DF4E7F99A5E74394B94EC7
paidflag           %
listview           Detail
l_nm           geo_number
go           GO

2//////////////////////////////////////

Type = POST   URL=http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp
........... HEADER PARAMETERS ............
Accept-Language           en-us
Referer           http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp
User-Agent           Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
Content-Type           application/x-www-form-urlencoded
Connection           Keep-Alive
Accept           image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, **
.......... POST PARAMETERS .............
geo_number           251922
site           collect_search
sid           A8D7D22DF4DF4E7F99A5E74394B94EC7
paidflag           %
listview           Detail
l_nm           geo_number
go           GO

16:57:44.328[828ms][total 828ms] Status: 302[Object moved]
POST http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?go.x=1 Load Flags[LOAD_DOCUMENT_URI  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[321] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=geo_number&form=searchform&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Post Data:
      geo_number[251922]
      listview[Detail]
      paidflag[%25]
      go[GO]
      site[collect_search]
      l_nm[geo_number]
      sid[CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Date[Fri, 13 Jun 2008 13:57:47 GMT]
      Server[Microsoft-IIS/6.0]
      Location[list_collect_v5-5.asp?sid=CFAEAA9DA41C46BFAC87868E39A3AA2A&l_nm=geo_number&l_wc=|geo_number=251922%2D|listview=DETAIL|paidflag=%25&geo_number=251922%2D&listview=DETAIL&paidflag=%25]
      Content-Length[321]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:57:47 GMT]
      Cache-Control[private]


16:57:45.156[531ms][total 531ms] Status: 200[OK]
GET http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/list_collect_v5-5.asp?sid=CFAEAA9DA41C46BFAC87868E39A3AA2A&l_nm=geo_number&l_wc=|geo_number=251922%2D|listview=DETAIL|paidflag=%25&geo_number=251922%2D&listview=DETAIL&paidflag=%25 Load Flags[LOAD_DOCUMENT_URI  LOAD_REPLACE  LOAD_INITIAL_DOCUMENT_URI  ] Content Size[-1] Mime Type[text/html]
   Request Headers:
      Host[fl-sumter-taxcollector.governmaxa.com]
      User-Agent[Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14]
      Accept[text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,**;q=0.5]
      Accept-Language[en-us,en;q=0.5]
      Accept-Encoding[gzip,deflate]
      Accept-Charset[ISO-8859-1,utf-8;q=0.7,*;q=0.7]
      Keep-Alive[300]
      Connection[keep-alive]
      Referer[http://fl-sumter-taxcollector.governmaxa.com/COLLECTMAX/search_collect.asp?l_nm=geo_number&form=searchform&sid=CFAEAA9DA41C46BFAC87868E39A3AA2A]
   Response Headers:
      Connection[close]
      Date[Fri, 13 Jun 2008 13:57:48 GMT]
      Server[Microsoft-IIS/6.0]
      Content-Type[text/html]
      Expires[Fri, 13 Jun 2008 13:57:47 GMT]
      Cache-Control[private]
 * 
 */	
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		FilterResponse rnre = new RejectNonRealEstate(searchId);
		rnre.setThreshold(new BigDecimal("0.65"));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);

		// P1 : search by PIN - Account Number	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);  
			module.addFilter(rnre);
			modules.add(module);		
		}
		
		// P2 : search by Address
		if(hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREET_FULL_NAME_EX);
			module.addFilter(rnre);
			module.addFilter(addressFilter);
			if(hasLegal()){
				module.addFilter(legalFilter);
			}
			modules.add(module);
		}
		
		// P3 : search by Owner Name	
		if(hasName()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			FilterResponse fr = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
			fr.setThreshold(new BigDecimal("0.65"));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(fr);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, module));
			
			if(hasLegal()){
				module.addFilter(legalFilter);
			}
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M","L;F;"});
			module.addIterator(nameIterator);
			
			modules.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}	
}
