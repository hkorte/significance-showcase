package com.hkorte.elasticsearch.significance.measures;

/**
 * The Chi-squared test.
 */
public class ChiSquared extends SignificanceMeasure {

	@Override
	public String shortName() {
		return "chi2";
	}

	/**
	 * @param n00 docs which do not contain word with negative class
	 * @param n01 docs which do not contain word with positive class
	 * @param n10 docs which contain word with negative class
	 * @param n11 docs which contain word with positive class
	 * @return The Chi-squared test result of the given distribution
	 * @author hkorte
	 */
	@Override
	public double compute(long n00, long n01, long n10, long n11) {
		// add +1 for smoothing and to avoid division by zero
		n00++; n01++; n10++; n11++;
		return ((n11+n10+n01+n00) * Math.pow(n11*n00 - n10*n01, 2)) / ((n11+n01) * (n11+n10) * (n10+n00) * (n01+n00));
	}
}
