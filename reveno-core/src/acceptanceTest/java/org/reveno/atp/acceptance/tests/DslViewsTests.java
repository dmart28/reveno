package org.reveno.atp.acceptance.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.commands.dynamic.DynamicCommand;
import org.reveno.atp.utils.MapUtils;

public class DslViewsTests extends RevenoBaseTest {
	
	@Test
	public void shortestSyntax() throws Exception {
		dslTest(this::createCommandShort);
	}
	
	@Test
	public void longestSyntax() throws Exception {
		dslTest(this::createCommandLong);
	}
	
	public void dslTest(Function<TestRevenoEngine, DynamicCommand> f) throws Exception {
		TestRevenoEngine reveno = new TestRevenoEngine(tempDir);
		
		reveno.config().modelType(ModelType.MUTABLE);
		DynamicCommand createPage = f.apply(reveno);
		reveno.domain().viewMapper(Page.class, PageView.class, (e,o,r) -> new PageView(e.name, null));
		
		reveno.startup();
		
		long pageId = reveno.executeSync(createPage, MapUtils.map("name", "Wikipedia"));
		Assert.assertEquals(1L, pageId);
		PageView view = reveno.query().find(PageView.class, pageId).get();
		Assert.assertEquals("Wikipedia", view.name);
		
		reveno.shutdown();
		
		reveno = new TestRevenoEngine(tempDir);
		reveno.config().modelType(ModelType.MUTABLE);
		createPage = f.apply(reveno);
		reveno.domain().viewMapper(Page.class, PageView.class, (e,o,r) -> new PageView(e.name, null));
		
		reveno.startup();
		
		view = reveno.query().find(PageView.class, pageId).get();
		Assert.assertEquals("Wikipedia", view.name);
		
		reveno.shutdown();
	}

	private DynamicCommand createCommandShort(TestRevenoEngine reveno) {
		DynamicCommand createPage = reveno.domain().transaction("createPage", (tx, ctx) -> {
		    ctx.repository().store(tx.id(), new Page(tx.id(), tx.arg()));
		}).uniqueIdFor(Page.class).command();
		return createPage;
	}
	
	private DynamicCommand createCommandLong(TestRevenoEngine reveno) {
		DynamicCommand createPage = reveno.domain().transaction("createPage", (tx, ctx) -> {
		    ctx.repository().store(tx.id(), new Page(tx.id(Page.class), tx.arg("name")));
		}).uniqueIdFor(Page.class).returnsIdOf(Page.class).command();
		return createPage;
	}
	
	public static class Page {
		public long id;
		public String name;
		public List<Integer> linksIds = new ArrayList<>();
		
		public Page(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
	public static class Link {
		public long id;
		public String name;
		public String url;
	}
	
	public static class PageView {
		public final String name;
		public final List<LinkView> views;
		
		public PageView(String name, List<LinkView> views) {
			this.name = name;
			this.views = views;
		}
	}
	
	public static class LinkView {
		public final String name;
		public final String url;
		
		public LinkView(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}
	
}
