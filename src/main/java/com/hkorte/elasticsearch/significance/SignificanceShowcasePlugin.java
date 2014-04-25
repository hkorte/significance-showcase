package com.hkorte.elasticsearch.significance;

import com.hkorte.elasticsearch.significance.rest.SignificanceShowcaseRestModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by hkorte on 29.03.14.
 */
public class SignificanceShowcasePlugin extends AbstractPlugin {
	@Override public String name() {
		return "significance-showcase-plugin";
	}

	@Override public String description() {
		return "Significance Showcase Plugin Description";
	}

	@Override
	public Collection<Class<? extends Module>> modules() {
		Collection<Class<? extends Module>> modules = new ArrayList<>();
		modules.add(SignificanceShowcaseRestModule.class);
		return modules;
	}
}
