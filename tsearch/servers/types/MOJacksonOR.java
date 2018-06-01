package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.lang.time.DateFormatUtils;
//
//import ro.cst.tsearch.generic.Util;
//import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Radu Bacrau
 */
public class MOJacksonOR extends GenericOrbit {

	private static final long serialVersionUID = 224346344606092458L;
//	private static final Pattern docAvailabilityLinkPattern = Pattern.compile("(?is)<a href=\"([^\"]*)\"[^>]*>Document availability");
//	private static final Pattern certDatePattern = Pattern.compile("(?is)<td>\\s*Jackson\\s*</td>\\s*<td>.*?</td>\\s*<td>\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");

	public MOJacksonOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public MOJacksonOR(long searchId) {
		super(searchId);
	}

	@Override
	protected String retrieveImage(String para, String docNo, String fileName){
		// first try to get it from RO, then from OR
		String newFileName = MOJacksonRO.retrieveImageFromDocNo(docNo, fileName, searchId); 
		if(newFileName!=null){
			return newFileName;
		} else {
			return super.retrieveImage(para, docNo, fileName);
		} 
	}

	/**
	 * Task 8682 - user requested the use of the default certification date
	 */
	/*@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				String page = getLinkContents(dataSite.getLink());
					
				if (StringUtils.isNotEmpty(page)){
					Matcher docAvailabilityLinkMatcher = docAvailabilityLinkPattern.matcher(page);
					
					if(docAvailabilityLinkMatcher.find()) {
						page = getLinkContents(dataSite.getLink() + (dataSite.getLink().endsWith("/") ? "" : "/") + docAvailabilityLinkMatcher.group(1));
						
						if (StringUtils.isNotEmpty(page)){
							Matcher certDateMatcher = certDatePattern.matcher(page);
							
							if(certDateMatcher.find()) {
								String date = certDateMatcher.group(1).trim();
								date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
								
								CertificationDateManager.cacheCertificationDate(dataSite, date);
								getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
							} else {
								CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
							}
						} else {
							CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
						}
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because doc availability link not found");
					}
					
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}*/
	
	@Override
	public InstrumentGenericIterator getInstrumentNumberIterator() {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= 4673988413856389341L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instrNo = state.getInstno().trim();
				if (StringUtils.isNotEmpty(instrNo)) {
					Matcher ma = Pattern.compile("(?i)(\\d{5})([A-Z])").matcher(instrNo);
					if (ma.matches()) {
						int year = state.getYear();
						if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
							instrNo = year + ma.group(2) + StringUtils.leftPad(ma.group(1), 7, '0');
						}
					}
				}
				return instrNo;
			}
			
		};
				
		return instrumentGenericIterator;
	}
	
}
