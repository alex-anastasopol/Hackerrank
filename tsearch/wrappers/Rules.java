package ro.cst.tsearch.wrappers;

public class Rules
{
	private Rule mIteration=null;
	private Rule mStartExtraction=null;
	private Rule mEndExtraction=null;
	
	/**
	 * Returns the Extraction.
	 * @return Rule
	 */
	public Rule getStartExtraction()
	{
		return mStartExtraction;
	}

	/**
	 * Returns the Iteration.
	 * @return Rule
	 */
	public Rule getIteration()
	{
		return mIteration;
	}

	/**
	 * Sets the Extraction.
	 * @param Extraction The Extraction to set
	 */
	public void setStartExtraction(Rule StartExtraction)
	{
		mStartExtraction= StartExtraction;
	}

	/**
	 * Sets the Iteration.
	 * @param Iteration The Iteration to set
	 */
	public void setIteration(Rule Iteration)
	{
		mIteration= Iteration;
	}

	/**
	 * Returns the endExtraction.
	 * @return Rule
	 */
	public Rule getEndExtraction()
	{
		return mEndExtraction;
	}

	/**
	 * Sets the endExtraction.
	 * @param endExtraction The endExtraction to set
	 */
	public void setEndExtraction(Rule endExtraction)
	{
		mEndExtraction= endExtraction;
	}

}
