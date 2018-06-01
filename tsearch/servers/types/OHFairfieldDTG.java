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
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;

/**
 * @author cristian stochina
 */
public class OHFairfieldDTG extends OHGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public OHFairfieldDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHFairfieldDTG(long searchId) {
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
				if (StringUtils.isNotEmpty(p.getArbDtrct()) && StringUtils.isNotEmpty(p.getArbParcel())){
					if (l.getArbDtrct().equals(p.getArbDtrct()) && l.getArbParcel().equals(p.getArbParcel())){
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
		boolean emptyArbDtrct = StringUtils.isEmpty(str.getArbDtrct());
		boolean emptyArbParcel = StringUtils.isEmpty(str.getArbParcel());
		boolean emptyArbParcelSplit = StringUtils.isEmpty(str.getArbParcelSplit());
		
		return emptyArbDtrct && emptyArbParcel && emptyArbParcelSplit;
	}
	
	@Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
		 TownShipI townShip = null;
			
		 if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 12){
			 subdivision = new Subdivision();
				
			 String subdivisionName = module.getFunction(12).getParamValue().trim();
			 subdivision.setName(subdivisionName);
			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
			 subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			 subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
			 subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
		 }
		 if (module.getModuleIdx() == TSServerInfo.ARB_MODULE_IDX && module.getFunctionCount() > 3){
			 townShip = new TownShip();
			 
			 String arb = module.getFunction(0).getParamValue() + "-" + module.getFunction(1).getParamValue() + "-" +  module.getFunction(2).getParamValue();
			 
			 townShip.setArb(arb);
			
		 }
		 if (subdivision != null){
			 legal = new Legal();
			 legal.setSubdivision(subdivision);
		 }
		 
		 if (townShip != null){
			 if (legal == null){
				 legal = new Legal();
			 }
			 legal.setTownShip(townShip);
		 }
		 
		 return legal;
	}
	
	@Override
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc, DTRecord record){
		
		String instrNoFromAlias = record.getInstrumentNoFromAlias();

		HashMap<String, String> data = new HashMap<String, String>();
		data.put("instrno", instrNoFromAlias);
		data.put("book", record.getBook().replaceFirst("^[A-Z]+", ""));
		data.put("page", record.getPage());
		data.put("year", record.getInstYear());
		data.put("type", doc.getServerDocType());
		
		DocumentI docToCheck = null;
		if (doc != null){
			docToCheck = doc.clone();
			docToCheck.setDocSubType(null);
		}

		return isInstrumentSaved(instrumentNo, docToCheck, data);
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
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
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
		if (image != null && image.getLinks().size() > 0) {
			HashMap<String, String> params = HttpUtils.getParamsFromLink(image.getLink(0));
			if (params != null) {
				String bookPage = params.get("instr");

				String extraInfo = "";
				if (bookPage.matches("\\d+_\\d+")) {
					extraInfo = params.get("extraInstrDesc");
				} else if (bookPage.matches("\\d+")) {
					String year = params.get("year");
					if (StringUtils.isNotEmpty(year)){
						extraInfo = year + "." + bookPage;
					}
				}
					

					if (StringUtils.isNotEmpty(extraInfo) && extraInfo.matches("\\d{4}\\.\\d+")) {
						try {
							ImageI imageForRo = image.clone();
							imageForRo.setContentType("application/pdf");
							String imageForRoPath = imageForRo.getPath();
							String filename = imageForRoPath.substring(imageForRoPath.lastIndexOf("\\") + 1, imageForRoPath.length() - 5) + ".pdf";
							imageForRoPath = imageForRoPath.substring(0, imageForRoPath.lastIndexOf("\\") + 1) + filename;
							imageForRo.setFileName(filename);
							imageForRo.setType(IType.PDF);
							imageForRo.setExtension("pdf");

							long siteId = TSServersFactory.getSiteId(
									getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV),
									getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME),
									"RO");
							OHFairfieldRO ohFairfieldRO = (OHFairfieldRO) TSServersFactory.GetServerInstance((int) siteId, getSearch().getID());

							if (ohFairfieldRO.getDataSite().isEnableSite(getSearch().getCommId())) {
								DownloadImageResult dir = ohFairfieldRO.saveImageFromRO(imageForRo);
								if (dir == null) {
									SearchLogger.info(
											"<br/>Image(searchId=" + searchId + ") book="
													+ instr.getBook() + " page=" + instr.getPage() + " inst=" + extraInfo.substring(5) + " year="
													+ extraInfo.substring(0, 4)
													+ " failed to be taken from RO<br/>", searchId);
								}
								if (dir != null && TSInterface.DownloadImageResult.Status.OK.equals(dir.getStatus())) {

									byte[] b = new byte[0];

									try {
										b = FileUtils.readFileToByteArray(new File(imageForRoPath));
										image.setContentType("application/pdf");
										image.setPath(imageForRoPath);
										image.setFileName(filename);
										image.setType(IType.PDF);
										image.setExtension("pdf");
									} catch (IOException e) {
										logger.error("Failed reading RO image from local file", e);
									}
									if (b.length > 0) {
										afterDownloadImage(true, GWTDataSite.RO_TYPE);
										SearchLogger.info(
												"<br/>Image(searchId=" + searchId + ") book="
														+ instr.getBook() + " page=" + instr.getPage() + " inst=" + extraInfo.substring(5) + " year="
														+ extraInfo.substring(0, 4)
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
