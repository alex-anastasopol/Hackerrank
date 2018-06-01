package ro.cst.tsearch.servers.types;

import java.util.Calendar;
import java.util.Date;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author Cristi Stochina
 */
public class FLLeeDT extends FLSubdividedBasedDASLDT {
	private static final long serialVersionUID = 3477834L;

	public FLLeeDT(long searchId) {
		super(searchId);
	}

	public FLLeeDT(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	protected void addAssesorMapSearch(TSServerInfoModule module,
			PersonalDataStruct str) {
		CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(
				searchId);
		Search search = ci.getCrtSearchContext();

		String tw = str.getTownship();
		String rg = str.getRange();
		String sec = str.getSection();

		DocumentsManagerI m = search.getDocManager();
		uncheckPlats(m);

		if (StringUtils.isEmpty(tw) || StringUtils.isEmpty(rg)
				|| StringUtils.isEmpty(sec)) {
			try {
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if (StringUtils.isEmpty(tw)) {
					tw = subdivVector[2];
				}
				if (StringUtils.isEmpty(rg)) {
					rg = subdivVector[3];
				}
				if (StringUtils.isEmpty(sec)) {
					sec = subdivVector[3];
				}
			} finally {
				m.releaseAccess();
			}
		}
		if (StringUtils.isEmpty(tw)) {
			tw = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
		}
		if (StringUtils.isEmpty(rg)) {
			rg = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
		}
		if (StringUtils.isEmpty(sec)) {
			sec = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		}

		String apn = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO)
				.replaceAll("[.-]", "");

		if (!StringUtils.isEmpty(apn) && StringUtils.isEmpty(tw)) {
			apn = apn.replaceAll("[a-zA-Z]", "");
			if (apn.length() >= 6) {
				sec = apn.substring(0, 2);
				tw = apn.substring(2, 4);
				rg = apn.substring(4, 6);
			}
		}

		if (!StringUtils.isEmpty(tw) && tw.length() >= 2) {
			tw = tw.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}

		if (!StringUtils.isEmpty(rg) && rg.length() >= 2) {
			rg = rg.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}

		if (!StringUtils.isEmpty(sec) && sec.length() >= 2) {
			sec = sec.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}

		deletePlat(m, tw, rg);
		module.forceValue(0, tw);
		module.forceValue(1, rg);
		module.forceValue(2, sec);
		module.forceValue(4, "PLAT");
		module.forceValue(5, "ASSESSOR_MAP");

		return;
	}

	@Override
	public void addParcelMapSearch(TSServerInfoModule module,
			PersonalDataStruct str) {
	}

	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module,
			PersonalDataStruct str) {
	}

	@Override
	public void addCondoMapSearch(TSServerInfoModule module,
			PersonalDataStruct str) {
	}

	@Override
	protected void fakeIstrumentNo(ParsedResponse item) {
		DocumentI doc = item.getDocument();
		doc.setInstno(cleanInstrNo(doc.getInstrument(), true));
		item.setDocument(doc);
	}

	@Override
	protected String cleanInstrNo(InstrumentI instr, boolean clean_flag) {
		String instrNo = instr.getInstno();

		if (instr instanceof RegisterDocumentI) {
			Date date = ((RegisterDocumentI) instr).getRecordedDate();
			Calendar r = Calendar.getInstance();
			r.set(2005, Calendar.AUGUST, 28);
			Calendar c = Calendar.getInstance();
			c.setTime(date);

			if (c.before(r))
				return instrNo;
			else if (c.after(r)
					|| (StringUtils.isEmpty(instr.getBook()) && StringUtils
							.isEmpty(instr.getPage())))
				instrNo = clean_flag ? makeInstrNo(instr)
						: remakeInstrNo(instr);
		} else {
			if (instr.getYear() > 2005
					|| (instr.getYear() == 2005
							&& StringUtils.isEmpty(instr.getBook()) && StringUtils
							.isEmpty(instr.getPage())))
				instrNo = clean_flag ? makeInstrNo(instr)
						: remakeInstrNo(instr);
		}

		return instrNo;
	}

	private String remakeInstrNo(InstrumentI instr) {
		String x = instr.getInstno();

		if (x.length() == 13) {
			String year = Integer.toString(instr.getYear());
			x = x.replaceFirst(year, "").replaceAll("^0+", "");
		}

		return x;
	}

	private String makeInstrNo(InstrumentI instr) {
		// bug 6925 - 6148
		String x = instr.getInstno();

		if (x.length() < 13) {
			String year = Integer.toString(instr.getYear());
			x = year
					+ org.apache.commons.lang.StringUtils.leftPad(x,
							13 - year.length(), "0");
		}

		return x;
	}
}
