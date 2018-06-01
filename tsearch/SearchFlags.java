package ro.cst.tsearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stewart.ats.base.warning.WarningInfoI;

public class SearchFlags implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public static enum CREATION_SOURCE_TYPES {
		NORMAL,
		CLONED,
		REOPENED
	}
	
	
	private boolean starter = false;
	private boolean isClosed = false;
	private boolean isForReview = false;
	private boolean isOld = false;
	private boolean isTsrCreated = false;
	private int status = 0;
	private boolean isForFVS = false;
	/**
	 * Flag which should only be true until the first SearchLog write statement<br>
	 * On deserialization of old objects it is expected to be false
	 */
	private boolean writeFirstLogLine = true;
	
	private HashSet<Integer> warnings = null;
	@Deprecated
	private Set<String> permanentWarnings = null;
	private List<WarningInfoI> warningList = null;
	
	
	
	private CREATION_SOURCE_TYPES creationSourceType = CREATION_SOURCE_TYPES.NORMAL;
	
	/**
     * the object version number.
     * (Used when the structure of the Search object is changed and 
     * you need to create a special converter for reloading old searches) 
     */
    private int objectVersionNumber = 0;

    /**
     * Checks if this search is a Base Search (previously known as Starter Search)
     * @return <code>true</code> if it is a Base Search
     */
	public boolean isBase() {
		return starter;
	}

	/**
	 * Marks this search to be a Base Search (previously known as Starter Search)
	 * @param isBase <code>true</code> to mark as Base Search
	 */
	public void setBase(boolean isBase) {
		this.starter = isBase;
	}
	
	/**
	 * Please use isBase() method as the name changed from Starter to Base Search
	 * @return <code>true</code> if it is a Base Search
	 */
	@Deprecated
	public boolean isStarter() {
		return isBase();
	}

	/**
	 * Please use setBase() method as the name changed from Starter to Base Search
	 * @param starter
	 */
	@Deprecated
	public void setStarter(boolean starter) {
		setBase(starter);
	}
	
	/**
	 * @return the objectVersionNumber
	 */
	public int getObjectVersionNumber() {
		return objectVersionNumber;
	}

	/**
	 * @param objectVersionNumber the objectVersionNumber to set
	 */
	public void setObjectVersionNumber(int objectVersionNumber) {
		this.objectVersionNumber = objectVersionNumber;
	}

	/**
	 * @return the isClosed
	 */
	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * @param isClosed the isClosed to set
	 */
	public void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}
	
	/**
	 * @return the isForReview
	 */
	public boolean isForReview() {
		return isForReview;
	}

	/**
	 * @param isForReview the isForReview to set
	 */
	public void setForReview(boolean isForReview) {
		this.isForReview = isForReview;
	}

	/**
	 * @return the isForFVS
	 */
	public boolean isForFVS() {
		return isForFVS;
	}

	/**
	 * @param isForFVS the isForFVS to set
	 */
	public void setForFVS(boolean isForFVS) {
		this.isForFVS = isForFVS;
	}
	
	/**
	 * @return the isOld
	 */
	public boolean isOld() {
		return isOld;
	}

	/**
	 * @param isOld the isOld to set
	 */
	public void setOld(boolean isOld) {
		this.isOld = isOld;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @return the isTsrCreated
	 */
	public boolean isTsrCreated() {
		return isTsrCreated;
	}

	/**
	 * @param isTsrCreated the isTsrCreated to set
	 */
	public void setTsrCreated(boolean isTsrCreated) {
		this.isTsrCreated = isTsrCreated;
	}

	/**
	 * @return the warnings
	 */
	public HashSet<Integer> getWarnings() {
		if(warnings == null) {
			warnings = new HashSet<Integer>();
		}
		return warnings;
	}
	
	public boolean addWarning(int warningId){
		return getWarnings().add(warningId);
	}
	
	public void cleanWarnings() {
		getWarnings().clear();
	}
	
	@Deprecated
	public Set<String> getPermanentWarnings(){
		if(permanentWarnings == null) {
			permanentWarnings = new HashSet<String>();
		}
		return permanentWarnings;
	}
	
	public boolean addPermanentWarning(String warning) {
		return getPermanentWarnings().add(warning);
	}

	public CREATION_SOURCE_TYPES getCreationSourceType() {
		return creationSourceType;
	}
	
	public int getCreationSourceTypeForDatabase() {
		CREATION_SOURCE_TYPES creationSourceTypes = getCreationSourceType();
		if(CREATION_SOURCE_TYPES.NORMAL.equals(creationSourceTypes)) {
			return 0;
		} else if(CREATION_SOURCE_TYPES.REOPENED.equals(creationSourceTypes)) {
			return 1;
		} else if(CREATION_SOURCE_TYPES.CLONED.equals(creationSourceTypes)) {
			return 2;
		} else {
			return 0;
		}
	}

	public void setCreationSourceType(CREATION_SOURCE_TYPES creationSourceType) {
		this.creationSourceType = creationSourceType;
	}

	public List<WarningInfoI> getWarningList() {
		if(warningList == null) {
			warningList = new ArrayList<WarningInfoI>();
		}
		return warningList;
	}
	
	/**
	 * Adds the warning in list if the list does not contain the warning
	 * @param warningInfoI the warning to be added
	 * @param <code>true</code> if and only if the warning was added
	 */
	public boolean addWarning(WarningInfoI warningInfoI) {
		if(!getWarningList().contains(warningInfoI)) {
			return getWarningList().add(warningInfoI);
		}
		return false;
	}

	public boolean isWriteFirstLogLine() {
		return writeFirstLogLine;
	}

	public void setWriteFirstLogLine(boolean writeFirstLogLine) {
		this.writeFirstLogLine = writeFirstLogLine;
	}
	
}
