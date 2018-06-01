package ro.cst.tsearch.search.filter.newfilters.date;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.document.sort.RecordedDateComparator;
import com.stewart.ats.base.search.DocumentsManagerI;

public class DateFilterForGoBack extends FilterResponse {
	private static final long serialVersionUID = 8049413332296989430L;

	private TSServerInfoModule module = null;
	Date dateLimit = null;

	public DateFilterForGoBack(long searchId) {
		super(searchId);
		super.threshold = BigDecimal.ONE;
	}

	public DateFilterForGoBack(String key, long searchId,
			TSServerInfoModule module) {
		super(searchId);
		super.threshold = BigDecimal.ONE;
		this.module = module;

	}

	public BigDecimal getScoreOneRow(ParsedResponse row) {
		GBManager gbm = (GBManager) sa
				.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		DocumentsManagerI documentsManager = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCrtSearchContext()
				.getDocManager();
		Date idate = null;

		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1);
		Calendar cal2 = new GregorianCalendar();

		String docType = null;
		String recDate = "";
		String instDate = "";
		if (row.getSaleDataSetsCount() > 0) {
			docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
		} else {
			if(row.getDocument() != null) {
				docType = row.getDocument().getServerDocType();
			}
		}
		if (DocumentTypes.checkDocumentType(docType,
				DocumentTypes.TRANSFER_INT, InstanceManager.getManager()
						.getCurrentInstance(searchId).getCrtSearchContext(),
				searchId)
				|| DocumentTypes.checkDocumentType(docType,
						DocumentTypes.MORTGAGE_INT, InstanceManager
								.getManager().getCurrentInstance(searchId)
								.getCrtSearchContext(), searchId)
				|| DocumentTypes.checkDocumentType(docType,
						DocumentTypes.COURT_INT, InstanceManager.getManager()
								.getCurrentInstance(searchId)
								.getCrtSearchContext(), searchId)) {
			
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
			
			RegisterDocument i = gbm.getDoc(this.module.getIndexInGB(),
					searchId);
			List<TransferI> allTransfers = null;
			try {

				documentsManager.getAccess();
				allTransfers = documentsManager.getTransferList(true);
				Collections.sort(allTransfers, new RecordedDateComparator());

			} catch (Exception e) {
				logger.error("Error while sorting transfers", e);
			} finally {
				documentsManager.releaseAccess();
			}
			RegisterDocument itmp = (RegisterDocument) allTransfers.get(0);

			if ("grantor".equals(module.getTypeSearchGB())) {
				dateLimit = i.getRecordedDate() != null
						&& (i.getRecordedDate().before(cal.getTime())) ? i
						.getRecordedDate() : cal.getTime();
				if (row.getSaleDataSetsCount() > 0) {
					recDate = row.getSaleDataSet(0).getAtribute("RecordedDate").trim();
					if (!StringUtils.isEmpty(recDate)) {
						try {
							idate = dateFormat.parse(recDate);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else {
						instDate = row.getSaleDataSet(0).getAtribute("InstrumentDate").trim();
						if (!StringUtils.isEmpty(instDate)) {
							try {
								idate = dateFormat.parse(instDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
				} else {
					DocumentI document = row.getDocument();
					if(document != null && document instanceof RegisterDocument) {
						RegisterDocumentI registerDocument = (RegisterDocument)document;
						idate = registerDocument.getRecordedDate();
						if (idate==null) {
							idate = registerDocument.getInstrumentDate();
						}
					}
				}
				if (!idate.after(dateLimit))
					return BigDecimal.ONE;
				else
					return BigDecimal.ZERO;
			} else {
				if (itmp != null
						&& !(itmp.getRecordedDate().equals(i.getRecordedDate())))
					dateLimit = cal2.getTime();
				else
					dateLimit = itmp.getRecordedDate() != null
							&& (itmp.getRecordedDate().before(cal.getTime())) ? itmp
							.getRecordedDate() : cal2.getTime();

				try {
					recDate = ((InfSet) ((Vector) row.infVectorSets
							.get("SaleDataSet")).get(0))
							.getAtribute("RecordedDate");

				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					instDate = ((InfSet) ((Vector) row.infVectorSets
							.get("SaleDataSet")).get(0))
							.getAtribute("InstrumentDate");
					if ((!"".equals(recDate)) && (recDate != null))
						idate = dateFormat.parse(recDate);
					else
						idate = dateFormat.parse(instDate);
				} catch (Exception e) {
					e.printStackTrace();

				}
				if (!idate.after(dateLimit))
					return BigDecimal.ONE;
				else
					return BigDecimal.ZERO;
			}

		}
		if (DocumentTypes.checkDocumentType(docType, DocumentTypes.LIEN_INT,
				InstanceManager.getManager().getCurrentInstance(searchId)
						.getCrtSearchContext(), searchId)
				|| DocumentTypes.checkDocumentType(docType,
						DocumentTypes.RELEASE_INT, InstanceManager.getManager()
								.getCurrentInstance(searchId)
								.getCrtSearchContext(), searchId)) {
			dateLimit = cal2.getTime();
			return BigDecimal.ONE;
		}
		return BigDecimal.ZERO;
	}

	public String getFilterName() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		return "Filter/Validate to date"
				+ (dateLimit != null ? sdf.format(dateLimit) : "");
	}

	@Override
	public String getFilterCriteria() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		return super.getFilterCriteria() + " Filter/Validate To Date ="
				+ (dateLimit != null ? sdf.format(dateLimit) : "");
	}
}
