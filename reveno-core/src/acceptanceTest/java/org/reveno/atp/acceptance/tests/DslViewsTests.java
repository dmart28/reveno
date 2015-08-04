package org.reveno.atp.acceptance.tests;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.commands.dynamic.DynamicCommand;
import org.reveno.atp.utils.MapUtils;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class DslViewsTests extends RevenoBaseTest {
	
	@Test
	public void onDemandViewCreationTest() throws Exception {
		TestRevenoEngine reveno = new TestRevenoEngine(tempDir);
		reveno.config().modelType(ModelType.MUTABLE);
		
		DynamicCommand createPage = reveno.domain().transaction("createPage", (tx, ctx) -> {
		    ctx.repository().store(tx.id(), new Page(tx.id(), tx.arg()));
		}).uniqueIdFor(Page.class).command();
		DynamicCommand createLink = reveno.domain().transaction("createLink", (tx, ctx) -> {
			if (tx.opArg("page").isPresent()) {
		    	ctx.repository().get(Page.class, tx.arg("page")).ifPresent(p -> p.linksIds.add(tx.arg("page")));
		    }
		    ctx.repository().store(tx.id(), new Link(tx.id(), tx.arg("name"), tx.arg("url")));
		}).uniqueIdFor(Link.class).command();
		
		reveno.domain().viewMapper(Link.class, LinkView.class, (e,o,r) -> new LinkView(e.id, e.name, e.url));
		reveno.domain().viewMapper(Page.class, PageView.class, (e,o,r) -> 
			new PageView(e.id, e.name, r.dynamic(e.linksIds.stream(), LinkView.class, Collectors.toList()));
		
		reveno.startup();
		
		long pageId = reveno.executeSync(createPage, MapUtils.map("name", "Open Source"));
		long linkId = reveno.executeSync(createLink, MapUtils.map("name", "reveno", "url", "http://reveno.org", "page", pageId));
		
		Assert.assertTrue(reveno.query().find(PageView.class, pageId).isPresent());
		PageView page = reveno.query().find(PageView.class, pageId).get();
		Assert.assertEquals(page.links.size(), 1);
		Assert.assertEquals(page.links.get(0).id, linkId);
	}
	
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
		reveno.domain().viewMapper(Page.class, PageView.class, (e,o,r) -> new PageView(e.id, e.name, null));
		
		reveno.startup();
		
		long pageId = reveno.executeSync(createPage, MapUtils.map("name", "Wikipedia"));
		Assert.assertEquals(1L, pageId);
		PageView view = reveno.query().find(PageView.class, pageId).get();
		Assert.assertEquals("Wikipedia", view.name);
		
		reveno.shutdown();
		
		reveno = new TestRevenoEngine(tempDir);
		reveno.config().modelType(ModelType.MUTABLE);
		createPage = f.apply(reveno);
		reveno.domain().viewMapper(Page.class, PageView.class, (e,o,r) -> new PageView(e.id, e.name, null));
		
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
		public LongList linksIds = new LongArrayList();
		
		public Page(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
	public static class Link {
		public long id;
		public String name;
		public String url;
		
		public Link(long id, String name, String url) {
			this.id = id;
			this.name = name;
			this.url = url;
		}
	}
	
	public static class PageView {
		public final long id;
		public final String name;
		public final List<LinkView> links;
		
		public PageView(long id, String name, List<LinkView> links) {
			this.id = id;
			this.name = name;
			this.links = links;
		}
	}
	
	public static class LinkView {
		public final long id;
		public final String name;
		public final String url;
		
		public LinkView(long id, String name, String url) {
			this.id = id;
			this.name = name;
			this.url = url;
		}
	}
	
}
