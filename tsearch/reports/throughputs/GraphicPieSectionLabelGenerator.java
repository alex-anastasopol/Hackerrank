package ro.cst.tsearch.reports.throughputs;

import java.math.BigDecimal;
import java.util.HashMap;

import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;

public class GraphicPieSectionLabelGenerator extends
		StandardPieSectionLabelGenerator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private HashMap<String, GraphicInfoStructure> extraInfo = null;
	
	public GraphicPieSectionLabelGenerator(String labelFormat, 
			HashMap<String, GraphicInfoStructure> extraInfo) {
		super(labelFormat);
		this.extraInfo = extraInfo;
	}

	@Override
	protected Object[] createItemArray(PieDataset dataset, Comparable key) {

        Object[] result = new Object[4];
        double total = DatasetUtilities.calculatePieDatasetTotal(dataset);
        result[0] = key.toString();
        Number value = dataset.getValue(key);
        if (value != null) {
        	if(extraInfo.containsKey(key)) {
        		try {
        			value = new BigDecimal(extraInfo.get(key).getRealValue());
        		} catch (Exception e) {
					e.printStackTrace();
				}
        	}
            result[1] = getNumberFormat().format(value);
        }
        else {
            result[1] = "null";
        }
        double percent = 0.0;
        if (value != null) {
            double v = value.doubleValue();
            if (v > 0.0) {
                percent = v / total;
            }
        }
        result[2] = getNumberFormat().format(percent);
        result[3] = getNumberFormat().format(total);
        return result;

	}
	
	

}
