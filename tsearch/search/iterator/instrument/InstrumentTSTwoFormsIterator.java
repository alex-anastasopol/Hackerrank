package ro.cst.tsearch.search.iterator.instrument;

import java.util.HashSet;
import java.util.List;

import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class InstrumentTSTwoFormsIterator extends InstrumentGenericIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstrumentTSTwoFormsIterator(long searchId) {
		super(searchId);
	}
	
	public static boolean isRelatedSourceDoctype(String type){
		String[] relatedSourceDoctype = new String[]{DocumentTypes.MORTGAGE, DocumentTypes.LIEN, DocumentTypes.CCER};
		for (int i=0;i<relatedSourceDoctype.length;i++)
			if (type.equalsIgnoreCase(relatedSourceDoctype[i]))
				return true;
		return false;
	} 
	
	@Override
	protected void processEnableInstrumentNo(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		String instrumentNo = cleanInstrumentNo(instrumentI.getInstno(), instrumentI.getYear());
		String keyJustDigits = null;
		String keyWithMinus = null;
		if(instrumentNo.matches("\\d{10}")) {
			keyJustDigits = "Instrument=" + instrumentNo;
			keyWithMinus = "Instrument=" + instrumentNo.substring(0,4) + "-" + instrumentNo.substring(4);
		}
		if(instrumentNo.matches("\\d{4}-+\\d{6}")) {
			keyJustDigits = "Instrument=" + instrumentNo.replace("-", "");
			keyWithMinus = "Instrument=" + instrumentNo.replace("-{2,}", "-");
		}
		
		if(StringUtils.isNotEmpty(instrumentNo) && 
				!(listsForNow.contains(keyJustDigits) && listsForNow.contains(keyWithMinus))
				) {
			InstrumentI iJustDigits = null;
			boolean foundSaved = false;
			if(keyJustDigits != null) {
				iJustDigits = instrumentI.clone();
				iJustDigits.setInstno(keyJustDigits.replace("Instrument=", ""));
				List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, iJustDigits);
				if(almostLike.size() > 0) {
					foundSaved = true;
				}
			}
			InstrumentI iWithMinus = null;
			if(keyWithMinus != null && !foundSaved) {
				iWithMinus = instrumentI.clone();
				iWithMinus.setInstno(keyWithMinus.replace("Instrument=", ""));
				List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, iWithMinus);
				if(almostLike.size() > 0) {
					foundSaved = true;
				}
			}
			if(isRelatedSourceDoctype(instrumentI.getDocType())			//B 6923 
					|| !foundSaved) {
				if(iJustDigits != null) {
					listsForNow.add(keyJustDigits);
					result.add(iJustDigits);
				}
				if(iWithMinus != null) {
					listsForNow.add(keyWithMinus);
					result.add(iWithMinus);
				}
			}
		}
	}

}
