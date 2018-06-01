package ro.cst.tsearch.reports.throughputs;

import java.awt.Font;
import java.awt.Paint;
import java.awt.font.TextAttribute;
import java.io.Serializable;
import java.text.AttributedString;
import java.text.MessageFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.AbstractPieItemLabelGenerator;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.data.general.PieDataset;
import org.jfree.util.ObjectList;

import ro.cst.tsearch.database.procedures.GraphicReportProcedure;

public class ATSPieSectionLabelGenerator 
	extends AbstractPieItemLabelGenerator
	implements PieSectionLabelGenerator, Cloneable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1662777711404359117L;

	/** The default section label format. */
    public static final String DEFAULT_SECTION_LABEL_FORMAT = "{0} = {1}";

    /** 
     * An optional list of attributed labels (instances of AttributedString). 
     */
    private ObjectList attributedLabels;
    
    private String type;

    /**
     * Creates an item label generator using default number formatters.
     */
    public ATSPieSectionLabelGenerator(String type) {
        this(type, "{0} = {1}", NumberFormat.getNumberInstance(), 
                NumberFormat.getPercentInstance());
    }

    /**
     * Creates an item label generator.
     * 
     * @param labelFormat  the label format.
     */
    public ATSPieSectionLabelGenerator(String type, String labelFormat) {
        this(type, labelFormat, NumberFormat.getNumberInstance(), 
                NumberFormat.getPercentInstance());   
    }
    
    /**
     * Creates an item label generator using the specified number formatters.
     *
     * @param labelFormat  the label format string (<code>null</code> not 
     *                     permitted).
     * @param numberFormat  the format object for the values (<code>null</code>
     *                      not permitted).
     * @param percentFormat  the format object for the percentages 
     *                       (<code>null</code> not permitted).
     */
    public ATSPieSectionLabelGenerator(String type, String labelFormat,
                                         NumberFormat numberFormat, 
                                         NumberFormat percentFormat) {

        super(labelFormat, numberFormat, percentFormat);
        this.attributedLabels = new ObjectList();
        this.type = type;

    }
    

	
	/**
     * Returns the attributed label for a section, or <code>null</code> if none
     * is defined.
     * 
     * @param section  the section index.
     * 
     * @return The attributed label.
     */
    public AttributedString getAttributedLabel(int section) {
        return (AttributedString) this.attributedLabels.get(section);    
    }
    
    /**
     * Sets the attributed label for a section.
     * 
     * @param section  the section index.
     * @param label  the label (<code>null</code> permitted).
     */
    public void setAttributedLabel(int section, AttributedString label) {
        this.attributedLabels.set(section, label);
    }
    
    /**
     * Generates a label for a pie section.
     * 
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param key  the section key (<code>null</code> not permitted).
     * 
     * @return The label (possibly <code>null</code>).
     */
    public String generateSectionLabel(PieDataset dataset, Comparable key) {
        //return super.generateSectionLabel(dataset, key);
    	String result = null;    
        if (dataset != null) {
            Object[] items = createItemArray(dataset, key);
            result = MessageFormat.format(this.getLabelFormat(), items);
        }
        if(type.equals(GraphicReportProcedure.ABSTRACTOR_DATA)){
        	String aux = result;
        	int pos1 = result.indexOf(" - ");
        	int pos2 = result.lastIndexOf(" ", pos1-1);
        	
        	if(pos1>0 && pos2>0){
        		aux = result.substring(pos2 + 1,pos1);
        	   	aux += " " + result.charAt(0) + ".";
        	   	result = aux;
        	}
        } /*else if(type.equals(ThroughputOpCode.AGENTS)) {
        	String aux = result;
        	int pos1 = result.indexOf(' ');
        	int pos2 = result.indexOf(" ", pos1+1);
        	if(pos1>0 && pos2>0){
        		aux = result.substring(pos1,pos2);
        	   	aux += " " + result.charAt(0);
        	   	result = aux;
        	}
        }*/
        return result;
    }

    /**
     * Generates an attributed label for the specified series, or 
     * <code>null</code> if no attributed label is available (in which case,
     * the string returned by 
     * {@link #generateSectionLabel(PieDataset, Comparable)} will 
     * provide the fallback).  Only certain attributes are recognised by the 
     * code that ultimately displays the labels: 
     * <ul>
     * <li>{@link TextAttribute#FONT}: will set the font;</li>
     * <li>{@link TextAttribute#POSTURE}: a value of 
     *     {@link TextAttribute#POSTURE_OBLIQUE} will add {@link Font#ITALIC} to
     *     the current font;</li>
     * <li>{@link TextAttribute#WEIGHT}: a value of 
     *     {@link TextAttribute#WEIGHT_BOLD} will add {@link Font#BOLD} to the 
     *     current font;</li>
     * <li>{@link TextAttribute#FOREGROUND}: this will set the {@link Paint} 
     *     for the current</li>
     * <li>{@link TextAttribute#SUPERSCRIPT}: the values 
     *     {@link TextAttribute#SUPERSCRIPT_SUB} and 
     *     {@link TextAttribute#SUPERSCRIPT_SUPER} are recognised.</li> 
     * </ul>
     * 
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param key  the key.
     * 
     * @return An attributed label (possibly <code>null</code>).
     */
    public AttributedString generateAttributedSectionLabel(PieDataset dataset, 
                                                           Comparable key) {    	
        return getAttributedLabel(dataset.getIndex(key));
    }

    /**
     * Tests the generator for equality with an arbitrary object.
     *
     * @param obj  the object to test against (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof StandardPieSectionLabelGenerator)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return true;
    }

    /**
     * Returns an independent copy of the generator.
     * 
     * @return A clone.
     * 
     * @throws CloneNotSupportedException  should not happen.
     */
    public Object clone() throws CloneNotSupportedException {      
        return super.clone();
    }
	
}
