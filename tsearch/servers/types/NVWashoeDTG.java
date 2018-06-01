package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import com.stewart.ats.base.document.InstrumentI;

/**
 * @author cristian stochina
 */
public class NVWashoeDTG extends NVGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public NVWashoeDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public NVWashoeDTG(long searchId) {
		super(searchId);
	}


	@Override
	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		return "";
	}
	
}
