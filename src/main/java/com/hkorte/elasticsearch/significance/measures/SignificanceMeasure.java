package com.hkorte.elasticsearch.significance.measures;

import com.hkorte.elasticsearch.significance.SignificantTermsProvider;
import com.hkorte.elasticsearch.significance.model.ScoredTerm;

/**
 * Created by hkorte on 25.04.14.
 */
public abstract class SignificanceMeasure {
	public abstract String shortName();
	public abstract double compute(long n00, long n01, long n10, long n11);
	public ScoredTerm apply(long n00, long n01, long n10, long n11) {
		return new ScoredTerm(compute(n00, n01, n10, n11), n00, n01, n10, n11);
	}
}
