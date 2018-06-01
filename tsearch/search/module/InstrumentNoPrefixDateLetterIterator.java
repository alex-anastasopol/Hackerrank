package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.utils.StringUtils;

public class InstrumentNoPrefixDateLetterIterator extends
		InstrumentNoModuleStatesIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String prefix = "";
	private boolean useYear = false;
	private int noOfLastLetters = 0;

	public InstrumentNoPrefixDateLetterIterator(long searchId) {
		super(searchId);
	}
	
	public InstrumentNoPrefixDateLetterIterator(long searchId, String prefix,
			boolean useYear, int noOfLastLetters) {
		super(searchId);
		this.prefix = prefix;
		this.useYear = useYear;
		this.noOfLastLetters = noOfLastLetters;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	protected List extractInstrumentNoList(List original)
	{
		// for B 4530
		List instr = new ArrayList();
		HashSet<String> alreadyAdded = new HashSet<String>();
		for (int i = 0; i < original.size(); i++)
		{
			Instrument instrCrt = (Instrument)original.get(i);
			if(!StringUtils.isStringBlank(instrCrt.getInstrumentNo()))
			{
				String newInstrumentNo = "";
				String newSecondInstrumentNo = "";
				if (!instrCrt.getInstrumentNo().contains("K")){
					
					if(useYear && instrCrt.getFileDate() != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(instrCrt.getFileDate());
						int year = cal.get(Calendar.YEAR);
						if (year >= 1999) {
							newInstrumentNo += year;
						} else if (year > 1989 && year < 1999){
							newInstrumentNo += Integer.toString(year).replaceAll("\\d{2}(\\d{2})", "$1");
						}
					}
					newInstrumentNo += prefix;
					newSecondInstrumentNo = newInstrumentNo;
					
					if(noOfLastLetters > 0) {
						boolean twoCorresp = false;
						String tempInstrumentNo = instrCrt.getInstrumentNo();
						if(useYear && instrCrt.getFileDate() != null) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(instrCrt.getFileDate());
							int year = cal.get(Calendar.YEAR);
							int day = cal.get(Calendar.DAY_OF_MONTH);
							int month = cal.get(Calendar.MONTH);
							if (year < 1993) {
								noOfLastLetters = noOfLastLetters - 1;
							} else if (year == 1993){
								if (month < 11){
									noOfLastLetters = noOfLastLetters - 1;
								} else if (month == 11){
									if (day < 12){
										noOfLastLetters = noOfLastLetters -1;
									} else if (day == 12){
										twoCorresp = true; 
									}
								}
							} 
						}
						if(tempInstrumentNo.length() > 6) {
							tempInstrumentNo = tempInstrumentNo.substring(tempInstrumentNo.length() - noOfLastLetters);
							if (twoCorresp){
								newSecondInstrumentNo += tempInstrumentNo.substring(tempInstrumentNo.length() - noOfLastLetters - 1);
							}
						} else {
							tempInstrumentNo = org.apache.commons.lang.StringUtils.leftPad(tempInstrumentNo, noOfLastLetters, "0");
							if (twoCorresp){
								newSecondInstrumentNo += org.apache.commons.lang.StringUtils.leftPad(tempInstrumentNo, noOfLastLetters - 1, "0");
							}
						}
						newInstrumentNo += tempInstrumentNo;
					} else {
						newInstrumentNo += instrCrt;
					}
				} else {
					newInstrumentNo = instrCrt.getInstrumentNo();
				}
				if(!alreadyAdded.contains(newInstrumentNo)) {
					alreadyAdded.add(newInstrumentNo);
					instrCrt.setInstrumentNo(newInstrumentNo);
					instr.add(instrCrt);
				}
				if (newSecondInstrumentNo.length() == 8 ){
					if(!alreadyAdded.contains(newSecondInstrumentNo)) {
						alreadyAdded.add(newSecondInstrumentNo);
						instrCrt.setInstrumentNo(newSecondInstrumentNo);
						instr.add(instrCrt);
					}
				}
			}
		}		
		
		return instr;
	}

}
