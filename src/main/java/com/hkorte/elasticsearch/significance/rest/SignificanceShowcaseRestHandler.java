package com.hkorte.elasticsearch.significance.rest;

import com.hkorte.elasticsearch.significance.SignificantTermsProvider;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.*;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 * Created by hkorte on 29.03.14.
 */
public class SignificanceShowcaseRestHandler extends BaseRestHandler {

	private final ESLogger logger;
	private final SignificantTermsProvider significantTermsProvider;

	@Inject
	public SignificanceShowcaseRestHandler(Settings settings, Client client, RestController controller) {
		super(settings, client);
		this.logger = Loggers.getLogger(getClass(), settings);
		this.significantTermsProvider = new SignificantTermsProvider(settings, client);

		controller.registerHandler(GET, "/_significance", this);
		controller.registerHandler(POST, "/_significance", this);
		controller.registerHandler(GET, "/{index}/_significance", this);
		controller.registerHandler(POST, "/{index}/_significance", this);
		controller.registerHandler(GET, "/{index}/{type}/_significance", this);
		controller.registerHandler(POST, "/{index}/{type}/_significance", this);

	}

	@Override
	public void handleRequest(final RestRequest request, final RestChannel channel) throws ExecutionException,
			IOException {
		String[] indices = Strings.splitStringByCommaToArray(request.param("index"));
		String[] types = Strings.splitStringByCommaToArray(request.param("type"));

		BytesReference data = request.content();
		XContent xContent = XContentFactory.xContent(data);
		XContentParser parser = xContent.createParser(data);
		XContentParser.Token token;
		// default values
		String query = "{\"match_all\":{}}";
		String field = "_all";
		int size = 20;
		String currentFieldName = null;
		while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
			if (token == XContentParser.Token.FIELD_NAME) {
				currentFieldName = parser.currentName();
			} else if ("query".equals(currentFieldName)) {
				if (token == XContentParser.Token.START_OBJECT && !parser.hasTextCharacters()) {
					XContentBuilder builder = XContentBuilder.builder(parser.contentType().xContent());
					builder.copyCurrentStructure(parser);
					query = builder.string();
				} else {
					query = parser.text();
				}
			} else if ("size".equals(currentFieldName)) {
				size = parser.intValue();
			} else if ("field".equals(currentFieldName)) {
				field = parser.text();
			}
		}

		this.significantTermsProvider.writeSignificantTerms(channel, indices, types, field, size, query);
	}
}
