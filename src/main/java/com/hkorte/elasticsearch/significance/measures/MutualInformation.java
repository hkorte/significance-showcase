package com.hkorte.elasticsearch.significance.measures;

/**
 * Created by hkorte on 25.04.14.
 */
public class MutualInformation extends SignificanceMeasure {

	@Override
	public String shortName() {
		return "mi";
	}

	/**
	 * See Christopher D. Manning, <em>Introduction to Information Retrieval</em> (2009), section 13.5.1, equation
	 * 13.17.
	 *
	 * @param n00 docs which do not contain word with negative class
	 * @param n01 docs which do not contain word with positive class
	 * @param n10 docs which contain word with negative class
	 * @param n11 docs which contain word with positive class
	 * @return The mutual information of the given distribution
	 * @author hkorte
	 */
	@Override
	public double compute(long n00, long n01, long n10, long n11) {
		// add +1 for smoothing and to avoid division by zero
		n00++; n01++; n10++; n11++;
		double n1x = n11 + n10;
		double nx1 = n11 + n01;
		double n0x = n01 + n00;
		double nx0 = n10 + n00;
		double nxx = n11 + n10 + n01 + n00;
		return (n11 / nxx * log2(nxx * n11 / (n1x * nx1))) + n01 / nxx * log2(nxx * n01 / (n0x * nx1)) + n10 / nxx *
				log2(nxx * n10 / (n1x * nx0)) + n00 / nxx * log2(nxx * n00 / (n0x * nx0));
	}

	private static double log2(double d) {
		return Math.log(d) / Math.log(2d);
	}
}
