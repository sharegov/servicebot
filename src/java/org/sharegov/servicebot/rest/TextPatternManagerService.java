/*******************************************************************************
 * Copyright 2014 Miami-Dade County
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.sharegov.servicebot.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.restlet.resource.Options;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.UPattern;
import static mjson.Json.*;
import mjson.Json;
import org.sharegov.servicebot.pattern.NamedPattern;
import org.sharegov.servicebot.pattern.PatternMatcher;
import org.sharegov.servicebot.pattern.PatternRecognizer;
import org.sharegov.servicebot.pattern.PhraseRecognizer;
import org.sharegov.servicebot.pattern.RegexRecognizer;
import org.sharegov.servicebot.pattern.parser.ParseError;
import org.sharegov.servicebot.pattern.parser.PatternParser;

import disko.ContentDownloader;

@Path("patterns")
@Produces("application/json")
public class TextPatternManagerService extends RestService
{
	private HyperGraph graph = BotApp.get().getGraph();
	
	Map<HGHandle, Double> recognizePatterns(String text)
	{
		Map<HGHandle, Double> map = new HashMap<HGHandle, Double>();
//		List<PatternRecognizer> L = hg.getAll(BotApp.get().getGraph(), 
//				hg.and(hg.typePlus(PatternRecognizer.class), hg.eq("enabled", true)));
//		for (PatternRecognizer rec : L)		
		PatternRecognizer rec = new PhraseRecognizer();
		rec.setHyperGraph(graph);
		map.putAll(PatternRecognizer.asScoreMap(rec.detectPatterns(map, text)));
		rec = new RegexRecognizer();
		rec.setHyperGraph(graph);		
		map.putAll(PatternRecognizer.asScoreMap(rec.detectPatterns(map, text)));		
		return map;
	}

	private Json asjson(NamedPattern p)
	{
		String expression = "";
		if (p.getPattern() != null)
		{
			UPattern pp = graph.get(p.getPattern());
			expression = p.getPattern() != null ? pp.prettyPrint("") : "";
		}
		return object().set("name", p.getName())
					   .set("description", p.getDescription())
					   .set("metadata", p.getMetadata())
					   .set("expression", expression);		
	}
	
	@Options
	@Path("/matchtext")
	public Response optionsRequest(@Context HttpHeaders headers, 
								   @Context Request request)
	{
		String method = headers.getRequestHeader("access-control-request-method").get(0);
		String origin = headers.getRequestHeader("origin").get(0);
//		try { URL url = new URL(origin); origin = url.getHost() + ":" + url.getPort(); }
//		catch (Throwable t) { t.printStackTrace(System.err); } 
		System.out.println("Request from " + origin + " for method " + method);
//		httpresp.addHeader("Access-Control-Allow-Origin", origin);
//		httpresp.addHeader("Access-Control-Allow-Methods", method);
//		httpresp.addHeader("Content-Type", "application/json");
//		httpresp.addHeader("Access-Control-Max-Age", "60000");
		return Response.ok().header("Access-Control-Allow-Origin", origin)
							.header("Access-Control-Allow-Methods", method)
							.header("Content-Type", "application/json")
							.header("Access-Control-Max-Age", 60000)
							.build();
	}
	
	@GET
	@Path("/matchurl/{url}")
	public Response queryByUrl(@PathParam("url") String url, @FormParam("threshold") double threshold)
	{
		String text = ContentDownloader.getInstance().readText(url);
		return query(text, threshold);
	}

	@GET
	@Path("/matchget")
	public Response queri(@QueryParam("text") String text, 
				   		  @QueryParam("threshold") double threshold)
	{
		Json A = array();
		List<NamedPattern> L = hg.getAll(graph, 
				hg.and(hg.type(NamedPattern.class), hg.not(hg.eq("pattern", null))));
		Map<HGHandle, Double> detected = recognizePatterns(text);
		PatternMatcher matcher = new PatternMatcher(graph, detected);
		for (NamedPattern p : L)
		{
			Double score = matcher.score(p.getPattern());
			if (score != null && score >= threshold)
				A.add(asjson(p).set("score", score));
		}
		return ok(A);
	}
	
	@POST
	@Path("/matchtext")
	public Response query(@FormParam("text") String text, 
						  @FormParam("threshold") double threshold)
	{
		return queri(text, threshold);
	}
	
	@GET
	@Path("/list")
	public Response list()
	{
		Json A = array();
		List<NamedPattern> L = hg.getAll(graph, hg.type(NamedPattern.class));
		for (NamedPattern p : L)
			A.add(asjson(p));
		return ok(A);
	}
	
	@GET
	@Path("/get/{name}")
	public Response get(@PathParam("name") String name)
	{
		HGHandle h = hg.findOne(graph, hg.and(hg.type(NamedPattern.class), hg.eq("name", name)));
		if (h == null)
			return ok(nil());
		NamedPattern pattern = graph.get(h);
		return ok(asjson(pattern));
	}
	
	@POST
	@Path("/save")
	public Response save(@FormParam("name") String name, 
					     @FormParam("description") String description,
					     @FormParam("metadata") String metadata,
					     @FormParam("expression") String expression)
	{
		try
		{
			HGHandle pattern = expression != null && expression.trim().length() > 0 ?
					new PatternParser(expression, graph).parse() : null;
			HGHandle namedHandle = hg.findOne(graph, hg.and(hg.type(NamedPattern.class), hg.eq("name", name)));
			NamedPattern named = (namedHandle == null) ? new NamedPattern() : (NamedPattern)graph.get(namedHandle); 
			named.setName(name);
			named.setDescription(description);
			named.setMetadata(metadata);
			named.setPattern(pattern);
			if (namedHandle != null)
				graph.update(named);
			else
				graph.add(named);
			return ok(object().set("success", true));
		}
		catch (ParseError err)
		{
			return ok(object().set("success", false)
						      .set("error", err.getMessage()));
		}
		catch (Throwable t)
		{
			return ok(object().set("success", false)
						      .set("error", t.toString()));			
		}
	}
	
	@DELETE
	@Path("/delete/{name}")
	public Response remove(@PathParam("name") String name)
	{
		HGHandle h = hg.findOne(graph, hg.and(hg.type(NamedPattern.class), hg.eq("name", name)));
		if (h != null)
			graph.remove(h);
		return ok(object().set("success", true));		
	}
}
