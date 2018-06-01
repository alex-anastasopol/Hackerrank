package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class ILCookAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
 
	private static final Pattern dummyPattern       = Pattern.compile("&dummy=([A-Za-z0-9]+)&");
	private static final Pattern pinPattern         = Pattern.compile("&pin=0*(\\d+)");
	private static final Pattern pinPattern1        = Pattern.compile("(\\d{2})-(\\d{2})-?(\\d{3})?-?(\\d{3})?-?(\\d{4})?");
	private static final Pattern pinPattern2        = Pattern.compile("(\\d{2})(\\d{2})(\\d{3})?(\\d{3})?(\\d{4})?");

	private boolean downloadingForSave; 
	
	
	public ILCookAO(long searchId) {
		super(searchId);
	}

	public ILCookAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	private static final String CLASS_SEL_SELECT_PIN = 
		"<select name=\"ClassSel\" size=\"1\">" +
		"<option selected VALUE=\"\">Choose Residential Class --&gt;</option>"+
		"<option VALUE=202>2-02:  One Story Residence, Any Age, up to 999 Sq. Ft.</option>"+
		"<option VALUE=203>2-03:  One Story Residence, Any Age, 1,000 to 1,800 Sq. Ft.</option>"+
		"<option VALUE=204>2-04:  One Story Residence, Any Age, 1,801 Sq Ft. and Over</option>"+
		"<option VALUE=205>2-05:  Two or More Story Residence, Over 62 Years, up to 2,200 Sq. Ft.</option>"+
		"<option VALUE=206>2-06:  Two or More Story Residence, Over 62 Years, 2,201 to 4,999 Sq. Ft.</option>"+
		"<option VALUE=207>2-07:  Two or More Story Residence, Up to 62 Years, up to 2,000 Ft.</option>"+
		"<option VALUE=208>2-08:  Two or More Story Residence, Up to 62 Years, 3,801 to 4,999 Sq. Ft.</option>"+
		"<option VALUE=209>2-09:  Two or More Story Residence, Any Age, 5,000 Sq. Ft. & Over</option>"+
		"<option VALUE=210>2-10:  Old Style Row House, Over 62 Years</option>"+
		"<option VALUE=211>2-11:  Two to Six Apartments, Over 62 Years</option>"+
		"<option VALUE=212>2-12:  Two to Six Apartments, Up to 62 Years</option>"+
		"<option VALUE=221>2-21:  Corner Store, Office with Apartments Above Six Units or Less and Building Square Foot Less Than 20,000</option>"+
		"<option VALUE=234>2-34:  Split Level Residence with Lower Level Below Grade, All Ages, All Sizes</option>"+
		"<option VALUE=278>2-78:  Two or More Story Residence, Up to 62 Years, 2,001 to 3,800 Sq. Ft</option>"+
		"<option VALUE=295>2-95:  Individually Owned Row Houses or Townhouses, Up to 62 Years</option>"+
		"<option VALUE=299>2-99:  Residential Condominium</option>"+
		"</select>";	
	
	private static final String CLASS_SEL_SELECT_ADDRESS = 
		"<select name=\"ctl00$BodyContent$classel\" size=\"1\">" +
		"<option selected VALUE=\"0\">Choose Residential Class --&gt;</option>"+
		"<option VALUE=202>2-02:  One Story Residence, Any Age, up to 999 Sq. Ft.</option>"+
		"<option VALUE=203>2-03:  One Story Residence, Any Age, 1,000 to 1,800 Sq. Ft.</option>"+
		"<option VALUE=204>2-04:  One Story Residence, Any Age, 1,801 Sq Ft. and Over</option>"+
		"<option VALUE=205>2-05:  Two or More Story Residence, Over 62 Years, up to 2,200 Sq. Ft.</option>"+
		"<option VALUE=206>2-06:  Two or More Story Residence, Over 62 Years, 2,201 to 4,999 Sq. Ft.</option>"+
		"<option VALUE=207>2-07:  Two or More Story Residence, Up to 62 Years, up to 2,000 Ft.</option>"+
		"<option VALUE=208>2-08:  Two or More Story Residence, Up to 62 Years, 3,801 to 4,999 Sq. Ft.</option>"+
		"<option VALUE=209>2-09:  Two or More Story Residence, Any Age, 5,000 Sq. Ft. & Over</option>"+
		"<option VALUE=210>2-10:  Old Style Row House, Over 62 Years</option>"+
		"<option VALUE=211>2-11:  Two to Six Apartments, Over 62 Years</option>"+
		"<option VALUE=212>2-12:  Two to Six Apartments, Up to 62 Years</option>"+
		"<option VALUE=221>2-21:  Corner Store, Office with Apartments Above Six Units or Less and Building Square Foot Less Than 20,000</option>"+
		"<option VALUE=234>2-34:  Split Level Residence with Lower Level Below Grade, All Ages, All Sizes</option>"+
		"<option VALUE=278>2-78:  Two or More Story Residence, Up to 62 Years, 2,001 to 3,800 Sq. Ft</option>"+
		"<option VALUE=295>2-95:  Individually Owned Row Houses or Townhouses, Up to 62 Years</option>"+
		"<option VALUE=299>2-99:  Residential Condominium</option>"+
		"</select>";	
	
	private static final String CLASS_SEL2_SELECT_PIN = 
		"<select name=\"ClassSel2\" size=\"1\">" +
		"<option selected VALUE=\"\">Choose Non-Residential Class --&gt;</option>" +
		"<OPTION VALUE=000>0-00:  Exempt</OPTION>" +
		"<OPTION VALUE=100>1-00:  Vacant Land</OPTION>" +
		"<OPTION VALUE=190>1-90:  Other minor improvement which does not add value</OPTION>" +
		"<OPTION VALUE=200>2-00:  Residential land</OPTION>" +
		"<OPTION VALUE=201>2-01:  Residential garage</OPTION>" +
		"<OPTION VALUE=213>2-13:  Cooperatives (must have cdu of co)</OPTION>" +
		"<OPTION VALUE=224>2-24:  Farm buildings</OPTION>" +
		"<OPTION VALUE=225>2-25:  Qualified single room occupancy improvements (must have cdu of sr)</OPTION>" +
		"<OPTION VALUE=236>2-36:  Any residence located on a parcel used primarily for industrial or commercial use</OPTION>" +
		"<OPTION VALUE=239>2-39:  Farm land under use-value pricing</OPTION>" +
		"<OPTION VALUE=240>2-40:  Farm land under market pricing</OPTION>" +
		"<OPTION VALUE=241>2-41:  Vacant land under common ownership with adjacent residence</OPTION>" +
		"<OPTION VALUE=288>2-88:  Home improvement exemption</OPTION>" +
		"<OPTION VALUE=290>2-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=294>2-94:  Rented modern row houses or townhouses with less than seven units</OPTION>" +
		"<OPTION VALUE=297>2-97:  Special residential improvements</OPTION>" +
		"<OPTION VALUE=300>3-00:  Land Used in Conjunction with Rental Apartments</OPTION>" +
		"<OPTION VALUE=301>3-01:  Garage used in conjunction with rental apartments</OPTION>" +
		"<OPTION VALUE=313>3-13:  2 or 3 story building, 7 or more units, sgl. devel., one or more contig. parcels, in common ownership</OPTION>" +
		"<OPTION VALUE=314>3-14:  Two or three story non-frprf. crt. and corridor apts or california type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=315>3-15:  Two or three story non-fireproof corridor apartments,or california type apartments, interior entrance</OPTION>" +
		"<OPTION VALUE=318>3-18:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=319>3-19:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=320>3-20:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=321>3-21:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=390>3-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=391>3-91:  Apartment buildings over three stories</OPTION>" +
		"<OPTION VALUE=396>3-96:  Rented mdrn row houses, 7 or more units in a single develop. or 1 or more contig. parcels in cmn. ownshp.</OPTION>" +
		"<OPTION VALUE=397>3-97:  Special rental improvements</OPTION>" +
		"<OPTION VALUE=399>3-99:  Rental condo units in a sngl. dvlp. of 1 or more contig. parcels w 7 or more rental units, comn ownshp</OPTION>" +
		"<OPTION VALUE=400>4-00:  Not for profit land</OPTION>" +
		"<OPTION VALUE=401>4-01:  Not for profit garage</OPTION>" +
		"<OPTION VALUE=413>4-13:  Not for prof. 2 or 3 stry bldg., 7 or more units, sng develop., 1 or more contig. parcels, in comn ownshp</OPTION>" +
		"<OPTION VALUE=414>4-14:  Not for prof. 2 or 3 stry non-frprf crt and corridor apts or ca. type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=415>4-15:  Not for prof 2 or 3 stry non-frprf corridor apts, or ca. type apts, inter. entrance</OPTION>" +
		"<OPTION VALUE=416>4-16:  Not for profit non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=417>4-17:  Not for profit One story store</OPTION>" +
		"<OPTION VALUE=418>4-18:  Not for profit Two or three story frame stores, with apartments above</OPTION>" +
		"<OPTION VALUE=419>4-19:  Not for profit Two or three story old style store, with apartments above</OPTION>" +
		"<OPTION VALUE=420>4-20:  Not for profit Two or three story modern inside store with apartment above</OPTION>" +
		"<OPTION VALUE=421>4-21:  Not for profit corner store, office with apartment above</OPTION>" +
		"<OPTION VALUE=422>4-22:  Not for profit One story non-fireproof public garage</OPTION>" +
		"<OPTION VALUE=423>4-23:  Not for profit gasoline station</OPTION>" +
		"<OPTION VALUE=426>4-26:  Not for profit commercial greenhouse</OPTION>" +
		"<OPTION VALUE=427>4-27:  Not for profit theatres</OPTION>" +
		"<OPTION VALUE=428>4-28:  Not for profit bank buildings</OPTION>" +
		"<OPTION VALUE=429>4-29:  Not for profit motels</OPTION>" +
		"<OPTION VALUE=430>4-30:  Not for profit supermarket</OPTION>" +
		"<OPTION VALUE=431>4-31:  Not for profit shopping center</OPTION>" +
		"<OPTION VALUE=432>4-32:  Not for profit bowling alley</OPTION>" +
		"<OPTION VALUE=433>4-33:  Not for profit quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=435>4-35:  Not for profit golf course improvement</OPTION>" +
		"<OPTION VALUE=480>4-80:  Not for profit other industrial improvements</OPTION>" +
		"<OPTION VALUE=483>4-83:  Not for profit industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=487>4-87:  Not for profit special industrial improvements</OPTION>" +
		"<OPTION VALUE=489>4-89:  Not for profit industrial condominium units</OPTION>" +
		"<OPTION VALUE=490>4-90:  Not for profit other minor improvements</OPTION>" +
		"<OPTION VALUE=491>4-91:  Not for profit improvement over three stories</OPTION>" +
		"<OPTION VALUE=492>4-92:  Not for profit Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=493>4-93:  Not for profit industrial</OPTION>" +
		"<OPTION VALUE=496>4-96:  Not for profit rent mdrn row houses, 7 or more units a sing dvlp or 1 or more contig prcls in comn ownshp</OPTION>" +
		"<OPTION VALUE=497>4-97:  Not for profit special improvement</OPTION>" +
		"<OPTION VALUE=499>4-99:  Not for profit condominium</OPTION>" +
		"<OPTION VALUE=500>5-00:  Commercial land</OPTION>" +
		"<OPTION VALUE=501>5-01:  Garage used in conjunction with commercial improvements</OPTION>" +
		"<OPTION VALUE=516>5-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=517>5-17:  One story store</OPTION>" +
		"<OPTION VALUE=522>5-22:  One story non-fireproof public garage</OPTION>" +
		"<OPTION VALUE=523>5-23:  Gasoline station</OPTION>" +
		"<OPTION VALUE=526>5-26:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=527>5-27:  Theatres</OPTION>" +
		"<OPTION VALUE=528>5-28:  Bank buildings</OPTION>" +
		"<OPTION VALUE=529>5-29:  Motels</OPTION>" +
		"<OPTION VALUE=530>5-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=531>5-31:  Shopping center</OPTION>" +
		"<OPTION VALUE=532>5-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=533>5-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=535>5-35:  Golf course</OPTION>" +
		"<OPTION VALUE=550>5-50:  Industrial land</OPTION>" +
		"<OPTION VALUE=580>5-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=581>5-81:  Garage used in conjunction with industrial improvements</OPTION>" +
		"<OPTION VALUE=583>5-83:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=587>5-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=589>5-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=590>5-90:  Commercial minor improvements</OPTION>" +
		"<OPTION VALUE=591>5-91:  Commercial buildings over three stories</OPTION>" +
		"<OPTION VALUE=592>5-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=593>5-93:  Industrial</OPTION>" +
		"<OPTION VALUE=597>5-97:  Special commercial improvements</OPTION>" +
		"<OPTION VALUE=599>5-99:  Commercial condominium units</OPTION>" +
		"<OPTION VALUE=637>6-37:  Industrial brownfield land</OPTION>" +
		"<OPTION VALUE=638>6-38:  Industrial brownfield</OPTION>" +
		"<OPTION VALUE=650>6-50:  Industrial land</OPTION>" +
		"<OPTION VALUE=651>6-51:  Industrial land</OPTION>" +
		"<OPTION VALUE=654>6-54:  Other industrial brownfield minor improvements</OPTION>" +
		"<OPTION VALUE=655>6-55:  Garage used in conjunction with industrial brownfield incentive improvements</OPTION>" +
		"<OPTION VALUE=663>6-63:  Industrial</OPTION>" +
		"<OPTION VALUE=666>6-66:  Industrial brownfield quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=668>6-68:  Special improvements</OPTION>" +
		"<OPTION VALUE=669>6-69:  Industrial brownfield condominium units</OPTION>" +
		"<OPTION VALUE=670>6-70:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=671>6-71:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=673>6-73:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=677>6-77:  Special improvements</OPTION>" +
		"<OPTION VALUE=679>6-79:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=680>6-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=681>6-81:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=683>6-83:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=687>6-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=689>6-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=693>6-93:  Industrial</OPTION>" +
		"<OPTION VALUE=700>7-00:  Commercial incentive land</OPTION>" +
		"<OPTION VALUE=701>7-01:  Garage used in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=716>7-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=717>7-17:  One story retail, restaurant, or banquet hall, medical building, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=722>7-22:  Garage, public/service</OPTION>" +
		"<OPTION VALUE=723>7-23:  Gasoline station, with/without bays, store</OPTION>" +
		"<OPTION VALUE=726>7-26:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=727>7-27:  Theatres</OPTION>" +
		"<OPTION VALUE=728>7-28:  Bank buildings</OPTION>" +
		"<OPTION VALUE=729>7-29:  Motels</OPTION>" +
		"<OPTION VALUE=730>7-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=731>7-31:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=732>7-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=733>7-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=735>7-35:  Golf course</OPTION>" +
		"<OPTION VALUE=742>7-42:  Commercial incentive land</OPTION>" +
		"<OPTION VALUE=743>7-43:  Garage used in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=745>7-45:  Golf course</OPTION>" +
		"<OPTION VALUE=746>7-46:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=747>7-47:  One story retail, rstrnt, or bnqt hall, med. blding, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=748>7-48:  Motels</OPTION><OPTION VALUE=752>7-52:  Garage, public/service</OPTION>" +
		"<OPTION VALUE=753>7-53:  Gasoline station, with/without bays, store</OPTION>" +
		"<OPTION VALUE=756>7-56:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=757>7-57:  Theatres</OPTION><OPTION VALUE=758>7-58:  Bank buildings</OPTION>" +
		"<OPTION VALUE=760>7-60:  Supermarket</OPTION>" +
		"<OPTION VALUE=761>7-61:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=762>7-62:  Bowling alley</OPTION><OPTION VALUE=764>7-64:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=765>7-65:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=767>7-67:  Facilities (tennis, rqtball, hlth club), (nursing, retirement home), auto. dlrshp, comm. structure</OPTION>" +
		"<OPTION VALUE=772>7-72:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=774>7-74:  office building (One story, low rise, mid rise, high rise)</OPTION>" +
		"<OPTION VALUE=790>7-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=791>7-91:  Office building (one story, low rise, mid rise, high rise)</OPTION>" +
		"<OPTION VALUE=792>7-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=797>7-97:  Facilities (tennis, rqtbll, hlth club), (nursing, rtrmnt home), auto dlrshp, comm. structure</OPTION>" +
		"<OPTION VALUE=798>7-98:  Commercial/industrial-condominium units/garage</OPTION>" +
		"<OPTION VALUE=799>7-99:  Commercial/industrial-condominium units/garage</OPTION>" +
		"<OPTION VALUE=801>8-01:  Garage in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=816>8-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=817>8-17:  One story retail, restaurant, (banquet hall, fast food), medical, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=822>8-22:  Garage (public/service)</OPTION>" +
		"<OPTION VALUE=823>8-23:  Gasoline station with/without bay, store</OPTION>" +
		"<OPTION VALUE=826>8-26:  Commercial greenhouse</OPTION><OPTION VALUE=827>8-27:  Theatres</OPTION>" +
		"<OPTION VALUE=828>8-28:  Bank building</OPTION>" +
		"<OPTION VALUE=829>8-29:  Motels</OPTION>" +
		"<OPTION VALUE=830>8-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=831>8-31:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=832>8-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=833>8-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=835>8-35:  Golf course</OPTION>" +
		"<OPTION VALUE=850>8-50:  Industrial incentive land</OPTION>" +
		"<OPTION VALUE=880>8-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=881>8-81:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=883>8-83:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=887>8-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=889>8-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=890>8-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=891>8-91:  Office building, (One story, low rise, midrise, high rise)</OPTION>" +
		"<OPTION VALUE=892>8-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=893>8-93:  Industrial buildings</OPTION>" +
		"<OPTION VALUE=897>8-97:  Facilities, (tennis, rqtball, hlth club), (nursing, retirement home), auto. dlrshp, misc. comm. structure</OPTION>" +
		"<OPTION VALUE=899>8-99:  Commercial/industrial condominium units/Garage</OPTION>" +
		"<OPTION VALUE=900>9-00:  Land used in conjunction with incentive rental apartments</OPTION>" +
		"<OPTION VALUE=901>9-01:  Garage used in conjunction with incentive rental apartments</OPTION>" +
		"<OPTION VALUE=913>9-13:  2 or 3 story bldng, 7 or more units, sngle devel., 1 or more contig. parcels, in common ownership</OPTION>" +
		"<OPTION VALUE=914>9-14:  2 or 3 story non-freprf crt and corridor apts or california type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=915>9-15:  2 or 3 story non-frprf corridor apts, or california type apts, interior entrance</OPTION>" +
		"<OPTION VALUE=918>9-18:  2 or 3 story frame stores, with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=919>9-19:  2 or 3 story old style store, with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=920>9-20:  2 or 3 story modern inside store with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=921>9-21:  Corner store, office with apartments above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=959>9-59:  Rental condo unts in a sing. dvlp. of 1 or more contiprcls w/ 7 or more rent units, commn ownrshp</OPTION>" +
		"<OPTION VALUE=990>9-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=991>9-91:  Apartment buildings over three stories</OPTION>" +
		"<OPTION VALUE=996>9-96:  Rental mdrn row houses, 7 or more unts in a sing. dvlpment or 1 or more contig. prcls in comm. ownrshp</OPTION>" +
		"<OPTION VALUE=997>9-97:  Special rental improvements</OPTION>" +
		"</select>";	
	private static final String CLASS_SEL2_SELECT_ADDRESS = 
		"<select name=\"ctl00$BodyContent$ClassSel2\" size=\"1\">" +
		"<option selected VALUE=\"0\">Choose Non-Residential Class --&gt;</option>" +
		"<OPTION VALUE=000>0-00:  Exempt</OPTION>" +
		"<OPTION VALUE=100>1-00:  Vacant Land</OPTION>" +
		"<OPTION VALUE=190>1-90:  Other minor improvement which does not add value</OPTION>" +
		"<OPTION VALUE=200>2-00:  Residential land</OPTION>" +
		"<OPTION VALUE=201>2-01:  Residential garage</OPTION>" +
		"<OPTION VALUE=213>2-13:  Cooperatives (must have cdu of co)</OPTION>" +
		"<OPTION VALUE=224>2-24:  Farm buildings</OPTION>" +
		"<OPTION VALUE=225>2-25:  Qualified single room occupancy improvements (must have cdu of sr)</OPTION>" +
		"<OPTION VALUE=236>2-36:  Any residence located on a parcel used primarily for industrial or commercial use</OPTION>" +
		"<OPTION VALUE=239>2-39:  Farm land under use-value pricing</OPTION>" +
		"<OPTION VALUE=240>2-40:  Farm land under market pricing</OPTION>" +
		"<OPTION VALUE=241>2-41:  Vacant land under common ownership with adjacent residence</OPTION>" +
		"<OPTION VALUE=288>2-88:  Home improvement exemption</OPTION>" +
		"<OPTION VALUE=290>2-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=294>2-94:  Rented modern row houses or townhouses with less than seven units</OPTION>" +
		"<OPTION VALUE=297>2-97:  Special residential improvements</OPTION>" +
		"<OPTION VALUE=300>3-00:  Land Used in Conjunction with Rental Apartments</OPTION>" +
		"<OPTION VALUE=301>3-01:  Garage used in conjunction with rental apartments</OPTION>" +
		"<OPTION VALUE=313>3-13:  2 or 3 story building, 7 or more units, sgl. devel., one or more contig. parcels, in common ownership</OPTION>" +
		"<OPTION VALUE=314>3-14:  Two or three story non-frprf. crt. and corridor apts or california type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=315>3-15:  Two or three story non-fireproof corridor apartments,or california type apartments, interior entrance</OPTION>" +
		"<OPTION VALUE=318>3-18:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=319>3-19:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=320>3-20:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=321>3-21:  Mixed use commercial/residential with apts. above seven units or more or building sq. ft. over 20,000</OPTION>" +
		"<OPTION VALUE=390>3-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=391>3-91:  Apartment buildings over three stories</OPTION>" +
		"<OPTION VALUE=396>3-96:  Rented mdrn row houses, 7 or more units in a single develop. or 1 or more contig. parcels in cmn. ownshp.</OPTION>" +
		"<OPTION VALUE=397>3-97:  Special rental improvements</OPTION>" +
		"<OPTION VALUE=399>3-99:  Rental condo units in a sngl. dvlp. of 1 or more contig. parcels w 7 or more rental units, comn ownshp</OPTION>" +
		"<OPTION VALUE=400>4-00:  Not for profit land</OPTION>" +
		"<OPTION VALUE=401>4-01:  Not for profit garage</OPTION>" +
		"<OPTION VALUE=413>4-13:  Not for prof. 2 or 3 stry bldg., 7 or more units, sng develop., 1 or more contig. parcels, in comn ownshp</OPTION>" +
		"<OPTION VALUE=414>4-14:  Not for prof. 2 or 3 stry non-frprf crt and corridor apts or ca. type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=415>4-15:  Not for prof 2 or 3 stry non-frprf corridor apts, or ca. type apts, inter. entrance</OPTION>" +
		"<OPTION VALUE=416>4-16:  Not for profit non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=417>4-17:  Not for profit One story store</OPTION>" +
		"<OPTION VALUE=418>4-18:  Not for profit Two or three story frame stores, with apartments above</OPTION>" +
		"<OPTION VALUE=419>4-19:  Not for profit Two or three story old style store, with apartments above</OPTION>" +
		"<OPTION VALUE=420>4-20:  Not for profit Two or three story modern inside store with apartment above</OPTION>" +
		"<OPTION VALUE=421>4-21:  Not for profit corner store, office with apartment above</OPTION>" +
		"<OPTION VALUE=422>4-22:  Not for profit One story non-fireproof public garage</OPTION>" +
		"<OPTION VALUE=423>4-23:  Not for profit gasoline station</OPTION>" +
		"<OPTION VALUE=426>4-26:  Not for profit commercial greenhouse</OPTION>" +
		"<OPTION VALUE=427>4-27:  Not for profit theatres</OPTION>" +
		"<OPTION VALUE=428>4-28:  Not for profit bank buildings</OPTION>" +
		"<OPTION VALUE=429>4-29:  Not for profit motels</OPTION>" +
		"<OPTION VALUE=430>4-30:  Not for profit supermarket</OPTION>" +
		"<OPTION VALUE=431>4-31:  Not for profit shopping center</OPTION>" +
		"<OPTION VALUE=432>4-32:  Not for profit bowling alley</OPTION>" +
		"<OPTION VALUE=433>4-33:  Not for profit quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=435>4-35:  Not for profit golf course improvement</OPTION>" +
		"<OPTION VALUE=480>4-80:  Not for profit other industrial improvements</OPTION>" +
		"<OPTION VALUE=483>4-83:  Not for profit industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=487>4-87:  Not for profit special industrial improvements</OPTION>" +
		"<OPTION VALUE=489>4-89:  Not for profit industrial condominium units</OPTION>" +
		"<OPTION VALUE=490>4-90:  Not for profit other minor improvements</OPTION>" +
		"<OPTION VALUE=491>4-91:  Not for profit improvement over three stories</OPTION>" +
		"<OPTION VALUE=492>4-92:  Not for profit Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=493>4-93:  Not for profit industrial</OPTION>" +
		"<OPTION VALUE=496>4-96:  Not for profit rent mdrn row houses, 7 or more units a sing dvlp or 1 or more contig prcls in comn ownshp</OPTION>" +
		"<OPTION VALUE=497>4-97:  Not for profit special improvement</OPTION>" +
		"<OPTION VALUE=499>4-99:  Not for profit condominium</OPTION>" +
		"<OPTION VALUE=500>5-00:  Commercial land</OPTION>" +
		"<OPTION VALUE=501>5-01:  Garage used in conjunction with commercial improvements</OPTION>" +
		"<OPTION VALUE=516>5-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=517>5-17:  One story store</OPTION>" +
		"<OPTION VALUE=522>5-22:  One story non-fireproof public garage</OPTION>" +
		"<OPTION VALUE=523>5-23:  Gasoline station</OPTION>" +
		"<OPTION VALUE=526>5-26:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=527>5-27:  Theatres</OPTION>" +
		"<OPTION VALUE=528>5-28:  Bank buildings</OPTION>" +
		"<OPTION VALUE=529>5-29:  Motels</OPTION>" +
		"<OPTION VALUE=530>5-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=531>5-31:  Shopping center</OPTION>" +
		"<OPTION VALUE=532>5-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=533>5-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=535>5-35:  Golf course</OPTION>" +
		"<OPTION VALUE=550>5-50:  Industrial land</OPTION>" +
		"<OPTION VALUE=580>5-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=581>5-81:  Garage used in conjunction with industrial improvements</OPTION>" +
		"<OPTION VALUE=583>5-83:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=587>5-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=589>5-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=590>5-90:  Commercial minor improvements</OPTION>" +
		"<OPTION VALUE=591>5-91:  Commercial buildings over three stories</OPTION>" +
		"<OPTION VALUE=592>5-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=593>5-93:  Industrial</OPTION>" +
		"<OPTION VALUE=597>5-97:  Special commercial improvements</OPTION>" +
		"<OPTION VALUE=599>5-99:  Commercial condominium units</OPTION>" +
		"<OPTION VALUE=637>6-37:  Industrial brownfield land</OPTION>" +
		"<OPTION VALUE=638>6-38:  Industrial brownfield</OPTION>" +
		"<OPTION VALUE=650>6-50:  Industrial land</OPTION>" +
		"<OPTION VALUE=651>6-51:  Industrial land</OPTION>" +
		"<OPTION VALUE=654>6-54:  Other industrial brownfield minor improvements</OPTION>" +
		"<OPTION VALUE=655>6-55:  Garage used in conjunction with industrial brownfield incentive improvements</OPTION>" +
		"<OPTION VALUE=663>6-63:  Industrial</OPTION>" +
		"<OPTION VALUE=666>6-66:  Industrial brownfield quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=668>6-68:  Special improvements</OPTION>" +
		"<OPTION VALUE=669>6-69:  Industrial brownfield condominium units</OPTION>" +
		"<OPTION VALUE=670>6-70:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=671>6-71:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=673>6-73:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=677>6-77:  Special improvements</OPTION>" +
		"<OPTION VALUE=679>6-79:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=680>6-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=681>6-81:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=683>6-83:  Industrial quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=687>6-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=689>6-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=693>6-93:  Industrial</OPTION>" +
		"<OPTION VALUE=700>7-00:  Commercial incentive land</OPTION>" +
		"<OPTION VALUE=701>7-01:  Garage used in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=716>7-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=717>7-17:  One story retail, restaurant, or banquet hall, medical building, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=722>7-22:  Garage, public/service</OPTION>" +
		"<OPTION VALUE=723>7-23:  Gasoline station, with/without bays, store</OPTION>" +
		"<OPTION VALUE=726>7-26:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=727>7-27:  Theatres</OPTION>" +
		"<OPTION VALUE=728>7-28:  Bank buildings</OPTION>" +
		"<OPTION VALUE=729>7-29:  Motels</OPTION>" +
		"<OPTION VALUE=730>7-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=731>7-31:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=732>7-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=733>7-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=735>7-35:  Golf course</OPTION>" +
		"<OPTION VALUE=742>7-42:  Commercial incentive land</OPTION>" +
		"<OPTION VALUE=743>7-43:  Garage used in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=745>7-45:  Golf course</OPTION>" +
		"<OPTION VALUE=746>7-46:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=747>7-47:  One story retail, rstrnt, or bnqt hall, med. blding, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=748>7-48:  Motels</OPTION><OPTION VALUE=752>7-52:  Garage, public/service</OPTION>" +
		"<OPTION VALUE=753>7-53:  Gasoline station, with/without bays, store</OPTION>" +
		"<OPTION VALUE=756>7-56:  Commercial greenhouse</OPTION>" +
		"<OPTION VALUE=757>7-57:  Theatres</OPTION><OPTION VALUE=758>7-58:  Bank buildings</OPTION>" +
		"<OPTION VALUE=760>7-60:  Supermarket</OPTION>" +
		"<OPTION VALUE=761>7-61:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=762>7-62:  Bowling alley</OPTION><OPTION VALUE=764>7-64:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=765>7-65:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=767>7-67:  Facilities (tennis, rqtball, hlth club), (nursing, retirement home), auto. dlrshp, comm. structure</OPTION>" +
		"<OPTION VALUE=772>7-72:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=774>7-74:  office building (One story, low rise, mid rise, high rise)</OPTION>" +
		"<OPTION VALUE=790>7-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=791>7-91:  Office building (one story, low rise, mid rise, high rise)</OPTION>" +
		"<OPTION VALUE=792>7-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=797>7-97:  Facilities (tennis, rqtbll, hlth club), (nursing, rtrmnt home), auto dlrshp, comm. structure</OPTION>" +
		"<OPTION VALUE=798>7-98:  Commercial/industrial-condominium units/garage</OPTION>" +
		"<OPTION VALUE=799>7-99:  Commercial/industrial-condominium units/garage</OPTION>" +
		"<OPTION VALUE=801>8-01:  Garage in conjunction with commercial incentive improvements</OPTION>" +
		"<OPTION VALUE=816>8-16:  Non-fireproof hotel or rooming house (apartment hotel)</OPTION>" +
		"<OPTION VALUE=817>8-17:  One story retail, restaurant, (banquet hall, fast food), medical, miscellaneous commercial use</OPTION>" +
		"<OPTION VALUE=822>8-22:  Garage (public/service)</OPTION>" +
		"<OPTION VALUE=823>8-23:  Gasoline station with/without bay, store</OPTION>" +
		"<OPTION VALUE=826>8-26:  Commercial greenhouse</OPTION><OPTION VALUE=827>8-27:  Theatres</OPTION>" +
		"<OPTION VALUE=828>8-28:  Bank building</OPTION>" +
		"<OPTION VALUE=829>8-29:  Motels</OPTION>" +
		"<OPTION VALUE=830>8-30:  Supermarket</OPTION>" +
		"<OPTION VALUE=831>8-31:  Shopping center (regional, community, neighborhood, promotional, specialty)</OPTION>" +
		"<OPTION VALUE=832>8-32:  Bowling alley</OPTION>" +
		"<OPTION VALUE=833>8-33:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=835>8-35:  Golf course</OPTION>" +
		"<OPTION VALUE=850>8-50:  Industrial incentive land</OPTION>" +
		"<OPTION VALUE=880>8-80:  Other industrial minor improvements</OPTION>" +
		"<OPTION VALUE=881>8-81:  Garage used in conjunction with industrial incentive improvements</OPTION>" +
		"<OPTION VALUE=883>8-83:  Quonset huts and butler type buildings</OPTION>" +
		"<OPTION VALUE=887>8-87:  Special industrial improvements</OPTION>" +
		"<OPTION VALUE=889>8-89:  Industrial condominium units</OPTION>" +
		"<OPTION VALUE=890>8-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=891>8-91:  Office building, (One story, low rise, midrise, high rise)</OPTION>" +
		"<OPTION VALUE=892>8-92:  Two or three story building containing part or all retail and/or commercial space</OPTION>" +
		"<OPTION VALUE=893>8-93:  Industrial buildings</OPTION>" +
		"<OPTION VALUE=897>8-97:  Facilities, (tennis, rqtball, hlth club), (nursing, retirement home), auto. dlrshp, misc. comm. structure</OPTION>" +
		"<OPTION VALUE=899>8-99:  Commercial/industrial condominium units/Garage</OPTION>" +
		"<OPTION VALUE=900>9-00:  Land used in conjunction with incentive rental apartments</OPTION>" +
		"<OPTION VALUE=901>9-01:  Garage used in conjunction with incentive rental apartments</OPTION>" +
		"<OPTION VALUE=913>9-13:  2 or 3 story bldng, 7 or more units, sngle devel., 1 or more contig. parcels, in common ownership</OPTION>" +
		"<OPTION VALUE=914>9-14:  2 or 3 story non-freprf crt and corridor apts or california type apts, no corridors, ex. entrance</OPTION>" +
		"<OPTION VALUE=915>9-15:  2 or 3 story non-frprf corridor apts, or california type apts, interior entrance</OPTION>" +
		"<OPTION VALUE=918>9-18:  2 or 3 story frame stores, with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=919>9-19:  2 or 3 story old style store, with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=920>9-20:  2 or 3 story modern inside store with apts above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=921>9-21:  Corner store, office with apartments above (must be split coded with another class)</OPTION>" +
		"<OPTION VALUE=959>9-59:  Rental condo unts in a sing. dvlp. of 1 or more contiprcls w/ 7 or more rent units, commn ownrshp</OPTION>" +
		"<OPTION VALUE=990>9-90:  Other minor improvements</OPTION>" +
		"<OPTION VALUE=991>9-91:  Apartment buildings over three stories</OPTION>" +
		"<OPTION VALUE=996>9-96:  Rental mdrn row houses, 7 or more unts in a sing. dvlpment or 1 or more contig. prcls in comm. ownrshp</OPTION>" +
		"<OPTION VALUE=997>9-97:  Special rental improvements</OPTION>" +
		"</select>";	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule sim = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		setupSelectBox(sim.getFunction(6), CLASS_SEL_SELECT_PIN);
        setupSelectBox(sim.getFunction(7), CLASS_SEL2_SELECT_PIN);
        
        sim = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
        setupSelectBox(sim.getFunction(7), CLASS_SEL_SELECT_ADDRESS);
        setupSelectBox(sim.getFunction(8), CLASS_SEL2_SELECT_ADDRESS);
		return msiServerInfoDefault;
	}
	
	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		// split the PIN into its parts
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
            String [] pins = extractPins(module.getFunction(0).getParamValue());
    		if(pins == null){
    			return ServerResponse.createErrorResponse("Invalid PIN!");
    		}
            for(int i=0; i<5; i++){
            	module.getFunction(i + 1).setParamValue(pins[i]);
            }
        }
        return super.SearchBy(module, sd);
    }
    
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Collection<String> pins = getSearchAttributes().getPins(-1);
		if(pins.size() > 0) {
			for(String pin: pins){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);
				modules.add(module);	
			}
			if(pins.size() > 1) { // multiple pins 
				serverInfo.setModulesForAutoSearch(modules);
				return;
			}
		}
		
		SearchAttributes sa = getSearchAttributes();
		
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String city = sa.getAtribute(SearchAttributes.P_CITY);
		
		if(StringUtils.isNotEmpty(streetNo)
				&& StringUtils.isNotEmpty(streetName)
				&& StringUtils.isNotEmpty(city)) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, streetNo);
			module.forceValue(3, streetName);
			module.forceValue(6, city);
			module.addFilter(AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true));
			module.addFilter(CityFilterFactory.getCityFilterDefault(searchId));
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		return  mSearch.getSa().getPins(-1).size() > 1;
	}  
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		
		String response = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		String contents;
		
		
		if (response.contains(ro.cst.tsearch.connection.http2.ILCookAO.CAPTCHA_OCR_FAILED_MESSAGE)) {
			parsedResponse.setError(response);
			return;
		}
		
		if(viParseID == ID_SEARCH_BY_ADDRESS && response.contains("Property Characteristics")) {
			viParseID = ID_DETAILS;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, response, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            }
			
			/*
			contents = getIntermediateContents(response, viParseID);
			if(contents == null){
				return;
			}			
			contents = rewriteLinks(response, contents);
        	
			// call the parser
        	parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
        	
			// append the additional info
        	Response.getParsedResponse().setFooter(Response.getParsedResponse().getFooter());
        	*/
			break;
			
		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
			if(response.contains("Please enter a valid security code")){
				Response.getParsedResponse().setError("<font color=\"red\">Please enter a valid security code!</font>");
				return;
			}
			contents = getDetails(response);
			if(StringUtils.isEmpty(contents)){
				Response.getParsedResponse().setError("Selected document has no details!");
				return;
			}						
			String displayContents = contents;
			
			ResultMap map = new ResultMap();
			parseAndFillResultMap(Response, contents, map);

			String pin  = (String)map.get("PropertyIdentificationSet.ParcelID");
			if(StringUtils.isEmpty(pin)) {
				pin = "none";
			}
			
			
			// create filename
			String fileName = pin + ".html";
			
			if ((!downloadingForSave)) {

                String originalLink = sAction + "&dummy=" + pin + "&" + Response.getQuerry();
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

                if (FileAlreadyExist(fileName) ) {
                	displayContents += CreateFileAlreadyInTSD();
                } else {
                	displayContents = addSaveToTsdButton(displayContents, sSave2TSDLink, viParseID);
                    mSearch.addInMemoryDoc(sSave2TSDLink, response);
                }

                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
                Response.getParsedResponse().setResponse(displayContents);
                
            } else {
            	
                msSaveToTSDFileName = fileName;
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = displayContents + CreateFileAlreadyInTSD();                
                //parser.Parse(Response.getParsedResponse(), displayContents, Parser.PAGE_DETAILS);
                smartParseDetails(Response,displayContents);
                //DocumentI document = smartParseDe
                //Response.getParsedResponse().setOnlyResponse(displayContents);
			}

			break;
			
		case ID_GET_LINK :			
			String query = Response.getQuerry();
			if(query.contains("seq=")){
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;			
			break;
			
		}
	}
	
	
	private String rewriteLinks(String initialResponse, String contents) {
		// parse and store parameters on search
		Map<String,String> params = HttpSite.fillAndValidateConnectionParams(
				initialResponse, 
				new String[]{ "__VIEWSTATE", "__EVENTVALIDATION" }, 
				ro.cst.tsearch.connection.http2.ILCookAO.FORM_NAME);
		int seq = getSeq();
		mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
		
		// rewrite detail links
		contents = contents.replaceAll("(?i)<a href=\"(property_details.aspx)\\?([^\"]+)\"[^>]*>",
				"<a href='" + CreatePartialLink(TSConnectionURL.idGET) +"/Property_Search/$1&" + "$2" + "'>");
		
		contents = contents.replaceAll("(?i)<a href=\"javascript:__doPostBack\\('([^']+)',''\\)\">", 
				"<a href='" + CreatePartialLink(TSConnectionURL.idPOST) +"/Property_Search/Property_Search_Results.aspx?__EVENTTARGET=$1&seq=" + seq + "'>");
		
		return contents;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map)
			{
		
		map.put("OtherInformationSet.SrcType","AO");
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList spans = parser.extractAllNodesThatMatch(new TagNameFilter("span"));
			NodeList nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id","idadressa"));
			if(nodeList.size() > 0) {
				String address = nodeList.elementAt(0).toPlainTextString();
				map.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
				map.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id","idpin"));
			if(nodeList.size() > 0) {
				map.put("PropertyIdentificationSet.ParcelID", nodeList.elementAt(0).toPlainTextString().replaceAll("-", ""));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_BodyContent_lbl_City"));
			if(nodeList.size() > 0) {
				map.put("PropertyIdentificationSet.City", nodeList.elementAt(0).toPlainTextString());
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_BodyContent_lbl_currtot"));
			if(nodeList.size() > 0) {
				map.put("PropertyAppraisalSet.TotalAssessment", nodeList.elementAt(0).toPlainTextString().replaceAll(",", ""));
			}
			
		
		} catch (Exception e) {
			logger.error(getSearch().getID() + ": Error while parsing ILCookAO detail page ", e);
		}
		return null;	
	}
	
	/**
	 * Split the results row; called from 
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	@SuppressWarnings("unchecked")
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException {
		// perform split        
		p.splitResultRows(pr, htmlString, pageId, "<tr>", "</tr>", linkStart, action);
		
        // remove table header
        Vector rows = pr.getResultRows();        
        if (rows.size()>0){ 
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse()); 
        }
    }
	
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link){
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find()){
			return dummyMatcher.group(1) + ".html";
		}
		Matcher pinMatcher = pinPattern.matcher(link);
		if(pinMatcher.find()){
			return pinMatcher.group(1) + ".html";
		}
        return "none.html";
    }
	
	/**
	 * Extract the pin parts from 11-11-111-111-1111 or 11111111111111
	 * @param pin input pin
	 * @return 5 pin parts
	 */
	public static String [] extractPins(String pin){
				 
		Matcher m = pinPattern1.matcher(pin);
		if(!m.matches()){
			m = pinPattern2.matcher(pin);
		}
		if(!m.matches()){
			return null;
		}
		String p1 = m.group(1); if(p1 == null){ p1 = ""; }
		String p2 = m.group(2); if(p2 == null){ p2 = ""; }
		String p3 = m.group(3); if(p3 == null){ p3 = ""; }
		String p4 = m.group(4); if(p4 == null){ p4 = ""; }
		String p5 = m.group(5); if(p5 == null){ p5 = ""; }
		return new String [] {p1, p2, p3, p4, p5};
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable){
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		if(table.contains("Unable to Process Search Criteria") || table.contains("No Records found")){
			return intermediaryResponse;
		}
		
		try {
			table = table.replaceAll("(?i)<a href=\"#infoPane\"[^>]*>([^<]+)</a>","$1");
			table = table.replaceAll("<img[^>]*>","");
			table = table.replace("Neigh- borhood","Neighborhood");
			
			table = rewriteLinks(response.getResult(), table);
			
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag mainTable = (TableTag)parser.getNodeById("ctl00_BodyContent_DisplayGrid"); 
				
			if(mainTable == null) {
				return intermediaryResponse;
			}
			
			String footer = "";
			String header = "";
			TableRow[] rows = mainTable.getRows();
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 7) {
					TableColumn[] cols = row.getColumns();
					LinkTag linkTag = ((LinkTag)cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(
							linkTag.getLink(),
							linkTag.getLink(),
							TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[0].toPlainTextString().trim());
					
					String[] address = StringFormats.parseAddress(cols[1].toPlainTextString().trim());
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1] + " " + cols[2].toPlainTextString().trim());
					
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), cols[6].toPlainTextString().replaceAll(",", "").trim());
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cols[3].toPlainTextString().trim());

					
					resultMap.removeTempDef();
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} else if (rows.length - 1 == i ) {
					if("pageLinks".equals(row.getAttribute("class"))) {
						footer = row.toHtml();
						header = rows[0].toHtml();
						
						int searchType = getSearch().getSearchType();
						
						
						if(searchType != Search.PARENT_SITE_SEARCH) {
						
							NodeList pageLinksList = row.getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("a"), true);
							int crtPage = -1;
							if(pageLinksList.size() > 0) {
								NodeList spanList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
								if(spanList.size() > 0) {
									crtPage = Integer.parseInt(spanList.elementAt(0).toPlainTextString().trim());
								}
							}
							LinkTag crossLink = null;
							boolean foundNextPage = false;
							for (int indexPageLink = 0; indexPageLink < pageLinksList.size() && crtPage >= 0; indexPageLink++) {
								crossLink = (LinkTag) pageLinksList.elementAt(indexPageLink);
								
								if(!foundNextPage) {
									try {
										String crossLinkText = crossLink.toPlainTextString().trim();
										if(Integer.parseInt(crossLinkText) == (crtPage + 1)) {
											response.getParsedResponse().setNextLink("<a href=" + crossLink.extractLink()+ ">Next</a>");
											foundNextPage = true;
											break;
										}
										
									} catch (Exception e) {
									}
								}							
							}
							if(!foundNextPage && crossLink!= null) {
								String crossLinkText = crossLink.toPlainTextString().trim();
								if("...".equals(crossLinkText)) {
									response.getParsedResponse().setNextLink("<a href=" + crossLink.extractLink()+ ">Next</a>");
								}
								
							}
						}
						
					}
				}
			}
			
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header);
			response.getParsedResponse().setFooter(footer + "</table>");

		} catch (Exception e) {
			logger.error("Error while parsing intermediary response for " + getClass().getName() + " and searchId " + searchId, e);
			
		}
		
		outputTable.append(table);
		return intermediaryResponse;

		
		// clean-up links
		
		/*
		table = table.replaceFirst("(?i)<table[^>]+>","<table border='1' cellspacing='0' cellpadding='0' width='100%'>");
		table = table.replaceAll("(?i)</?font[^>]*>","");
		table = table.replaceAll("(?i)<th[^>]+>","<th align='center'>");
		table = table.replaceAll("(?i)<td[^>]+>","<td align='center'>");
		table = table.replaceAll("(?i)<tr[^>]+>","<tr>");
		table = table.replaceFirst("(?i)<tr>", "<tr align=\"center\" bgcolor=\"#cccccc\">");
		
		
		// polish the table a little bit
		
		//table = table.replaceFirst("(?i)<tr", "<tr bgcolor='#cccccc'");
		
		// correct html
		table = table.replace("<TR>","<tr>");
		table = table.replace("<TD>","<td>");
		table = table.replace("</TR>","</tr>");
		table = table.replace("</TD>","</td>");		
		table = table.replace("</td><tr>","</td></tr><tr>");
		
		int lastPos = table.lastIndexOf("<td");
		String colspan = "<td colspan=\"8\" ";
		if(lastPos > 0) {
			table = table.substring(0, lastPos) + colspan + table.substring(lastPos + 3);
		}
		
		*/
		
	}
	
	/**
	 * Extract relevant details info from response page
	 * @param response
	 * @return
	 */
	private static String getDetails(String response){
		
		if(response.contains("Unable to Process Search Criteria")){
			return null;
		}
		if(response.contains("No Records found")){
			return null;
		}
		
		String resultHtml = "";
		String tempHtml = null;
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);		
			NodeList divs = parser.extractAllNodesThatMatch(new TagNameFilter("div"));
			NodeList propertySearchTabContainerList = divs.extractAllNodesThatMatch(new HasAttributeFilter("id","leftColumn-inner"));
			if(propertySearchTabContainerList.size() > 0) {
				propertySearchTabContainerList = propertySearchTabContainerList.elementAt(0).getChildren();
				Vector<Node> toRemoveIndex = new Vector<Node>();
				for (int i = 0; i < propertySearchTabContainerList.size(); i++) {
					Node n = propertySearchTabContainerList.elementAt(i);
					if (n instanceof HeadingTag || n instanceof TableTag) {
						//keep them
					} else {
						toRemoveIndex.add(n);
					}
				}
				for (Node node : toRemoveIndex) {
					propertySearchTabContainerList.remove(node);
				}
				
				tempHtml = propertySearchTabContainerList.toHtml();
				tempHtml = tempHtml.replaceAll("(?i)\\s+class\\s*=\\s*[\"][^\"]+\"","");
			}
			
			NodeList spanList = divs
					.extractAllNodesThatMatch(new TagNameFilter("span"), true);
			NodeList addressList = spanList.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_BodyContent_lbl_Address"));
			NodeList pinList = spanList.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_BodyContent_lbl_pin"));
			resultHtml = "";
			if(addressList.size() > 0) {
				resultHtml += "<tr><td>Address </td><td><span id=\"idadressa\"> " + addressList.elementAt(0).toPlainTextString() + "</span></td></tr>";
			}
			if(pinList.size() > 0) {
				resultHtml += "<tr><td>Property Index Number</td><td><span id=\"idpin\"> " + pinList.elementAt(0).toPlainTextString() + "</span></td></tr>";
			}
			if(resultHtml.length() > 0) {
				resultHtml = "<h2>Address & PIN</h2><table width=\"600\">" + resultHtml + "</table>";
			}
			
			if(StringUtils.isNotEmpty(tempHtml)) {
				tempHtml = tempHtml.replaceAll("(?is)<a[^>]*>\\s*New\\s+Search\\s*</a>", "");
				tempHtml = tempHtml.replaceAll("(?is)<img[^>]+>", "");
				tempHtml = tempHtml.replaceAll("(?is)</?iframe[^>]*>", "");
				return resultHtml + tempHtml;
			} else {
				return resultHtml;
			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}	
	
	private static int seq = 0;	
	public synchronized static int getSeq(){
		return seq++;
	}
	

}
