package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;

/**
 * @author cristian stochina
 */
public class OHDelawareDTG extends OHGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public OHDelawareDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHDelawareDTG(long searchId) {
		super(searchId);
	}

	@Override
	protected boolean isCompleteLegal(LegalStructDTG ret1, int qo, String qv, int siteType, String stateAbbrev){
		return (StringUtils.isNotEmpty(ret1.getArbDtrct()) 
				&& StringUtils.isNotEmpty(ret1.getArbParcel()) 
				&& StringUtils.isNotEmpty(ret1.getArbParcelSplit()));
	}
	
	@Override
	protected boolean testIfExist(Set<LegalStructDTG> legalStruct2, LegalStructDTG l,long searchId) {
		
		if (ARB_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p : legalStruct2){
				if (StringUtils.isNotEmpty(p.getArb())){
					if (l.getArb().equals(p.getArb())){
						return true;
					}
				}
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p:legalStruct2){
				if (p.isPlated()){
					if (l.equalsSubdivided(p)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	protected boolean emptyArb(LegalStructDTG str){
		boolean emptyArb = StringUtils.isEmpty(str.getArb());
		
		return emptyArb;
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;
			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (instno.length() > 0 && instno.length() == 7){
					instno = StringUtils.leftPad(instno, 8, "0");
				}
				return instno;
			}
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				return state.getInstno().trim();
			}
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}

		return instrumentGenericIterator;
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate) {
		if ( inst.hasInstrNo() ){ 
			String instr = StringUtils.defaultString(inst.getInstno());
			
			if(instr.length()>0 && instr.length()==7){
				instr = "0"+instr;
			}
			
			
			String year1 = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year1)){
				searched.add(instr+year1);
			}else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			module.setData(1, inst.getYear()+"");
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	

	@Override
	public DownloadImageResult downloadedFromOtherSite(ImageI image, InstrumentI instr) {
		if (image != null && image.getLinks().size() > 0){
			HashMap<String, String> params = HttpUtils.getParamsFromLink(image.getLink(0));
			if (params != null) {
				String docNumber = params.get("instr");
				String instrument = "";
				
				if (docNumber.matches("\\d+_\\d+")) {
					String extraInfo = params.get("extraInstrDesc");
					if (extraInfo != null && extraInfo.matches("\\d{4}\\.\\d+")) {
						instrument = extraInfo.substring(0,4) + StringUtils.leftPad(extraInfo.substring(5), 8, "0");
					}
				} else if (docNumber.matches("\\d+")) {
					String year = params.get("year");
					if (StringUtils.isNotEmpty(year)){
						instrument = year + StringUtils.leftPad(docNumber, 8, "0");
					}
				}
				
				if (StringUtils.isNotEmpty(instrument)){
					try {					    	
						ImageI imageForRo = image.clone();
					    		
						long siteId = TSServersFactory.getSiteId(
								getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
								getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
								"RO");
						OHDelawareRO ohDelawareRo = (OHDelawareRO)TSServersFactory.GetServerInstance((int)siteId, getSearch().getID());
								
						if( ohDelawareRo.getDataSite().isEnableSite(getSearch().getCommId())) {
							DownloadImageResult dir = ohDelawareRo.saveImageFromRO(imageForRo, instrument);
							if (dir == null){
								SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
										+ instr.getBook() + " page=" + instr.getPage() + " inst=" +  instrument.substring(5) + " year=" + instrument.substring(0,4)
										+ " failed to be taken from RO<br/>", searchId);
							}
							if (dir != null && TSInterface.DownloadImageResult.Status.OK.equals(dir.getStatus())){
								byte[] b = new byte[0];
								try {
									b = FileUtils.readFileToByteArray(new File(image.getPath()));
								} catch (IOException e) {
									logger.error("Failed reading RO image from local file", e);
								}
								if (b.length > 0){
									afterDownloadImage(true, GWTDataSite.RO_TYPE);
									SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
													+ instr.getBook() + " page=" + instr.getPage() + " inst=" +  instrument.substring(5) + " year=" + instrument.substring(0,4)
													+ " was taken from RO<br/>", searchId);
									return dir;
								}
							}
						}
					} catch (Exception e) {
						logger.error("Something happened while trying to get image from OHDelawareRO while on OHDelawareDG", e);
					}
				}
			}
		}

		return new DownloadImageResult();
	}

	
}
