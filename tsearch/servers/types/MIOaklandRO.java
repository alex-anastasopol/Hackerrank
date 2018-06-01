package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.OaklandSubdivisions;
import ro.cst.tsearch.database.SingletonOaklandSubdivision;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
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

public class MIOaklandRO extends MILandaccessGenericRO {

	private static final long serialVersionUID = -6820490718185787352L;

	public MIOaklandRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		subdivKey = OaklandSubdivisions.DB_OAKLAND_SUBDIVISION;
		condoKey = OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM;
	}

	protected void setupConstants() {
		countyName = "Oakland";
		searchPath = "oakland_mi";
		countyCode = "8039";
		docTypeSelect = DOC_TYPE_SELECT;
		seriesSelect  = SERIES_SELECT;
		appcodeSelectName  = APPCODE_SELECT_NAME;
		appcodeSelectDoc   = APPCODE_SELECT_DOCNO;
		appcodeSelectLiber = APPCODE_SELECT_LIBER;
		appcodeSelectPin   = APPCODE_SELECT_PIN;
		appcodeSelectSubd  = APPCODE_SELECT_SUBD;
		appcodeSelectCond  = APPCODE_SELECT_COND;
		appcodeSelectAssoc = APPCODE_SELECT_ASSOC;
		addressSupported = false;
	}

	protected String getSubdivCondoCode(String subdivCondoFullName, int type) {
		String code = "";

		// get all oakland subdivisions that match current name
		OaklandSubdivisions[] allOaklandSubdiv = SingletonOaklandSubdivision.getInstance().getSubdivision(
				subdivCondoFullName, type);

		// we should have only one subdiv here
		if (allOaklandSubdiv.length > 0) {
			code = allOaklandSubdiv[0].getCode();
		}

		// just in case we don't, print a warning
		if (allOaklandSubdiv.length != 1) {
			logger.info("Oakland RO found subdivisions : " + allOaklandSubdiv.length);
		}

		return code;
	}

	private static final String APPCODE_SELECT_NAME = 
	    "<select name=\"appliccode\" size=1>" +
		"<option value='O' selected>Land(Other)</option>" +
		"<option value='U'>UCC</option>" +
		"<option value='J'>Judgment</option>" +
		"<option value='C'>Court</option>" +
		"<option value=' '>All</option>" +                    										
		"</select>";	
	private static final String APPCODE_SELECT_DOCNO = 
	    "<select name=\"appliccode\" size=1>" +
		"<option value='O' selected>Land(Other)</option>" +
		"<option value='U'>UCC</option>" +
		"<option value='J'>Judgment</option>" +
		"<option value=' '>All</option>" +                    										
		"</select>";			
	private static final String APPCODE_SELECT_LIBER =
	    "<select name=\"appliccode\" size=1>" +
		"<option value='O' selected>Land(Other)</option>" +
		"<option value='U'>UCC</option>" +
		"<option value=' '>All</option>" +                    										
		"</select>";
	private static final String APPCODE_SELECT_PIN   = APPCODE_SELECT_NAME;
	private static final String APPCODE_SELECT_SUBD  =
		 "<select name=\"appliccode\" size=1>" +
			"<option value='O' selected>Land(Other)</option>" +
			"<option value=' '>All</option>" +                    										
			"</select>";
	private static final String APPCODE_SELECT_COND  = APPCODE_SELECT_SUBD;
	private static final String APPCODE_SELECT_ASSOC = 
	    "<select name=\"appliccode\" size=1>" +
		"<option value='O' selected>Land(Other)</option>" +
		"<option value='U'>UCC</option>" +                    										
		"</select>";
		
	private static final String SERIES_SELECT = 
		"<select name=\"series\" size=1><option value=' ' selected>ALL</option>" +
		"<option value='2'>2:GRANTEE - LAND</option>" +
		"<option value='1'>1:GRANTOR - LAND</option>" +
		"<option value='L'>L:CREDITOR - UCC</option>" + 
		"<option value='D'>D:DEBTOR - UCC</option>" +										
		"</select>";	

	private static final String DOC_TYPE_SELECT = "<select name=instrumenttype size=1>"
		+ "<option value=' ' selected>ALL </option>"
		+ "<option value='AAB'>AAB:AFF OF ABANDONMENT</option>"
		+ "<option value='AAG'>AAG:ASMT OF AGREEMENT</option>"
		+ "<option value='AAL'>AAL:AMEND OF ASMT LEASE</option>"
		+ "<option value='AAS'>AAS:ASSIGNMENT OF ASSIGN</option>"
		+ "<option value='ABY'>ABY:AMEND OR AMEN BY LAW</option>"
		+ "<option value='ACL'>ACL:COLLATERAL ASSIGNMNT</option>"
		+ "<option value='ADD'>ADD:ADDENDUM TO MTG, ETC</option>"
		+ "<option value='ADL'>ADL:AMEND ANY TYPE LIEN</option>"
		+ "<option value='ADM'>ADM:ADMINISTRATOR'S DEED</option>"
		+ "<option value='ADV'>ADV:AGN OF DEVELOPER RGH</option>"
		+ "<option value='AER'>AER:AFF DOC REC IN ERROR</option>"
		+ "<option value='AFD'>AFD:AFF.LOST/DESTR DOC</option>"
		+ "<option value='AFF'>AFF:AFFIDAVIT</option>"
		+ "<option value='AFN'>AFN:AMEND OF FIN STMT</option>"
		+ "<option value='AFR'>AFR:AFFT OF FOREFITURE</option>"
		+ "<option value='AGR'>AGR:AGREEMENT-ANY TYPE</option>"
		+ "<option value='AIN'>AIN:ASG OF INTEREST</option>"
		+ "<option value='ALC'>ALC:AMEND TO LAND CONT</option>"
		+ "<option value='ALN'>ALN:ASMT OF LIEN-NOT MTG</option>"
		+ "<option value='ALP'>ALP:AMEN.OF LIS PENDENS</option>"
		+ "<option value='ALS'>ALS:AMEND TO LEASE</option>"
		+ "<option value='AMA'>AMA:AMEND OF AGREEMENT</option>"
		+ "<option value='AMD'>AMD:AMEND OF MASTER DEED</option>"
		+ "<option value='AMD'>AMD:UCC AMENDMENT</option>"
		+ "<option value='AMF'>AMF:AFF.MTG/DEED FRAUD</option>"
		+ "<option value='AMG'>AMG:AMEND MORTGAGE</option>"
		+ "<option value='AML'>AML:AMEND OF CLAIM/LIEN</option>"
		+ "<option value='AMR'>AMR:AMEND RESTRICTIONS</option>"
		+ "<option value='AMT'>AMT:AMN ST OF MI TX LIEN</option>"
		+ "<option value='ANC'>ANC:AMEN OF NOTICE COMM</option>"
		+ "<option value='ANL'>ANL:ASSOCIATION LIEN</option>"
		+ "<option value='AOL'>AOL:ASSIGN OF OIL LEASE</option>"
		+ "<option value='AOP'>AOP:ASG OF OPTION AGREE</option>"
		+ "<option value='APA'>APA:AMN OF PURCH ASSN LN</option>"
		+ "<option value='API'>API:AFFT OF PAY OF INSUR</option>"
		+ "<option value='APT'>APT:AFFT OF PAY OF TAXES</option>"
		+ "<option value='ARE'>ARE:ASSIGNMENT OF RENTS</option>"
		+ "<option value='ASA'>ASA:AMEN OF SELLER ASMT</option>"
		+ "<option value='ASD'>ASD:ASG OF SHERIFFS DEED</option>"
		+ "<option value='ASF'>ASF:ASG OF FIN STMT</option>"
		+ "<option value='ASL'>ASL:ASSIGNMENT OF LEASE</option>"
		+ "<option value='ASM'>ASM:ASSUM AGREE OF MTG</option>"
		+ "<option value='ASN'>ASN:UCC ASSIGNMENT</option>"
		+ "<option value='AST'>AST:ASMT OF MORTGAGE</option>"
		+ "<option value='ASV'>ASV:SURVEYOR'S AFFIDAVIT</option>"
		+ "<option value='ATL'>ATL:ATTORNEY LIEN</option>"
		+ "<option value='AUT'>AUT:AMEN.U S TAX LIEN</option>"
		+ "<option value='CBS'>CBS:CER SHER SALE OF RE</option>"
		+ "<option value='CDD'>CDD:CLERK'S DEED</option>"
		+ "<option value='CDS'>CDS:CER OF DIS PROP FTL</option>"
		+ "<option value='CER'>CER:CERT OF ERROR</option>"
		+ "<option value='CES'>CES:CERT/EXECUTION SALE</option>"
		+ "<option value='CIN'>CIN:CLAIM OF INTEREST</option>"
		+ "<option value='CLN'>CLN:CLAIM OF LIEN</option>"
		+ "<option value='CMD'>CMD:CONS MAST DEED</option>"
		+ "<option value='CNA'>CNA:CERT OF NON ATTACH</option>"
		+ "<option value='CNJ'>CNJ:CONSENT JUDGMENT</option>"
		+ "<option value='CON'>CON:CONSENT</option>"
		+ "<option value='CON'>CON:UCC - CONTINUATION</option>"
		+ "<option value='CRD'>CRD:CERT OF REDEMPTION</option>"
		+ "<option value='CRL'>CRL:CERT OF SPEC REL RP</option>"
		+ "<option value='CSB'>CSB:CERT OF SUBORD TX LN</option>"
		+ "<option value='CSL'>CSL:CERT SALE SIEZED PRO</option>"
		+ "<option value='CST'>CST:CONTINUATION STMT</option>"
		+ "<option value='CTR'>CTR:CERT OF TRUST AGRMNT</option>"
		+ "<option value='CWT'>CWT:CERT OF WITHDRAWAL</option>"
		+ "<option value='DAC'>DAC:DIS/ASMT LAND/CONT.</option>"
		+ "<option value='DAF'>DAF:DIS.OF AFFIDAVIT</option>"
		+ "<option value='DAG'>DAG:DISCHARGE OF AGRMNT</option>"
		+ "<option value='DAL'>DAL:DIS OF AST OF LEASE</option>"
		+ "<option value='DAR'>DAR:DIS OF ASG OF RENTS</option>"
		+ "<option value='DAS'>DAS:DIS OF ASSOC LIEN</option>"
		+ "<option value='DAT'>DAT:DIS OF ATTORNEY LIEN</option>"
		+ "<option value='DCI'>DCI:DIS OF CLAIM OF INTR</option>"
		+ "<option value='DCP'>DCP:DECLARATION OF POOL</option>"
		+ "<option value='DCT'>DCT:DECL. OF TAKING</option>"
		+ "<option value='DEC'>DEC:DECLARATION</option>"
		+ "<option value='DED'>DED:DEED</option>"
		+ "<option value='DES'>DES:DIS. OF EASEMENT</option>"
		+ "<option value='DIS'>DIS:DIS, SAT, RELEASE</option>"
		+ "<option value='DIV'>DIV:DIVORCE</option>"
		+ "<option value='DJM'>DJM:DIS. OF JUDGMENT</option>"
		+ "<option value='DLC'>DLC:DEED IN PUR OF LN CN</option>"
		+ "<option value='DLN'>DLN:DISCHARGE OF LIEN</option>"
		+ "<option value='DLP'>DLP:DIS OF LIS PENDENS</option>"
		+ "<option value='DLS'>DLS:DISCHARGE OF LEASE</option>"
		+ "<option value='DLV'>DLV:RELEASE OF LEVY</option>"
		+ "<option value='DMI'>DMI:DIS OF MI TAX LIEN</option>"
		+ "<option value='DML'>DML:DIS OF CLAIM OF LIEN</option>"
		+ "<option value='DMO'>DMO:DIS.MEM.OPTION</option>"
		+ "<option value='DMT'>DMT:DIS. OF ASSIGNMENT</option>"
		+ "<option value='DOL'>DOL:DISCHARGE OF OIL LEA</option>"
		+ "<option value='DOP'>DOP:DIS.OFFER/PURCHASE</option>"
		+ "<option value='DPA'>DPA:DIS.OF POWER OF ATTY</option>"
		+ "<option value='DPI'>DPI:DIS.OF AOPIILC</option>"
		+ "<option value='DRF'>DRF:DIS.RT.FIRST REFUSAL</option>"
		+ "<option value='DRW'>DRW:DIS.RIGHT OF WAY</option>"
		+ "<option value='DSI'>DSI:DIS.OF AOSIILC</option>"
		+ "<option value='DTH'>DTH:DEATH CERTIFICATE</option>"
		+ "<option value='DUS'>DUS:DIS OF US T LIEN</option>"
		+ "<option value='ELC'>ELC:EXT OF LAND CONTRACT</option>"
		+ "<option value='EST'>EST:EASEMENT</option>"
		+ "<option value='ESY'>ESY:VACATION OF EASEMENT</option>"
		+ "<option value='EXD'>EXD:EXECUTOR'S DEED</option>"
		+ "<option value='EXE'>EXE:EXECUTION</option>"
		+ "<option value='FST'>FST:FINANCE STATEMENT</option>"
		+ "<option value='FTL'>FTL:FEDERAL TAX LIEN</option>"
		+ "<option value='INV'>INV:INVENTORY (PROBATE)</option>"
		+ "<option value='JGM'>JGM:JUDGMENT</option>"
		+ "<option value='JLN'>JLN:JUDGMENT LIEN</option>"
		+ "<option value='JTA'>JTA:JEOPARDY TAX ASSESS</option>"
		+ "<option value='LAC'>LAC:LAND CONTRACT</option>"
		+ "<option value='LCT'>LCT:MECHANICS LIEN CERT</option>"
		+ "<option value='LDC'>LDC:LAND CORNERS CERT.</option>"
		+ "<option value='LET'>LET:LETTERS OF AUTHORITY</option>"
		+ "<option value='LEV'>LEV:LEVY OF EXECUTION</option>"
		+ "<option value='LIN'>LIN:LIEN (MISC.)</option>"
		+ "<option value='LSE'>LSE:LEASE</option>"
		+ "<option value='LSP'>LSP:LIS PENDENS</option>"
		+ "<option value='MIS'>MIS:MISC DOCUMENT</option>"
		+ "<option value='MIT'>MIT:STATE MI TAX LIEN</option>"
		+ "<option value='MLC'>MLC:MEMO OF LAND CONTRCT</option>"
		+ "<option value='MLN'>MLN:MECHANICS LIEN</option>"
		+ "<option value='MLS'>MLS:MEMO OF LEASE</option>"
		+ "<option value='MMG'>MMG:MOD. OF MORTGAGE</option>"
		+ "<option value='MOL'>MOL:MOD OF LAND CONTRACT</option>"
		+ "<option value='MSD'>MSD:MASTER DEED</option>"
		+ "<option value='MTG'>MTG:MORTGAGE</option>"
		+ "<option value='MTL'>MTL:MICHIGAN TAX LIEN</option>"
		+ "<option value='NBP'>NBP:NBPCTUTD</option>"
		+ "<option value='NCM'>NCM:NOT OF COMMENCEMENT</option>"
		+ "<option value='NDF'>NDF:NOTICE OF DEFAULT</option>"
		+ "<option value='NDX'>NDX:NOT INDEXED</option>"
		+ "<option value='NOT'>NOT:NOTICE OF (MISC.)</option>"
		+ "<option value='OLL'>OLL:OIL LEASE</option>"
		+ "<option value='OPT'>OPT:OPT.PURCH.MEM OF OPT</option>"
		+ "<option value='ORD'>ORD:ORDER</option>"
		+ "<option value='PAT'>PAT:POWER OF ATTORNEY</option>"
		+ "<option value='PDA'>PDA:PART DIS OF AGRMNT</option>"
		+ "<option value='PDF'>PDF:PART DIS FIN STMT</option>"
		+ "<option value='PDL'>PDL:PART DIS MECH LIEN</option>"
		+ "<option value='PDM'>PDM:PART DIS OF MORTGAGE</option>"
		+ "<option value='PDT'>PDT:PART DIS OF TAX LIEN</option>"
		+ "<option value='PLC'>PLC:ASG PURC ITR LND CN</option>"
		+ "<option value='PLT'>PLT:PLAT</option>"
		+ "<option value='PNH'>PNH:PROOF NOTICE/HEARING</option>"
		+ "<option value='PRD'>PRD:PERSONAL REPRES DEED</option>"
		+ "<option value='PTD'>PTD:PARTIAL DISCHARGE</option>"
		+ "<option value='PTL'>PTL:UCC PARTIAL RELEASE</option>"
		+ "<option value='QCD'>QCD:QUIT CLAIM DEED</option>"
		+ "<option value='QDC'>QDC:QCD (D/C ATTACHED.)</option>"
		+ "<option value='QDP'>QDP:QCD (POA ATTACHED.)</option>"
		+ "<option value='RDR'>RDR:MORTGAGE RIDER</option>"
		+ "<option value='RED'>RED:REDEMPTION RECEIPT</option>"
		+ "<option value='REL'>REL:RELEASE OF ANYTHING</option>"
		+ "<option value='RES'>RES:RESOLUTION</option>"
		+ "<option value='RIM'>RIM:REIN.MTG.DIS/INERROR</option>"
		+ "<option value='RLF'>RLF:REINST LAPSED FILING</option>"
		+ "<option value='RRF'>RRF:RIGHT OF 1ST REFUSAL</option>"
		+ "<option value='RRW'>RRW:RELEASE OF R/WAY</option>"
		+ "<option value='RST'>RST:RESTRICTIONS</option>"
		+ "<option value='RTF'>RTF:REIN.TREAS.FORFEIT.</option>"
		+ "<option value='RVT'>RVT:REVOCATION OF TRUST</option>"
		+ "<option value='RWY'>RWY:RIGHT OF WAY</option>"
		+ "<option value='SBA'>SBA:SUBORDINATION AGMT</option>"
		+ "<option value='SBL'>SBL:SUBORD ANY KIND LIEN</option>"
		+ "<option value='SBM'>SBM:SUBORDINATION MTG</option>"
		+ "<option value='SDD'>SDD:SHERIFF'S DEED</option>"
		+ "<option value='SLC'>SLC:ASG SLR INR IN LN CN</option>"
		+ "<option value='SRV'>SRV:SURVEY</option>"
		+ "<option value='TDD'>TDD:TAX DEED</option>"
		+ "<option value='TMD'>TMD:TERM OF MASTER DEED</option>"
		+ "<option value='TNC'>TNC:TERM NOT OF COMMENCE</option>"
		+ "<option value='TRD'>TRD:TRUSTEES DEED</option>"
		+ "<option value='TRM'>TRM:TERMINATION STMT</option>"
		+ "<option value='TRM'>TRM:UCC TERMINATION</option>"
		+ "<option value='TRS'>TRS:MEMO OF TRUST</option>"
		+ "<option value='TTR'>TTR:TERM.OF TRUST</option>"
		+ "<option value='UCC'>UCC:UCC - FINANCING STMT</option>"
		+ "<option value='UST'>UST:FEDERAL TAX LIEN</option>"
		+ "<option value='VAC'>VAC:VACATION</option>"
		+ "<option value='WDC'>WDC:WD WITH D/C ATTACHED</option>"
		+ "<option value='WDD'>WDD:WARRANTY DEED</option>"
		+ "<option value='WDP'>WDP:WD W/POA ATTACHED.</option>"
		+ "<option value='WRR'>WRR:WRIT OF RESTITUTION</option>"
		+ "<option value='WTL'>WTL:WITHDRAWAL T/LIEN</option>"
		+ "<option value='WVR'>WVR:WVR OF DOWER RIGHTS</option>"
		+ "<option value='65Q'>65Q:DIS.OF AFFIDAVIT</option>" 
		+ "</select>";

	/**
	 * Get certification date
	 * @return certificationd date in MM/dd/yyyy format, or empty string if certification not found
	 */
	public static String getCertificationDate(){
		try{
			return MILandaccessGenericRO.getCertificationDate("MI", "Oakland", "oakland_mi", "8039");
		}catch(Exception e){
			logger.error(e);
			return "";
		}
	}
	
	private void setValidators(TSServerInfoModule m){	
		m.addValidator(getAlreadyPresentFilter().getValidator());
		m.addValidator(getIntervalFilter().getValidator());		
		m.addValidator(getLegalFilter().getValidator());		
	}
	
	private void setCrossValidators(TSServerInfoModule m){
		m.addCrossRefValidator(getAlreadyPresentFilter().getValidator());
		m.addCrossRefValidator(getIntervalFilter().getValidator());
		m.addCrossRefValidator(getLegalFilter().getValidator());
		m.addCrossRefValidator(getPinFilter().getValidator());
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
		
		FilterResponse ownerNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		
		// Owner search
		String grantorLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		ConfigurableNameIterator nameIterator = null;
		if (!StringUtils.isEmpty(grantorLast)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));	
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.getFunction(19).setDefaultValue("25"); // 25 results at a time			
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));			
			m.addFilter(ownerNameFilter);	
			
			m.addValidator(getPinFilter().getValidator());
			m.addFilter(alreadyPresentFilter);
			
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			addFilterForUpdate(m, false);
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());
			
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
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
		m.addValidator(getPinFilter().getValidator());
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
		m.addValidator(getPinFilter().getValidator());
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
		m.addValidator(getPinFilter().getValidator());
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
		m.addValidator(getPinFilter().getValidator());
		setCrossValidators(m);		
		l.add(m);	
		
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator intervalNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.getFunction(19).setDefaultValue("25"); // 25 results at a time			
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));			
			m.addFilter(ownerNameFilter);
			m.addValidator(getPinFilter().getValidator());
			
			m.addFilter(alreadyPresentFilter);
			
			m.addValidator(alreadyPresentFilter.getValidator());
			m.addValidator(intervalNameValidator);		
			m.addValidator(getLegalFilter().getValidator());
			m.addCrossRefValidator(alreadyPresentFilter.getValidator());
			m.addCrossRefValidator(intervalNameValidator);
			m.addCrossRefValidator(getLegalFilter().getValidator());
			
			ArrayList< NameI> searchedNames = null;
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

		
		// set the list of search modules
		serverInfo.setModulesForAutoSearch(l);
	}

}
