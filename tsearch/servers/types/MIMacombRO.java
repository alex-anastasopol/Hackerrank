package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.Subdivisions;
import ro.cst.tsearch.database.SingletonOaklandSubdivision;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class MIMacombRO extends MILandaccessGenericRO {

	private static final long serialVersionUID = -8456290597213928232L;

	public MIMacombRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		subdivKey = Subdivisions.DB_MACOMB_SUBDIVISION;
		condoKey = Subdivisions.DB_MACOMB_CONDOMINIUM;
	}

	protected void setupConstants() {
		countyName = "Macomb";
		searchPath = "macomb_mi";
		countyCode = "8025";
		docTypeSelect = DOC_TYPE_SELECT;
		seriesSelect  = SERIES_SELECT;
		appcodeSelectName  = APPCODE_SELECT_NAME;
		appcodeSelectDoc   = APPCODE_SELECT_DOCNO;
		appcodeSelectLiber = APPCODE_SELECT_LIBER;
		appcodeSelectPin   = APPCODE_SELECT_PIN;
		appcodeSelectSubd  = APPCODE_SELECT_SUBD;
		appcodeSelectCond  = APPCODE_SELECT_COND;
		appcodeSelectAssoc = APPCODE_SELECT_ASSOC;
		appcodeSelectAddr = APPCODE_SELECT_ADDR;
		addressSupported = true;
	}

	protected String getSubdivCondoCode(String subdivCondoFullName, int type) {

		String code = "";

		// get all macomb subdivisions that match current name
		Subdivisions[] allMacombSubdiv = SingletonOaklandSubdivision.getInstance().getMacombSubdivision(
				subdivCondoFullName, type);

		// we should have only one subdiv here
		if (allMacombSubdiv.length > 0) {
			code = allMacombSubdiv[0].getCode();
		}

		// just in case we don't, print a warning
		if (allMacombSubdiv.length != 1) {
			logger.info("Macomb RO found subdivisions : " + allMacombSubdiv.length);
		}

		return code;
	}

	private static final String APPCODE_SELECT_NAME = 
	    "<select name=\"appliccode\" size=1>" +
		"<option value='O' selected>Land(Other)</option>" +
		"<option value='U'>UCC</option>" +
		"<option value=' '>All</option>" +                    										
		"</select>";	
	private static final String APPCODE_SELECT_DOCNO = APPCODE_SELECT_NAME;
	private static final String APPCODE_SELECT_LIBER = APPCODE_SELECT_NAME;
	private static final String APPCODE_SELECT_PIN   = APPCODE_SELECT_NAME;
	private static final String APPCODE_SELECT_SUBD  =
		 "<select name=\"appliccode\" size=1>" +
			"<option value='O' selected>Land(Other)</option>" +
			"<option value=' '>All</option>" +                    										
			"</select>";
	private static final String APPCODE_SELECT_COND  = APPCODE_SELECT_SUBD;
	private static final String APPCODE_SELECT_ASSOC = APPCODE_SELECT_NAME;
	private static final String APPCODE_SELECT_ADDR = APPCODE_SELECT_NAME;    
	
	private static final String SERIES_SELECT = 
		"<select name=\"series\" size=1>" +
		"<option value=' ' selected>ALL</option>" +
		"<option value='2'>2:GRANTEE</option>" +
		"<option value='1'>1:GRANTOR</option>" +
		"<option value='C'>C:CREDITOR</option>" + 
		"<option value='D'>D:DEBTOR</option>" +
		"<option value='N'>N:NOTATION</option>" +
		"</select>";	

	private static final String DOC_TYPE_SELECT = 
		"<select name=instrumenttype size=1>"
		+ "<option value=' ' selected>ALL</option>" 
		+ "<option value='A'>A:ASSIGNMENT</option>"
		+ "<option value='ACD'>ACD:AFF CERT DISC NONPMT</option>"
		+ "<option value='ACN'>ACN:AFFID CERT NON PMT</option>"
		+ "<option value='AFF'>AFF:AFFIDAVIT</option>"
		+ "<option value='AFR'>AFR:AFFIDAVIT OF RELEASE</option>"
		+ "<option value='AGR'>AGR:AGREEMENT</option>"
		+ "<option value='AL'>AL:AFFIDAVIT OF LIEN</option>"
		+ "<option value='ALD'>ALD:ATTORNEY LIEN DISCHA</option>"
		+ "<option value='ALR'>ALR:ASSIGN. LEASE/RENTS</option>"
		+ "<option value='ALW'>ALW:ASSIGN LH WORK INT</option>"
		+ "<option value='API'>API:AFFIDAVIT OF PAY.INS</option>"
		+ "<option value='APT'>APT:AFFIDAVIT OF PAY TAX</option>"
		+ "<option value='ARD'>ARD:ASSIGN OF L/R DISCHG</option>"
		+ "<option value='ATL'>ATL:ATTORNEY'S LIEN</option>"
		+ "<option value='ATT'>ATT:ATTACHMENT</option>"
		+ "<option value='BLI'>BLI:BOND LIEN</option>"
		+ "<option value='CAL'>CAL:CONSENT ATTY'S LIEN</option>"
		+ "<option value='CCF'>CCF:CERT CANCEL FORFEIT</option>"
		+ "<option value='CD'>CD:CLERK'S DEED</option>"
		+ "<option value='CFT'>CFT:CERT OF FORFEIT TAX</option>"
		+ "<option value='CIN'>CIN:CLAIM OF INTEREST</option>"
		+ "<option value='CLM'>CLM:MECHANIC LIEN CERT.</option>"
		+ "<option value='COR'>COR:CORRECTION STATEMENT</option>"
		+ "<option value='COR'>COR:CORRECTION</option>"
		+ "<option value='CSO'>CSO:CERT SALE BY OFFICER</option>"
		+ "<option value='CSS'>CSS:CERT SALE BY SHERIFF</option>"
		+ "<option value='D'>D:DEED FEES</option>"
		+ "<option value='DC'>DC:DEED FEES</option>"
		+ "<option value='DEE'>DEE:DEED</option>"
		+ "<option value='DI'>DI:DISCHARG OF INTEREST</option>"
		+ "<option value='DIS'>DIS:DISCHARGE</option>"
		+ "<option value='DOP'>DOP:DECLARATION OF POOL</option>"
		+ "<option value='DOT'>DOT:DECLAR. OF TAKING</option>"
		+ "<option value='DPR'>DPR:DEED PERSONAL REP</option>"
		+ "<option value='DR'>DR:DEATH RECORD</option>"
		+ "<option value='DS'>DS:DEED FEES</option>"
		+ "<option value='DSA'>DSA:DEBT SERVICE AGREEMT</option>"
		+ "<option value='DVC'>DVC:DEED FEES</option>"
		+ "<option value='E'>E:EASEMENT</option>"
		+ "<option value='EAG'>EAG:EASEMENT AGREEMENT</option>"
		+ "<option value='EG'>EG:EASEMENT GRANT</option>"
		+ "<option value='EX'>EX:EXEMPLIF. OF RECORD</option>"
		+ "<option value='FCR'>FCR:FIXTURE FILING CORR</option>"
		+ "<option value='FL'>FL:FEDERAL TAX LIEN</option>"
		+ "<option value='FLD'>FLD:FEDERAL TAX LIEN DIS</option>"
		+ "<option value='FLM'>FLM:FEDERAL TAX LIEN MIS</option>"
		+ "<option value='FLS'>FLS:FEDERAL TAX LIEN SUB</option>"
		+ "<option value='FS'>FS:FINANCIAL STATEMENT</option>"
		+ "<option value='FS'>FS:FINANCING STATEMENT</option>"
		+ "<option value='FSA'>FSA:FINANCIAL STMT ASSIG</option>"
		+ "<option value='FSA'>FSA:FINANCING STM ASSIGN</option>"
		+ "<option value='FSC'>FSC:FINANCIAL STMT CONT</option>"
		+ "<option value='FSC'>FSC:FINANCING STM CONTIN</option>"
		+ "<option value='FSM'>FSM:FINANCIAL STMT AMEND</option>"
		+ "<option value='FSM'>FSM:FINANCING STM AMEND</option>"
		+ "<option value='FSR'>FSR:FINANCIAL STMT REL</option>"
		+ "<option value='FSR'>FSR:FINANCING STM REL</option>"
		+ "<option value='FSS'>FSS:FINANCIAL STMT SUB</option>"
		+ "<option value='FST'>FST:FINANCIAL STMT TERM</option>"
		+ "<option value='FST'>FST:FINANCING STATE TERM</option>"
		+ "<option value='FSW'>FSW:FINANCIAL STMT WAIVE</option>"
		+ "<option value='FTD'>FTD:FEDERAL TAX LIEN DIS</option>"
		+ "<option value='FTL'>FTL:FEDERAL TAX LIEN</option>"
		+ "<option value='FTM'>FTM:FEDERAL TAX LIEN MIS</option>"
		+ "<option value='FTS'>FTS:FEDERAL TAX LIEN SUB</option>"
		+ "<option value='FX'>FX:FIXTURE FILING</option>"
		+ "<option value='FXA'>FXA:FIXTURE FILING ASSIG</option>"
		+ "<option value='FXC'>FXC:FIXTURE FILING CONT</option>"
		+ "<option value='FXC'>FXC:FIXTURE FILING CONTI</option>"
		+ "<option value='FXM'>FXM:FIXTURE FILING AMEND</option>"
		+ "<option value='FXR'>FXR:FIXTURE FILING REL</option>"
		+ "<option value='FXR'>FXR:FIXTURE FILING RELEA</option>"
		+ "<option value='FXS'>FXS:FIXTURE FILING SUB</option>"
		+ "<option value='FXS'>FXS:FIXTURE FILING SUBOR</option>"
		+ "<option value='FXT'>FXT:FIXTURE FILING TERM</option>"
		+ "<option value='FXT'>FXT:FIXTURE FILING TERMI</option>"
		+ "<option value='FXW'>FXW:FIXTURE FILING WAIVE</option>"
		+ "<option value='IFD'>IFD:IND FACILITIES DISC</option>"
		+ "<option value='IFL'>IFL:IND FACILITIES LIEN</option>"
		+ "<option value='INJ'>INJ:INJUNCTION</option>"
		+ "<option value='JD'>JD:JUDGE. OF DIVORCE</option>"
		+ "<option value='JF'>JF:JUDG OF FORECLOSURE</option>"
		+ "<option value='JL'>JL:JUDGEMENT LIEN</option>"
		+ "<option value='JLD'>JLD:JUDGEMENT LIEN DISCH</option>"
		+ "<option value='JT'>JT:JEOPARD TAX FILING</option>"
		+ "<option value='JTT'>JTT:JEOPARD TAX FIL TERM</option>"
		+ "<option value='JUD'>JUD:JUDGMENT</option>"
		+ "<option value='L'>L:LEASE</option>"
		+ "<option value='LA'>LA:LEASE ASSIGNMENT</option>"
		+ "<option value='LAC'>LAC:LAND CORNER</option>"
		+ "<option value='LAM'>LAM:LIEN AMENDMENT</option>"
		+ "<option value='LAS'>LAS:LAND CONT ASSGN SECR</option>"
		+ "<option value='LC'>LC:LAND CONTRACT</option>"
		+ "<option value='LCA'>LCA:LAND CONTRACT ASSIGN</option>"
		+ "<option value='LCM'>LCM:LAND CONTRACT AMEND.</option>"
		+ "<option value='LCS'>LCS:LAND CONRACT SEC/MTG</option>"
		+ "<option value='LD'>LD:LEASE DISCHARGE</option>"
		+ "<option value='LDP'>LDP:LIS PENDENS DISCHRG</option>"
		+ "<option value='LIC'>LIC:LICENSE</option>"
		+ "<option value='LM'>LM:LEASE MEMO.</option>"
		+ "<option value='LN'>LN:LIEN</option>"
		+ "<option value='LNA'>LNA:LIEN ASSIGNMENT</option>"
		+ "<option value='LND'>LND:LIEN DISCHARGE</option>"
		+ "<option value='LNP'>LNP:LIEN NON-PAYMT ASSEM</option>"
		+ "<option value='LP'>LP:LIS PENDENS</option>"
		+ "<option value='LPD'>LPD:LIEN PARTIAL DISCHRG</option>"
		+ "<option value='LSB'>LSB:LEASE SUBORDINATION</option>"
		+ "<option value='LV'>LV:LEVY</option>"
		+ "<option value='LVD'>LVD:LEVY DISCHARGE</option>"
		+ "<option value='M'>M:MORTGAGE</option>"
		+ "<option value='MA'>MA:MORTGAGE ASSIGNMENT</option>"
		+ "<option value='MAG'>MAG:MORTGAGE AGREEMENT</option>"
		+ "<option value='MAM'>MAM:MORTGAGE AMENDMENT</option>"
		+ "<option value='MAS'>MAS:MORTGAGE ASSUMPTION</option>"
		+ "<option value='MD'>MD:MASTER DEED</option>"
		+ "<option value='MDC'>MDC:MASTER DEED CONSENT</option>"
		+ "<option value='MDI'>MDI:MORTGAGE DISCHARGE</option>"
		+ "<option value='MDM'>MDM:MASTER DEED AMENDMNT</option>"
		+ "<option value='MDP'>MDP:MASTER DEED PERMIT</option>"
		+ "<option value='MER'>MER:MERGER</option>"
		+ "<option value='MEX'>MEX:MORTGAGE EXTENSION</option>"
		+ "<option value='MID'>MID:MINERAL DEED</option>"
		+ "<option value='MIS'>MIS:MISCELLANEOUS</option>"
		+ "<option value='ML'>ML:MECHANIC LIEN</option>"
		+ "<option value='MLC'>MLC:LAND CONTRACT MEMO.</option>"
		+ "<option value='MLD'>MLD:MECHANIC LIEN DISCHG</option>"
		+ "<option value='MOD'>MOD:MORTGAGE MODIFICATN</option>"
		+ "<option value='MPD'>MPD:MORTGAGE PART. DISCH</option>"
		+ "<option value='MSB'>MSB:MORTGAGE SUBORDINATN</option>"
		+ "<option value='MTD'>MTD:MESC LIEN DISCHARGE</option>"
		+ "<option value='MTL'>MTL:MESC LIEN</option>"
		+ "<option value='MTM'>MTM:MESC LIEN MISCELLANE</option>"
		+ "<option value='NOC'>NOC:NOTICE OF COMMENCEMT</option>"
		+ "<option value='NOF'>NOF:NOTICE OF FURNISHING</option>"
		+ "<option value='NOT'>NOT:NOTICE</option>"
		+ "<option value='NUL'>NUL:NULL</option>"
		+ "<option value='OAG'>OAG:OPTION AGREEMENT</option>"
		+ "<option value='OGA'>OGA:OIL & GAS LEASE ASSI</option>"
		+ "<option value='OGD'>OGD:OIL & GAS LEASE DISC</option>"
		+ "<option value='OGE'>OGE:OIL & GAS LEASE EXT</option>"
		+ "<option value='OGL'>OGL:OIL & GAS LEASE</option>"
		+ "<option value='OME'>OME:OPTION MEMORANDUM</option>"
		+ "<option value='ORD'>ORD:ORDER</option>"
		+ "<option value='PA'>PA:POWER OF ATTORNEY</option>"
		+ "<option value='PAT'>PAT:PATENT</option>"
		+ "<option value='PJD'>PJD:PART JUDG LIEN DISCH</option>"
		+ "<option value='PL'>PL:PLAT</option>"
		+ "<option value='PRC'>PRC:PARTIAL REDEMPT CERT</option>"
		+ "<option value='PS'>PS:PROOF OF SERVICE</option>"
		+ "<option value='PTD'>PTD:PARTIAL DISCHARGE</option>"
		+ "<option value='QCD'>QCD:QUIT CLAIM DEED</option>"
		+ "<option value='QDS'>QDS:QUIT CLAIM DEED SEC.</option>"
		+ "<option value='RA'>RA:RELEASE ATTACHMENT</option>"
		+ "<option value='RAG'>RAG:REGULATORY AGREEMENT</option>"
		+ "<option value='RAT'>RAT:RATIFICATION</option>"
		+ "<option value='RC'>RC:REDEMPTION CERTIF</option>"
		+ "<option value='REL'>REL:RELEASE</option>"
		+ "<option value='RES'>RES:RESOLUTION</option>"
		+ "<option value='ROW'>ROW:RIGHT OF WAY</option>"
		+ "<option value='RST'>RST:RESTRICTIONS</option>"
		+ "<option value='RWD'>RWD:RIGHT OF WAY DISCHAR</option>"
		+ "<option value='SBA'>SBA:SUBORDINATION AGREE.</option>"
		+ "<option value='SD'>SD:SHERIFF'S DEED</option>"
		+ "<option value='SL'>SL:STATE TAX LIEN</option>"
		+ "<option value='SLD'>SLD:STATE TAX LIEN DISCH</option>"
		+ "<option value='SLM'>SLM:STATE TAX LIEN MISC</option>"
		+ "<option value='SLS'>SLS:STATE TAX LIEN SUB</option>"
		+ "<option value='STD'>STD:STATE TAX LIEN DISCH</option>"
		+ "<option value='STL'>STL:STATE TAX LIEN</option>"
		+ "<option value='STM'>STM:STATE TAX LIEN MISC.</option>"
		+ "<option value='STM'>STM:STATE TAX LIEN MISC</option>"
		+ "<option value='STS'>STS:STATE TAX LIEN SUB.</option>"
		+ "<option value='STS'>STS:STATE TAX LIEN SUB</option>"
		+ "<option value='SUP'>SUP:SUPPLEMENTAL INDEN.</option>"
		+ "<option value='SVC'>SVC:SURVEYOR CERTIFICATE</option>"
		+ "<option value='SWL'>SWL:SEWER LIEN</option>"
		+ "<option value='T'>T:TRUST</option>"
		+ "<option value='TD'>TD:TAX DEED</option>"
		+ "<option value='TDA'>TDA:TAX DAY AFFIDAVIT</option>"
		+ "<option value='TJT'>TJT:JEOPARD TAX FIL TERM</option>"
		+ "<option value='TRC'>TRC:TREAS REDEMP CERT</option>"
		+ "<option value='TRE'>TRE:TREAS CERTIFICATE</option>"
		+ "<option value='WD'>WD:WARRANTY DEED</option>"
		+ "<option value='WDR'>WDR:WAIVER OF DOWER RGHT</option>"
		+ "<option value='WDS'>WDS:WARRANTY DEED SEC.</option>"
		+ "<option value='WIL'>WIL:WILL</option>"
		+ "<option value='WRT'>WRT:WRIT OF RESTITUTION</option>"
		+ "<option value='WVR'>WVR:WAIVER</option>" 
		+ "</select>";

	/**
	 * Get certification date
	 * @return certificationd date in MM/dd/yyyy format, or empty string if certification not found
	 */
	public static String getCertificationDate(){
		try{
			return MILandaccessGenericRO.getCertificationDate("MI", "Macomb", "macomb_mi", "8025");
		}catch(Exception e){
			logger.error(e);
			return "";
		}
	}
	
	/**
	 * Create Already Present filter
	 * @return
	 */
	protected FilterResponse getAlreadyPresentFilter(){
		RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(searchId);
		filter.setUseBookPage(true);
		filter.setUseInstr(true);
		filter.setUseYearInstr(true);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	/**
	 * Create address filter
	 * @return
	 */
	private FilterResponse getAddressFilter(){
		return new AddressFilterResponse2(searchId);
	}
	
	/**
	 * Setup validators
	 * @param m
	 */
	private void setValidators(TSServerInfoModule m){	
		m.addValidator(getAlreadyPresentFilter().getValidator());
		m.addValidator(getIntervalFilter().getValidator());		
		m.addValidator(getLegalFilter().getValidator());		
	}
	
	/**
	 * Setup crossreference validators
	 * @param m
	 */
	private void setCrossValidators(TSServerInfoModule m){
		m.addCrossRefValidator(getAlreadyPresentFilter().getValidator());
		m.addCrossRefValidator(getIntervalFilter().getValidator());
		m.addCrossRefValidator(getLegalFilter().getValidator());
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;
		FilterResponse alreadyPresentFilter = getAlreadyPresentFilter();
		// PIN Search
		String pid = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if (!StringUtils.isEmpty(pid)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.addFilter(getAlreadyPresentFilter());
			setValidators(m);
			setCrossValidators(m);
			l.add(m);
		}
		
		// Address Search
		boolean hasStrName = !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREETNAME));
		boolean hasStrNo = !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREETNO));
		if(hasStrName && hasStrNo){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			// do not use city
			m.getFunction(4).setSaKey("");
			m.getFunction(4).forceValue("");
			m.addFilter(getAddressFilter());
			m.addFilter(getAlreadyPresentFilter());
			setValidators(m);
			setCrossValidators(m);			
			l.add(m);
		}
		
		FilterResponse ownerNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		
		// Owner Search
		ConfigurableNameIterator nameIterator = null;
		String grantorLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		if (!StringUtils.isEmpty(grantorLast)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.getFunction(19).setDefaultValue("25"); // 25 results at a time			
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));			
			m.addFilter(ownerNameFilter);	
			m.addFilter(alreadyPresentFilter);
			addFilterForUpdate(m, true);
			
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());
			
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
			m.addIterator( nameIterator);
			l.add(m);
		}	

		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.getFunction(19).setDefaultValue("25"); // do not retrieve too many results in automatic
			m.getFunction(0).setSaKey("");
			m.getFunction(1).setSaKey("");
			m.getFunction(2).setSaKey("");
			m.getFunction(3).setSaKey("");
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(3,FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);					
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));
			m.addFilter(ownerNameFilter);
			m.addFilter(alreadyPresentFilter);
			addFilterForUpdate(m, true);
			
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"},true);
			m.addIterator( nameIterator);
			l.add(m);
		}
		
		// Search by book page list search from AO
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
		m.setSaObjKey(SearchAttributes.INSTR_LIST);		
		m.getFunction(0).setSaKey("");
		m.getFunction(1).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		setValidators(m);
		setCrossValidators(m);
		l.add(m);

		// Search by book and page list from search page
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));		
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN);		
		m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
		m.getFunction(0).setSaKey("");
		m.getFunction(1).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);		
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);				
		setValidators(m);
		setCrossValidators(m);
		l.add(m);

		// Search by cross ref book and page list from ro docs
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
		m.getFunction(0).setSaKey("");
		m.getFunction(1).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		setValidators(m);
		setCrossValidators(m);
		l.add(m);
		
        //OCR last transfer - book / page search
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
        m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setSaKey("");
		m.getFunction(1).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		setValidators(m);
		setCrossValidators(m);		
		l.add(m);
		
		
		ArrayList< NameI> searchedNames = null;
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));		
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.getFunction(19).setDefaultValue("25"); // 25 results at a time	
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));		
			m.addFilter(ownerNameFilter);	
			m.addFilter(alreadyPresentFilter);
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());

			if( nameIterator!=null ){
				searchedNames = nameIterator.getSearchedNames();
			}
			else
			{
				searchedNames = new ArrayList<NameI>();
			}
				
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
			//get your values at runtime
			nameIterator.setInitAgain( true );
			//
			nameIterator.setSearchedNames( searchedNames );
			m.addIterator( nameIterator ); 
			l.add(m);
		}
		
		{
		
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.getFunction(19).setDefaultValue("25"); // do not retrieve too many results in automatic
			m.getFunction(0).setSaKey("");
			m.getFunction(1).setSaKey("");
			m.getFunction(2).setSaKey("");
			m.getFunction(3).setSaKey("");
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(3,FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);					
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));
			m.addFilter(ownerNameFilter);
			m.addFilter(alreadyPresentFilter);
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"},true);
			//get your values at runtime
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
			m.addIterator( nameIterator ); 
	        l.add(m);
		
		}
        
		// !!! NOTE: we do not search by instrument number, because they are re-used each year
		
		// set the list of search modules
		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
			
		
		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		     String date=gbm.getDateForSearch(id,"MM/dd/yyyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(8).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				 date=gbm.getDateForSearchBrokenChain(id,"MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(8).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
				 module.addValidator( pinValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			
	
			 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
		     		     
	
	
		}
		
	
	
	}
