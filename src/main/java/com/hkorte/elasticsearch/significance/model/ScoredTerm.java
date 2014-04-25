package com.hkorte.elasticsearch.significance.model;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * Created by hkorte on 25.04.14.
 */
public class ScoredTerm {
	private double score;
	private String term;
	private long n00;
	private long n01;
	private long n10;
	private long n11;

	public ScoredTerm(double score, long n00, long n01, long n10, long n11) {
		this.term = null;
		this.score = score;
		this.n00 = n00;
		this.n01 = n01;
		this.n10 = n10;
		this.n11 = n11;
	}

	public double getScore() {
		return score;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public long getN00() {
		return n00;
	}

	public long getN01() {
		return n01;
	}

	public long getN10() {
		return n10;
	}

	public long getN11() {
		return n11;
	}

	public void addCustomFields(XContentBuilder builder) throws IOException {
		// does nothing by default
	}
}
