package ro.cst.tsearch.threads.general;

import java.util.List;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.TaxDatesMapper;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;

public class CheckTaxDatesExpiredThread extends Thread {

	private static final String UPDATE_TO_RUN = "UPDATE ts_tax_dates SET dueDate = date_format((str_to_date(dueDate, '%m/%d/%Y') + interval 1 year),'%m/%d/%Y'), payDate = date_format((str_to_date(payDate, '%m/%d/%Y') + interval 1 year),'%m/%d/%Y') "
			+ " where (STR_TO_DATE(paydate,'%m/%d/%Y') + interval 1 year) < now() and name != 'ILWillTU'";
	
	
	public static final String SQL_CHECK_PAY_DATE = 
			"select * from " + TaxDatesMapper.TABLE_TAX_DATES +
			" where (STR_TO_DATE(" + TaxDatesMapper.FIELD_PAY_DATE + ",'%m/%d/%Y') + interval 1 year) < now() and name != 'ILWillTU' ";
	
	public CheckTaxDatesExpiredThread() {
		super("CheckTaxDatesExpiredThread");
	}
	
	@Override
	public void run() {
		StringBuilder emailToSend = new StringBuilder();
		
		try {
			List<TaxDatesMapper> list = DBManager.getSimpleTemplate().query(SQL_CHECK_PAY_DATE, new TaxDatesMapper());
			if(!list.isEmpty()) {
				for (TaxDatesMapper taxDatesMapper : list) {
					emailToSend.append(taxDatesMapper.getName())
						.append(" with DD: ").append(taxDatesMapper.getDueDateString())
						.append(", PD: ").append(taxDatesMapper.getPayDateString())
						.append(" and TaxYearMode: ").append(taxDatesMapper.getTaxYearMode())
						.append(" needs to be updated\n");
				}
				
				DBManager.getSimpleTemplate().update(UPDATE_TO_RUN);
				
			}
		} catch (Exception e) {
			emailToSend.append("\nError encontered\n").append(Log.exceptionToString(e));
		}
		
		if(emailToSend.length() > 0) {
			
			emailToSend.append("\n\nUpdate Run:\n\n")
				.append(UPDATE_TO_RUN).append("\n");
			
			
			Log.sendEmail(MailConfig.getMailLoggerStatusAddress(), 
					"Check Tax Dates Expired on server " + URLMaping.INSTANCE_DIR + " update", 
					emailToSend.toString());
		}
		
	}

}
