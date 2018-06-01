package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * @author mihaib
 * 
 */

public class CAGeneric2DTG extends CAGenericDTG {

	private static final long serialVersionUID = -6964284342671945857L;

	public CAGeneric2DTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public CAGeneric2DTG(long searchId) {
		super(searchId);
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				
				if (StringUtils.isNotEmpty(instno)){
					if (instno.contains("-")){
						instno = instno.substring(instno.indexOf("-") + 1);
					}
					if (instno.length() < 7){
						if (year != SimpleChapterUtils.UNDEFINED_YEAR){
							String yr = "";
							yr = Integer.toString(year);
							if (StringUtils.isNotEmpty(yr)){
								if (yr.length() == 4){
									return yr.substring(2) + StringUtils.leftPad(instno, 6, "0");
								} else if (yr.length() == 2){
									return yr + StringUtils.leftPad(instno, 6, "0");
								}
							}
						}
					}
				}
				return instno;
			}
			
			@Override
			public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				
				return "";
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
	
	public InstrumentGenericIterator getInstrumentIteratorForImageFakeDocs(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (StringUtils.isNotEmpty(instno)){
					if (instno.contains("-")){
						instno = instno.substring(instno.indexOf("-") + 1);
					}
					if (instno.length() < 7){
						if (year != SimpleChapterUtils.UNDEFINED_YEAR){
							String yr = "";
							yr = Integer.toString(year);
							if (StringUtils.isNotEmpty(yr)){
								if (yr.length() == 4){
									return yr.substring(2) + StringUtils.leftPad(instno, 6, "0");
								} else if (yr.length() == 2){
									return yr + StringUtils.leftPad(instno, 6, "0");
								}
							}
						}
					} 
//					else if (instno.length() == 8){
//						return StringUtils.strip(instno.substring(2), "0");
//					}
				}
				return instno;
			}
			@Override
			protected void useInstrumentI(List<InstrumentI> result,	HashSet<String> listsForNow, DocumentsManagerI manager,
												InstrumentI instrumentI) {
				if (!isLoadFromRoLike()){
					instrumentI.setDocType(DocumentTypes.MISCELLANEOUS);
					instrumentI.setDocSubType(DocumentTypes.MISCELLANEOUS);
				}
				if (isEnableInstrumentNumber()) {
					processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
				}
				if (isEnableDocumentNumber()) {
					processEnableDocumentNumber(result, listsForNow, manager, instrumentI);
				}
				if (isEnableBookPage()) {
					processEnableBP(result, listsForNow, manager, instrumentI);
				}
			}

			@Override
			public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				
				return "";
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				return checkFlexibleInclusion(instrumentI, manager, false);
			}
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		instrumentGenericIterator.setCheckIfWasAlreadyInvalidated(true);
		return instrumentGenericIterator;
	}
	
	private List<DocumentI> checkFlexibleInclusion(InstrumentI documentToCheck, DocumentsManagerI documentManager, boolean checkDocType) {
		
		InstrumentI documentToCheckCopy = documentToCheck.clone();
//		String doctype = documentToCheckCopy.getDocType();
//		if (StringUtils.isNotEmpty(doctype)){
//			documentToCheckCopy.setDocType(DocumentTypes.getDocumentCategory(doctype, searchId));
//			documentToCheckCopy.setDocSubType(DocumentTypes.getDocumentSubcategory(doctype, searchId));
//		}
		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheckCopy);
		
		List<DocumentI> alike = new ArrayList<DocumentI>();
		
		if (almostLike != null && !almostLike.isEmpty()) {
			
			return almostLike;
			
		} else {
			if (checkDocType) {
				return almostLike;
			}
			String bookToCheck = documentToCheck.getBook();
				
			List<DocumentI> allRODocuments = documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
			for (DocumentI documentI : allRODocuments) {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getBook()) && org.apache.commons.lang.StringUtils.isNotEmpty(bookToCheck) 
							&& documentI.getBook().equals(bookToCheck)
					   && org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getPage()) && org.apache.commons.lang.StringUtils.isNotEmpty(documentToCheck.getPage()) 
					   		&& documentI.getPage().equals(documentToCheck.getPage())) {
						
					alike.add(documentI);
				} else if (documentI.getInstno().equals(documentToCheck.getInstno())){
					alike.add(documentI);
				}
			}
		}
		return alike;
	}
}
