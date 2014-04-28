package com.hkorte.elasticsearch.significance;

import com.hkorte.elasticsearch.significance.measures.*;
import com.hkorte.elasticsearch.significance.model.ScoredTerm;
import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.cache.CacheLoader;
import org.elasticsearch.common.cache.LoadingCache;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;

/**
 * Created by hkorte on 25.04.14.
 */
public class SignificantTermsProvider {
	private static final String FACET_NAME = "terms";
	private static final int NUM_TERMS = 100000;
	private final ESLogger logger;
	private final Client client;
	private final LoadingCache<FieldIdentifier, Map<String, Integer>> globalDocFreqCache;
	private final LoadingCache<TypeIdentifier, Long> globalDocCountCache;

	public SignificantTermsProvider(Settings settings, Client client) {
		this.logger = Loggers.getLogger(getClass(), settings);
		this.client = client;
		this.globalDocFreqCache = CacheBuilder.newBuilder().maximumSize(5).expireAfterWrite(5,
				TimeUnit.MINUTES).build(new CacheLoader<FieldIdentifier, Map<String, Integer>>() {

			@Override
			public Map<String, Integer> load(FieldIdentifier fieldIdentifier) {
				SearchResponse response = SignificantTermsProvider.this.client.prepareSearch(fieldIdentifier.indices)
						.setTypes(fieldIdentifier.types).setSize(0).setQuery(QueryBuilders.matchAllQuery()).addFacet
								(FacetBuilders.termsFacet(FACET_NAME).order(TermsFacet.ComparatorType.COUNT).field
										(fieldIdentifier.field).size(NUM_TERMS)).get();
				TermsFacet termsFacet = response.getFacets().facet(FACET_NAME);
				Map<String, Integer> map = new HashMap<String, Integer>();
				for (TermsFacet.Entry entry : termsFacet) {
					map.put(entry.getTerm().string(), entry.getCount());
				}
				return map;
			}
		});
		this.globalDocCountCache = CacheBuilder.newBuilder().maximumSize(5).expireAfterWrite(5,
				TimeUnit.MINUTES).build(new CacheLoader<TypeIdentifier, Long>() {

			@Override
			public Long load(TypeIdentifier fieldIdentifier) {
				SearchResponse response = SignificantTermsProvider.this.client.prepareSearch(fieldIdentifier.indices)
						.setTypes(fieldIdentifier.types).setSize(0).setQuery(QueryBuilders.matchAllQuery()).get();
				return response.getHits().getTotalHits();
			}
		});
	}

	public void writeSignificantTerms(RestRequest request, RestChannel channel, String[] indices, String[] types,
			String field, int size, String query) throws IOException, ExecutionException {

		Map<String, Integer> dfMap = this.globalDocFreqCache.get(new FieldIdentifier(indices, types, field));
		long numDocs = this.globalDocCountCache.get(new TypeIdentifier(indices, types));

		SearchResponse response = client.prepareSearch(indices).setTypes(types).setSize(0).setQuery(query).addFacet
				(FacetBuilders.termsFacet(FACET_NAME).order(TermsFacet.ComparatorType.COUNT).field(field).size
						(NUM_TERMS)).get();
		long numHits = response.getHits().getTotalHits();
		if (numHits > numDocs) {
			// obviously the numDocs were outdated
			// -> we simply adjust the value to be valid
			numDocs = numHits;
		}

		// we only have sensible results, if there is a negative set
		if (numHits < numDocs) {

			TermsFacet termsFacet = response.getFacets().facet(FACET_NAME);

			List<SignificanceMeasureResults> resultsList = new ArrayList<>();
			resultsList.add(new SignificanceMeasureResults(new DefaultHeuristic(), size));
			resultsList.add(new SignificanceMeasureResults(new MutualInformation(), size));
			resultsList.add(new SignificanceMeasureResults(new ChiSquared(), size));
			resultsList.add(new SignificanceMeasureResults(new KullbackLeiblerDivergence(), size));

			for (TermsFacet.Entry entry : termsFacet) {
				String term = entry.getTerm().string();
				if (dfMap.containsKey(term)) {
					int termDf = dfMap.get(term);
					// #docs which contain word with positive class
					long n11 = entry.getCount();
					// #docs which contain word with any class
					long n1X = termDf;
					// if the dfMap does not contain the word or is outdated, we give it at least the search results

					// frequency
					if (n1X < n11) {
						n1X = n11;
					}
					// #docs which contain word with negative class
					long n10 = n1X - n11;
					// #docs which do not contain word with positive class
					long n01 = numHits - n11;
					// #docs which do not contain word with any class
					long n0X = numDocs - n1X;
					// #docs which do not contain word with negative class
					long n00 = n0X - n01;
					// if the stats are outdated and the amount of recently added documents containing the current
					// word
					// is above average, the value of numDocs may be too small, so that n00 becomes negative
					if (n00 < 0) {
						n00 = 0;
					}

					double relativePositiveFreq = (1.0 + n11) / (1.0 + numHits);
					double relativeGlobalFreq = (1.0 + n1X) / (1.0 + numDocs);

					// Only consider positive deviation
					if ((relativePositiveFreq / relativeGlobalFreq) > 1.0) {
						for (SignificanceMeasureResults significanceMeasureResults : resultsList) {
							significanceMeasureResults.update(term, n00, n01, n10, n11);
						}
					}
				}
			}

			XContentBuilder builder = restContentBuilder(request);
			builder.startObject();
			for (SignificanceMeasureResults significanceMeasureResults : resultsList) {
				builder.startArray(significanceMeasureResults.significanceMeasure.shortName());
				for (ScoredTerm scoredTerm : reverse(significanceMeasureResults.priorityQueue)) {
					builder.startObject();
					builder.field("term", scoredTerm.getTerm());
					builder.field("score", scoredTerm.getScore());
					builder.field("n00", scoredTerm.getN00());
					builder.field("n01", scoredTerm.getN01());
					builder.field("n10", scoredTerm.getN10());
					builder.field("n11", scoredTerm.getN11());
					scoredTerm.addCustomFields(builder);
					builder.endObject();
				}
				builder.endArray();
			}
			builder.endObject();

			channel.sendResponse(new XContentRestResponse(request, OK, builder));
		}
	}

