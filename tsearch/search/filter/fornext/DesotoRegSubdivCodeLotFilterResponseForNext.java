/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.fornext;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DesotoRegSubdivCodeLotFilterResponseForNext extends FilterResponse{
	protected static final Category logger = Category.getInstance(DesotoRegSubdivCodeLotFilterResponseForNext.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + DesotoRegSubdivCodeLotFilterResponseForNext.class.getName());
	
	

	private String subdivCode ="";
	private String lotno ="";
	
	public DesotoRegSubdivCodeLotFilterResponseForNext(String subdivCode, String lotno,long searchId){
		super(searchId);
		this.subdivCode = subdivCode;
		this.lotno = lotno;
		threshold = new BigDecimal("0.99");
	}
	
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		String candSubdivCode = (String) row.getPropertyIdentificationSet(0).getAtribute("SubdivisionCode");
		String candLotNo = (String) row.getPropertyIdentificationSet(0).getAtribute("SubdivisionLotNumber");

		BigDecimal scoreSubdivCode = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_LOT , subdivCode, candSubdivCode,searchId)).getScore();
		BigDecimal scoreLotNo = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_LOT , lotno, candLotNo,searchId)).getScore();

		loggerDetails.debug(" score match [" +	subdivCode +	"] vs [" +	candSubdivCode + "]= " + scoreSubdivCode);
		loggerDetails.debug(" score match [" +	lotno +	"] vs [" +	candLotNo + "]= " + scoreLotNo);
		
		return scoreSubdivCode.min( scoreLotNo);
	}


	
}
