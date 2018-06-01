package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;

import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

public class SynonimNameInfoPinCandidateFilter extends SynonimNameFilter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SynonimNameInfoPinCandidateFilter(String key, long searchId,
			boolean useSubdivisionName, TSServerInfoModule module,
			boolean ignoreSuffix, int stringCleaner) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix, stringCleaner);
	}
	
	public SynonimNameInfoPinCandidateFilter(String key, long searchId,
			boolean useSubdivisionName, TSServerInfoModule module,
			boolean ignoreSuffix) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix);
	}
	
	public SynonimNameInfoPinCandidateFilter(long searchId){
		super(searchId);
	}
	
	public SynonimNameInfoPinCandidateFilter(String key, long searchId) {
		super(key, searchId);
	}
	
	@Override
    public String getFilterName(){
    	return "Filter " + ((pondereMiddle==0.0)?"(ignoring middle name) ":"") + "by Synonim Name Ignoring Middle When Empty Reference And Candidate Has No Pin";
    }
	
	@Override
	public String getFilterCriteria(){
    	return "Name='" + getReferenceNameString() + "'" + ((pondereMiddle==0.0)?"(ignoring middle name)":"");
    }
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		DocumentI document = row.getDocument();
		boolean ignoreMiddle = isIgnoreMiddleOnEmpty();;
		if(document != null && !document.getProperties().isEmpty()) {
			PropertyI property = document.getProperties().iterator().next();
			if(property != null) {
				if(!property.hasPin()) {
					try {
						setIgnoreMiddleOnEmpty(true);
						return super.getScoreOneRow(row);
					} catch (Throwable t) {
						// TODO: handle exception
					} finally {
						setIgnoreMiddleOnEmpty(ignoreMiddle);
					}
				}
			}
		}
		return super.getScoreOneRow(row);
	}
}