	//TODO: I'm sure there is a more elegant way to get desc ordering
	private <T> List<T> reverse(PriorityQueue<T> queue) {
		LinkedList<T> list = new LinkedList<>();
		T obj;
		while ((obj = queue.pop()) != null) {
			list.addFirst(obj);
		}
		return list;
	}

	private class SignificanceMeasureResults {
		private SignificanceMeasure significanceMeasure;
		private ScoredTermPriorityQueue priorityQueue;

		private SignificanceMeasureResults(SignificanceMeasure significanceMeasure, int size) {
			this.significanceMeasure = significanceMeasure;
			this.priorityQueue = new ScoredTermPriorityQueue(size);
		}

		public void update(String term, long n00, long n01, long n10, long n11) {
			ScoredTerm scoredTerm = this.significanceMeasure.apply(n00, n01, n10, n11);
			scoredTerm.setTerm(term);
			this.priorityQueue.insertWithOverflow(scoredTerm);
		}
	}

	private class ScoredTermPriorityQueue extends PriorityQueue<ScoredTerm> {
		public ScoredTermPriorityQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean lessThan(ScoredTerm a, ScoredTerm b) {
			return a.getScore() - b.getScore() < 0.0;
		}
	}

	private class TypeIdentifier {
		private String[] indices;
		private String[] types;

		private TypeIdentifier(String[] indices, String[] types) {
			this.indices = indices;
			this.types = types;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TypeIdentifier that = (TypeIdentifier) o;

			if (!Arrays.equals(indices, that.indices)) {
				return false;
			}
			if (!Arrays.equals(types, that.types)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = indices != null ? Arrays.hashCode(indices) : 0;
			result = 31 * result + (types != null ? Arrays.hashCode(types) : 0);
			return result;
		}
	}

	private class FieldIdentifier {
		private String[] indices;
		private String[] types;
		private String field;

		private FieldIdentifier(String[] indices, String[] types, String field) {
			this.indices = indices;
			this.types = types;
			this.field = field;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			FieldIdentifier that = (FieldIdentifier) o;

			if (field != null ? !field.equals(that.field) : that.field != null) {
				return false;
			}
			if (!Arrays.equals(indices, that.indices)) {
				return false;
			}
			if (!Arrays.equals(types, that.types)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = indices != null ? Arrays.hashCode(indices) : 0;
			result = 31 * result + (types != null ? Arrays.hashCode(types) : 0);
			result = 31 * result + (field != null ? field.hashCode() : 0);
			return result;
		}
	}
}
