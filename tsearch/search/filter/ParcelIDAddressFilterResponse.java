package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

import static ro.cst.tsearch.utils.StringUtils.*;

/**
 * ParcelIDAddressFilterResponse
 *
 */
public class ParcelIDAddressFilterResponse extends FilterResponse {

	private static final Category logger = Category.getInstance(ParcelIDAddressFilterResponse.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + ParcelIDAddressFilterResponse.class.getName());

	private FilterResponse parcelID ;
	private FilterResponse addressFilter ;

	public ParcelIDAddressFilterResponse(long searchId) {
		super(searchId);
		this.strategyType = STRATEGY_TYPE_HIGH_PASS;
		parcelID = getInstance(TYPE_ASSESSOR_PARCEL_ID,SearchAttributes.LD_PARCELNO, this.sa, stringCleanerId,searchId, null);
		addressFilter = getInstance(TYPE_ASSESSOR_ADDRESS2,SearchAttributes.NO_KEY, this.sa, stringCleanerId,searchId, null);
		this.threshold = new BigDecimal("0.60");
	}

	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal scoreParcelID = parcelID.getScoreOneRow( row);
		BigDecimal scoreAddress = addressFilter.getScoreOneRow( row);

		return scoreParcelID.add(scoreAddress).divide(new BigDecimal("0.5"),BigDecimal.ROUND_UNNECESSARY);
	}

	@Override
    public String getFilterName(){
    	return "Filter by PIN and Address";
    }
	
	@Override
	public String getFilterCriteria(){
		
		String pid = parcelID.getFilterCriteria();
		String addr = addressFilter.getFilterCriteria();
		
		return pid + " " + addr;
	}
}
