package ro.cst.tsearch.search.filter.newfilters.pin;

import java.math.BigDecimal;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;

public class PINRejectALL extends PinFilterResponse {

	private long miServerID = -1;
	private static final long serialVersionUID = -3357476032339987361L;

	public PINRejectALL(String saKey, long searchId, long miServerID) {
		super(saKey, searchId);
		threshold = new BigDecimal("0.90");
		this.miServerID = miServerID ;
	}
	
	@SuppressWarnings("unchecked")
	public void computeScores(Vector rows)
	{
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCurrentCommunity().getID().intValue(),
				miServerID);
		String siteName =  dat.getName();
		Search search = currentInstance.getCrtSearchContext();
		search.setAdditionalInfo(search.getID() + siteName + miServerID + ":" + "MAKE_OTHER_PIN_SEARCH", Boolean.TRUE);
		boolean rejectAll = false;
		
		for (int i = 0; i < rows.size(); i++)
		{
            IndividualLogger.info("Processing result " + i + " of total " + rows.size(),searchId);
			ParsedResponse row = (ParsedResponse)rows.get(i);
			BigDecimal score = null;
			if(rows.size() == 1 && isSkipUnique()){
				score = ATSDecimalNumberFormat.ONE;
			} else if(rows.size() > getMinRowsToActivate()){
				score = getScoreOneRow(row);
			} else {
				score = ATSDecimalNumberFormat.ONE;
			}
			if(	score.compareTo(threshold)<0 ){
				rejectAll = true;
				break;
			}
			scores.put(row.getResponse(), score);
			if (score.compareTo(bestScore) > 0)
			{
				bestScore = score;
			}
			IndividualLogger.info("ROW SCORE:" + score,searchId);
			logger.debug("\n\n ROW SCORE : [" + score + "]\nROW HTML: [" + row.getResponse() + "]\n");
		}
		
		if(rejectAll){
			for (int i = 0; i < rows.size(); i++)
			{
				ParsedResponse row = (ParsedResponse)rows.get(i);
				scores.put(row.getResponse(), ATSDecimalNumberFormat.ZERO);
			}
			search.setAdditionalInfo(search.getID() + siteName + miServerID + ":" + "MAKE_OTHER_PIN_SEARCH", Boolean.TRUE);
		}
		else{
			search.setAdditionalInfo(search.getID() + siteName + miServerID + ":" + "MAKE_OTHER_PIN_SEARCH", Boolean.FALSE);
		}
	}
	
	@Override
	public String getFilterCriteria(){
		return "PIN=" + getRefPin()+ " reject ALL when one row do not pass";
	}
	
}
