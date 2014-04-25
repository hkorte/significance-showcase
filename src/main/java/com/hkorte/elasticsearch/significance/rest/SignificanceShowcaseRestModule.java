package com.hkorte.elasticsearch.significance.rest;

import org.elasticsearch.common.inject.AbstractModule;

/**
 * Created by hkorte on 29.03.14.
 */
public class SignificanceShowcaseRestModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(SignificanceShowcaseRestHandler.class).asEagerSingleton();
	}
}
