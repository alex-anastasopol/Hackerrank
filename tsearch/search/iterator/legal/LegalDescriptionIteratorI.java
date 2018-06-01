package ro.cst.tsearch.search.iterator.legal;

import ro.cst.tsearch.search.iterator.data.LegalStruct;

import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;

public interface LegalDescriptionIteratorI {

	/**
	 * Secondary Platted Legal is the legal after plat (bp/instr), like lot/block/other<br>
	 * Default implementation load only lot and block
	 * @param subdiv source of the legal
	 * @param legalStruct destination of the legal
	 */
	void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct);

	/**
	 * Check if the transfer is allowed for legal collection<br>
	 * For example should be a real transfer
	 * @param doc the document that will be checked
	 * @return true only if this document is a transfer and is allowed 
	 */
	boolean isTransferAllowed(RegisterDocumentI doc);

}
