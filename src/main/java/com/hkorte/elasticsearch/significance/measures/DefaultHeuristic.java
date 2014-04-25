package com.hkorte.elasticsearch.significance.measures;

import com.hkorte.elasticsearch.significance.model.ScoredTerm;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * The default heuristic used in Elasticsearch to compute significant terms.
 *
 * @see org.elasticsearch.search.aggregations.bucket.significant.InternalSignificantTerms
 */
public class DefaultHeuristic extends SignificanceMeasure {

	@Override
	public String shortName() {
		return "default";
	}

	/**
	 * @param n00 docs which do not contain word with negative class
	 * @param n01 docs which do not contain word with positive class
	 * @param n10 docs which contain word with negative class
	 * @param n11 docs which contain word with positive class
	 * @return The default heuristic used in ES to compute significant terms
	 */
	@Override
	public double compute(long n00, long n01, long n10, long n11) {
		return apply(n00, n01, n10, n11).getScore();
	}

	@Override
	public ScoredTerm apply(long n00, long n01, long n10, long n11) {
		long subsetSize = n11 + n01;
		long supersetSize = n11 + n10 + n01 + n00;
		if ((subsetSize == 0) || (supersetSize == 0)) {
			// avoid any divide by zero issues
			return new ScoredTerm(0.0, n00, n01, n10, n11);
		}

		long subsetFreq = n11;
		long supersetFreq = n11 + n10;

		double subsetProbability = (double) subsetFreq / (double) subsetSize;
		double supersetProbability = (double) supersetFreq / (double) supersetSize;

		// Using absoluteProbabilityChange alone favours very common words e.g. you, we etc
		// because a doubling in popularity of a common term is a big percent difference
		// whereas a rare term would have to achieve a hundred-fold increase in popularity to
		// achieve the same difference measure.
		// In favouring common words as suggested features for search we would get high
		// recall but low precision.
		double absoluteProbabilityChange = subsetProbability - supersetProbability;
		if (absoluteProbabilityChange <= 0) {
			return new ScoredTerm(0.0, n00, n01, n10, n11);
		}
		// Using relativeProbabilityChange tends to favour rarer terms e.g.mis-spellings or
		// unique URLs.
		// A very low-probability term can very easily double in popularity due to the low
		// numbers required to do so whereas a high-probability term would have to add many
		// extra individual sightings to achieve the same shift.
		// In favouring rare words as suggested features for search we would get high
		// precision but low recall.
		double relativeProbabilityChange = (subsetProbability / supersetProbability);

		// A blend of the above metrics - favours medium-rare terms to strike a useful
		// balance between precision and recall.
		double score = absoluteProbabilityChange * relativeProbabilityChange;
		return new CustomScoredTerm(score, n00, n01, n10, n11, subsetProbability,
				supersetProbability, absoluteProbabilityChange, relativeProbabilityChange);
	}

	private static class CustomScoredTerm extends ScoredTerm {

		private double subsetProbability;
		private double supersetProbability;
		private double absoluteProbabilityChange;
		private double relativeProbabilityChange;

		public CustomScoredTerm(double score, long n00, long n01, long n10, long n11, double subsetProbability,
								double supersetProbability, double absoluteProbabilityChange,
								double relativeProbabilityChange) {
			super(score, n00, n01, n10, n11);
			this.subsetProbability = subsetProbability;
			this.supersetProbability = supersetProbability;
			this.absoluteProbabilityChange = absoluteProbabilityChange;
			this.relativeProbabilityChange = relativeProbabilityChange;
		}

		@Override
		public void addCustomFields(XContentBuilder builder) throws IOException {
			builder.field("subsetProbability", subsetProbability);
			builder.field("supersetProbability", supersetProbability);
			builder.field("absoluteProbabilityChange", absoluteProbabilityChange);
			builder.field("relativeProbabilityChange", relativeProbabilityChange);
		}
	}
}
