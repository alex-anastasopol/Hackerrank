package ro.cst.tsearch.templates;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Helper class that was originally designed as a response for {@link AddDocsTemplates}.getDocumentsInternal(...)
 * 
 * @author Andrei
 *
 */
public class FilledDocumentsInternalResponse {
	private Vector<String>	filledData				= new Vector<String>();
	private Set<String>		filledDocumentsIds		= new HashSet<String>();
	private int				appliedOverDocuments	= 0;

	public Vector<String> getFilledData() {
		return filledData;
	}

	public Set<String> getFilledDocumentsIds() {
		return filledDocumentsIds;
	}

	public void setAppliedOverDocuments(int appliedOverDocuments) {
		this.appliedOverDocuments = appliedOverDocuments;
	}

	public int getAppliedOverDocuments() {
		return appliedOverDocuments;
	}
}
