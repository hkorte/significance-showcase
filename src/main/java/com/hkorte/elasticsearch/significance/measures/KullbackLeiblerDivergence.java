package com.hkorte.elasticsearch.significance.measures;

public class KullbackLeiblerDivergence extends SignificanceMeasure {

	@Override
	public String shortName() {
		return "kld";
	}

	/**
	 * @param n00 docs which do not contain word with negative class
	 * @param n01 docs which do not contain word with positive class
	 * @param n10 docs which contain word with negative class
	 * @param n11 docs which contain word with positive class
	 * @return The Kullback-Leibler divergence of the given distribution
	 * @author hkorte
	 */
	@Override
	public double compute(long n00, long n01, long n10, long n11) {
		double nx1 = n11 + n01;
		double nx0 = n10 + n00;
		double p = (n11 + 1) / (nx1 + 1); // relative freq for positive class (smoothed with +1)
		double q = (n10 + 1) / (nx0 + 1); // relative freq for negative class (smoothed with +1)
		return p * log2(p / q);
	}

	private static double log2(double d) {
		return Math.log(d) / Math.log(2d);
	}
}
