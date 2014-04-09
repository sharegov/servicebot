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

import static mjson.Json.array;
import static mjson.Json.make;
import static mjson.Json.object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import mjson.Json;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HGSearchResult;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HyperNode;
import org.hypergraphdb.algorithms.HGALGenerator;
import org.hypergraphdb.algorithms.HGBreadthFirstTraversal;
import org.hypergraphdb.algorithms.HGTraversal;
import org.hypergraphdb.algorithms.HyperTraversal;
import org.hypergraphdb.query.impl.MappedResult;
import org.hypergraphdb.util.Mapping;
import org.hypergraphdb.util.Pair;
import org.sharegov.servicebot.Activates;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.BotRule;
import org.sharegov.servicebot.InputTransform;
import org.sharegov.servicebot.InteractionContext;
import org.sharegov.servicebot.InteractionFrame;
import org.sharegov.servicebot.LookupUtils;
import org.sharegov.servicebot.PrologUtils;
import org.sharegov.servicebot.StartUp;
import org.sharegov.servicebot.pattern.EnglishParser;

import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import alice.tuprolog.hgdb.PrologNode;
import disko.data.relex.RelOccurrence;
import disko.relex.PrologExporter;

/**
 * 
 * <p>
 * Administrative management of interaction frames, associated rules, forms etc. Equivalent
 * of the older "scenario management".
 * </p>
 *
 * <b>NOTE</b> : REST methods in this class all return a String rather a Json object (which is normally
 * streamed to the client with the JsonEntityProvider) because the Nginx proxy used by the chat bot 
 * inserts some numbers before and after a Json message and the browser fails to parse a Json mime type. 
 * That's a bug in nginx that I have no time to investigate/report right now...  
 * @author Borislav Iordanov
 *
 */
@Path("iframes")
@Produces("application/json")
@Consumes("application/json")
public class InteractionFrameService extends RestService
{	
	HyperGraph graph = BotApp.get().getGraph();
	PrologNode prologNode = BotApp.get().getPrologNode();

	@Path("/context")
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	public Json createContext(final Json context)
	{
		HGHandle existing = hg.findOne(graph, 
				hg.and(hg.type(InteractionContext.class), 
				hg.eq("name", context.at("name").asString())));
		if (existing != null)
			return ko("Context already exists.");
		InteractionContext ctx = new InteractionContext();
		ctx.setName(context.at("name").asString());
		ctx.setConfiguration(context.at("configuration", Json.object()).toString());
		HGHandle h = graph.add(ctx);
		return ok().set("data", Json.object("hghandle", h.getPersistent().toString()).with(context));
	}

	@Path("/context")
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	public Json updateContext(final Json context)
	{
		HGHandle existing = hg.findOne(graph, 
				hg.and(hg.type(InteractionContext.class), 
				hg.eq("name", context.at("name").asString())));
		if (existing == null)
			return ko("Context " + context.at("name") + " does not exists.");
		InteractionContext ctx = new InteractionContext();
		ctx.setName(context.at("name").asString());
		ctx.setConfiguration(context.at("configuration").toString());
		boolean success = graph.replace(existing, ctx);
		return ok().set("data", context).set("replaced", success);
	}
	
	@Path("/context/{handle}")
	@DELETE
	public Json deleteContext(@PathParam("handle") String handleAsString)
	{
		HGHandle h = graph.getHandleFactory().makeHandle(handleAsString);
		return graph.remove(h) ? ok():ko(); 
	}
	
	@Path("/context/{handle}")
	@GET
	public Json getContext(@PathParam("handle") String handleAsString)
	{
		InteractionContext ctx = null;
		if ("default".equals(handleAsString))
			ctx = LookupUtils.findContext(BotApp.config().at("defaultContext").asString());
		else
		{
			HGHandle ctxhandle = null;
			try
			{
				ctxhandle = graph.getHandleFactory().makeHandle(handleAsString);
			}
			catch (Throwable t)
			{
				ctxhandle = hg.findOne(graph, 
						hg.and(hg.type(InteractionContext.class), 
								hg.eq("name", handleAsString)));
			}
			if (ctxhandle == null)
				return ko("not found");
			ctx = graph.get(ctxhandle);
		}
		if (ctx == null)
			return ko("not found");
		return ok().set("name", ctx.getName())
				   .set("hghandle", handleAsString)
				   .set("configuration", Json.read(ctx.getConfiguration()));
	}	
	
	@Path("/contexts/all")
	@GET
	public Json getAllContext()
	{
		Json A = Json.array();
		List<InteractionContext> L = graph.getAll(hg.type(InteractionContext.class));
		for (InteractionContext ctx : L)
		{
			A.add(Json.object("name", ctx.getName(), 
					"hghandle", graph.getHandle(ctx).getPersistent().toString(),
					"configuration", Json.read(ctx.getConfiguration())));
		}
		return ok().set("data", A);
	}
	
	@Path("/validateProlog")
	@POST
	public String validatePrologTerm(final Json prolog)
	{
		try
		{
			Term.createTerm(prolog.at("text").asString());
			return object("ok", true).toString();
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();
		} 
	}

	public Json toJson(InteractionFrame f)
	{
		String h = graph.getHandle(f).getPersistent().toString();
		return object("name", (f.getName() == null || f.getName().length() == 0) ? h : f.getName(), 
				"id", h,
				"transform", object("name", f.getInputTransform().toString(), 
									"id", graph.getHandle(f.getInputTransform()).getPersistent().toString()),
				"output", f.getBotOutput().toString(),
				"rules", array());
	}
	
	@SuppressWarnings("unchecked")
	@Path("/transforms")
	@GET
	public String getInputTransforms()
	{
		Json A = array();
		System.out.println(hg.getAll(graph, 
				hg.subsumed(graph.getTypeSystem().getTypeHandle(InputTransform.class))));
		for (HGHandle h : (List<HGHandle>)(List<?>)hg.findAll(graph, hg.typePlus(InputTransform.class)))
		{
			A.add(object("name", graph.get(h).toString(), "id", h.getPersistent().toString()));
		}
		return A.toString();
	}

	@Path("/prevFrames/{frameHandle}")
	@GET
	public String getPrevFramesAsJson(@PathParam("frameHandle") String handleAsString)
	{
		List<InteractionFrame> L = getPrevFrames(graph.getHandleFactory().makeHandle(handleAsString));
		Json A = array();	
		for (InteractionFrame x : L)
			A.add(toJson(x));
		return A.toString();
	}
	
	public List<InteractionFrame> getPrevFrames(HGHandle frameHandle)
	{
		InteractionFrame frame = graph.get(frameHandle);
		String name = frame.getName();
		ArrayList<InteractionFrame> L = new ArrayList<InteractionFrame>();
		// We need to find all frames that contains a 'next' predicate in the action
		// portions of one of their rules. The next predicate will contain either 
		// this frame's name or this frame's handle as its single argument.
		HGHandle byHandle = prologNode.findOne(hg.and(hg.type(Struct.class), 
											hg.eq("name", frameHandle.getPersistent().toString())));
		if (byHandle != null)
			byHandle = prologNode.findOne(hg.and(hg.type(Struct.class), 
					hg.eq("name", "next"), hg.incident(byHandle)));
		HGHandle byName = prologNode.findOne(hg.and(hg.type(Struct.class), 
								hg.eq("name", name)));
		if (byName != null)
			byName = prologNode.findOne(hg.and(hg.type(Struct.class), 
					hg.eq("name", "next"), hg.incident(byName)));
		
		HGALGenerator gen = new HGALGenerator() {
			public HGSearchResult<Pair<HGHandle, HGHandle>> generate(HGHandle h)
			{
				return new MappedResult<HGHandle, Pair<HGHandle, HGHandle>>(
						graph.getIncidenceSet(h).getSearchResult(),
						new Mapping<HGHandle, Pair<HGHandle, HGHandle>>(){
							public Pair<HGHandle, HGHandle> eval(HGHandle h)
							{
								return new Pair<HGHandle, HGHandle>(h,h);
							}
						});
			}					
		};
		if (byHandle != null)
		{
			HGTraversal bfs = new HyperTraversal(graph, new HGBreadthFirstTraversal(byHandle, gen));
					//new DefaultALGenerator(graph, null, null));
//			HyperTraversal t = new HyperTraversal(graph, bfs);
			while (bfs.hasNext())
			{
				Object x = graph.get(bfs.next().getSecond());
				if (x instanceof InteractionFrame)
					L.add((InteractionFrame)x);
			}
		}
		
		if (byName != null)
		{
			HGTraversal bfs = new HyperTraversal(graph, new HGBreadthFirstTraversal(byName, gen)); 
			while (bfs.hasNext())
			{
				Object x = graph.get(bfs.next().getSecond());
				if (x instanceof InteractionFrame)
					L.add((InteractionFrame)x);
			}
		}
		return L;
	}
	
	@Path("/nextFrames/{frameHandle}")
	@GET
	public String getNextFrames(@PathParam("frameHandle") String handleAsString)
	{
		HGHandle frameHandle = graph.getHandleFactory().makeHandle(handleAsString);
		InteractionFrame frame = graph.get(frameHandle);
		Json A = array();
		for (BotRule rule : frame.getRules())
		{
			Struct action = rule.getAction();
			if (action.getName().equals("next"))
				A.add(toJson(LookupUtils.findFrame(((Struct)action.getArg(0)).getName())));
			else if (action.getName().equals(","))
				for (int i = 0; i < action.getArity(); i++)
				{
					if (! (action.getArg(i) instanceof Struct))
						continue;
					Struct s = (Struct)action.getArg(i);
					if (s.getName().equals("next"))
					{
						InteractionFrame nextf = LookupUtils.findFrame(((Struct)s.getArg(0)).getName());
						if (nextf != null)
							A.add(toJson(nextf));
						else
							// maybe this can be added as a warning to the end
							// user to be discretely displayed somewhere
							System.err.println("Missing next frame: " + ((Struct)s.getArg(0)).getName());
					}
				}
		}
		return A.toString();
	}
	
	@Path("/all")
	@GET
	public String getAllFrames(@QueryParam("context") String contextName)
	{
		Json A = array();
		HyperNode context = null;
		if (contextName != null )
		{
			HGHandle ctxh = hg.findOne(graph, 
					hg.and(hg.type(InteractionContext.class), 
					hg.eq("name", contextName)));
			if (ctxh != null)
				context = graph.get(ctxh);
			else
				return A.toString();
		}
		List<InteractionFrame> L = context.getAll(hg.type(InteractionFrame.class));
		for (InteractionFrame f : L)
		{			 
			Json j = toJson(f);
			for (BotRule rule : f.getRules())
			{
				String condition = rule.getCondition() == null ? "" : rule.getCondition().toString();
				String action = rule.getAction() == null ? "" : rule.getAction().toString();
				j.at("rules").add(object("id", graph.getHandle(rule).getPersistent().toString(),
										 "condition", rule.getCondition().toString(),
										 "action", rule.getAction().toString()));
				
			}
			A.add(j);
		}
		
		List<Json> list = A.asJsonList();
		Collections.sort(list, FrameNameComparator);
		return make(list).toString();
	}
	
	@PUT
	@Path("/frame")
	public String saveFrame(final Json frameData)
	{
		try
		{
			System.out.println("Save frame:" + frameData);
			return graph.getTransactionManager().ensureTransaction(new Callable<String>() {
			public String call() throws Exception {		
			
			HGHandle h = graph.getHandleFactory().makeHandle(frameData.at("id").asString());
			InteractionFrame frame = graph.get(h);
			HGHandle output = null;
			if (frame == null)
			{
				// create a new one
				frame = new InteractionFrame();
			}
			else
				output = frame.getTargetAt(0);
			frame.setBotOutput(prologNode.add(Term.createTerm(frameData.at("output").asString())));
			frame.setName(frameData.at("name").asString());
			if (frameData.at("transform").isObject())
				frame.setInputTransform(graph.getHandleFactory().makeHandle(frameData.at("transform").at("id").asString()));
			else
				frame.setInputTransform(graph.getHandleFactory().makeHandle(frameData.at("transform")/*.at("id")*/.asString()));
			if (output == null)
				graph.define(h, frame);
			else
			{
				graph.update(frame);
				if (hg.count(graph, hg.incident(output)) == 0)
					prologNode.remove(output);
			}
			return object("ok", true).toString();
			
			}});		
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();
		}
	}
	
	@DELETE
	@Path("/rule/{handle}")
	public String deleteRule(final @PathParam("handle") String handleAsString)
	{
		try
		{
			return graph.getTransactionManager().ensureTransaction(new Callable<String>() {
			public String call() throws Exception {		
			
			HGHandle handle = graph.getHandleFactory().makeHandle(handleAsString);
			BotRule rule = graph.get(handle);
			// Cleanup 
			graph.remove(handle, false);
			prologNode.release(rule.getTargetAt(0));
//			prologNode.release(rule.getTargetAt(1));
			return object("ok", true).toString();			

			}});		
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();			
		}
	}

	@PUT
	@Path("/rule")
	public String saveRule(final Json formData)
	{
		try
		{
			return graph.getTransactionManager().ensureTransaction(new Callable<String>() {
			public String call() throws Exception {		
			
			HGHandle handle = graph.getHandleFactory().makeHandle(formData.at("id").asString());
			BotRule rule = graph.get(handle);
			if (rule == null)
			{
				rule = new BotRule();
				rule.setCondition((Struct)Term.createTerm(formData.at("condition").asString()));
				rule.setAction((Struct)Term.createTerm(formData.at("action").asString()));
				graph.define(handle, rule);
			}
			else
			{
				HGHandle oldc = rule.getTargetAt(0), olda = rule.getTargetAt(1);
				rule.setCondition((Struct)Term.createTerm(formData.at("condition").asString()));
				rule.setAction((Struct)Term.createTerm(formData.at("action").asString()));
				graph.update(rule);
				// Cleanup 			
				prologNode.release(oldc);
				prologNode.release(olda);
			}
			return object("ok", true).toString();		
			}});		
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();			
		}
	}
	
	@POST
	@Path("/rule/{frameHandle}")
	public String createRule(final @PathParam("frameHandle") String frameHandle, final Json formData)
	{
		try
		{
			HGHandle condition = prologNode.add((Struct)Term.createTerm(formData.at("condition").asString()));
			HGHandle action = prologNode.add((Struct)Term.createTerm(formData.at("action").asString()));
			BotRule rule = new BotRule(condition, action);
			HGHandle ruleHandle = graph.add(rule);
			HGHandle frame = graph.getHandleFactory().makeHandle(frameHandle);
			graph.add(new Activates(frame, ruleHandle)); 
			return object("ok", true, "ruleHandle", ruleHandle.getPersistent().toString()).toString();			
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();			
		}
	}
	
	@POST
	@Path("/frame/new")
	public String newFrame(@QueryParam("context") String contextName)
	{
		InteractionContext context = LookupUtils.findContext(contextName);
		if (context == null)
			return ko("Context " + contextName + " could not be found.").toString();
		InteractionFrame frame = new InteractionFrame();
		InteractionFrame top = LookupUtils.findTopLevelFrame(context);
		if (top != null)
		{
			frame.setBotOutput(top.getTargetAt(0));
			frame.setInputTransform(top.getTargetAt(1));
		}
		else
		{
			frame.setBotOutput(prologNode.add(new Struct("utterance", new Struct("Hello"))));
			frame.setInputTransform((HGHandle)hg.findOne(graph, hg.typePlus(InputTransform.class)));
		}
		context.add(frame, graph.getTypeSystem().getTypeHandle(InteractionFrame.class), 0);
		return object("ok", true, "frame", toJson(frame)).toString();
	}
	
	public Json createFrame(Json frameSpec)
	{
		HGHandle [] targets = new HGHandle[] { 
				graph.getHandleFactory().nullHandle(),
				graph.getHandleFactory().nullHandle()
		};
		if (frameSpec.has("output"))
		{
			targets[0] = graph.add(PrologUtils.fromJson(frameSpec.at("output")));
		}
		if (frameSpec.has("inputTransform"))
		{
			targets[1] = graph.getHandleFactory().makeHandle(frameSpec.at("inputTransform").asString());
		}
		InteractionFrame frame = new InteractionFrame(targets);
		frameSpec.set("id", graph.add(frame).getPersistent().toString());
		return ok().set("frame", frameSpec);
	}

	public Json updateFrame(String frameId, Json frameSpec)
	{
		HGHandle frameHandle = graph.getHandleFactory().makeHandle(frameId);
		InteractionFrame frame = graph.get(frameHandle);
		if (frameSpec.has("output"))
		{
			frame.setBotOutput(graph.add(PrologUtils.fromJson(frameSpec.at("output"))));
		}
		if (frameSpec.has("inputTransform"))
		{
			frame.setInputTransform(graph.getHandleFactory().makeHandle(frameSpec.at("inputTransform").asString()));
		}		
		graph.update(frame);
		return ok();
	}
	
	@DELETE
	@Path("/frame/{id}")	
	public String deleteFrame(@PathParam("id") String frameId)
	{
		final InteractionFrame frame = LookupUtils.findFrame(frameId);
		if (frame == null)
			return ko("Frame not found: " + frameId).toString();
		final HGHandle frameHandle = graph.getHandle(frame);
		if (!getPrevFrames(frameHandle).isEmpty())
			return ko("Other frame depend on this frame (have it as a 'next' action)").toString();
		try
		{
			return graph.getTransactionManager().ensureTransaction(new Callable<String>() {
			public String call() throws Exception {		
			for (BotRule rule : frame.getRules())
				deleteRule(graph.getHandle(rule).getPersistent().toString());
			prologNode.release(frame.getTargetAt(0));
			graph.remove(frameHandle);
			return object("ok", true).toString();			
			}});		
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			return object("ok", false, "error", t.toString()).toString();			
		}
	}	
	
	public Json addRuleFromUtterance(String fromFrame, String toFrame, Json utterance)
	{
		HGHandle condition, action;
		List<Struct> one_of = new ArrayList<Struct>();
		if (utterance.has("text"))
		{
			EnglishParser parser = new EnglishParser();
			parser.parse(graph, utterance.at("text").asString());
			if (!parser.getParses().isEmpty())
			{
				List<Struct> relations = new ArrayList<Struct>();
				PrologExporter exporter = new PrologExporter();
				for (RelOccurrence occ : parser.getParses().get(0).getRelOccs())
					relations.add(exporter.toPlainStruct(graph, occ));
				one_of.add(new Struct("syntax_structure", relations.toArray(new Term[0])));				
			}			
			one_of.add(new Struct("utterance", new Struct(utterance.at("text").asString())));
		}
		// handle maybe some form responses...
		condition = prologNode.add(new Struct(";", one_of.toArray(new Term[0])));
		action = prologNode.add(new Struct("next", new Struct(toFrame)));
		BotRule rule = new BotRule(condition, action);
		HGHandle ruleHandle = graph.add(rule);
		HGHandle frameHandle = graph.getHandleFactory().makeHandle(fromFrame);
		graph.add(new Activates(frameHandle, ruleHandle));
		return ok();
	}
	
	public Json addRule(String frameId, Json ruleSpec)
	{
		return ok();
	}
	
	public Json deleteRule(String frameId, Json rule)
	{
		return ok();
	}
	
	public Json uploadDialogScenarios(Json scenarios)
	{
		for (Json scenario : scenarios.asJsonList())
		{
			
		}
		return ok();
	}

	public void exportFramesAsJson(File f)
	{
		try
		{
			FileWriter out = new FileWriter(f);			
			out.write(getAllFrames(null));			
			out.close();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	public void importFramesFromJson(File f)
	{
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(f));
			StringBuilder sb = new StringBuilder();
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				sb.append(line);
			Json frames = Json.read(sb.toString());
			for (final Json frame : frames.asJsonList())
			{
				final Json rules = frame.atDel("rules");
				graph.getTransactionManager().ensureTransaction(new Callable<Object>() {
					public Object call()
					{
						saveFrame(frame);
						if (rules == null || !rules.isArray())
							return null;
						HGHandle frameh = graph.getHandleFactory().makeHandle(frame.at("id").asString());						
						for (Json r : rules.asJsonList())
						{
							saveRule(r);
							HGHandle ruleh = graph.getHandleFactory().makeHandle(r.at("id").asString());
							graph.add(new Activates(frameh, ruleh));							
						}
						return null;
					}
				});
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public void exportFramesAsProlog(File f)
	{
		List<InteractionFrame> allframes = hg.getAll(graph, hg.type(InteractionFrame.class));
		try
		{
			FileWriter out = new FileWriter(f);
			for (InteractionFrame frame : allframes)
			{
				Struct frameId = new Struct("id", new Struct(graph.getHandle(frame).getPersistent().toString()));
				Struct frameFact = new Struct("frame", 
						frame.getBotOutput(), 
						new Struct("inputTransform",
								new Struct(frame.getInputTransform().getClass().getSimpleName()),
								new Struct(frame.getTargetAt(1).getPersistent().toString())),
								frameId);
				out.write(frameFact.toString() + ".\n");
				for (BotRule rule : frame.getRules())
				{
					out.write(new Struct("rule", frameId, 
							rule.getCondition(), rule.getAction()).toString() + ".\n");
				}
			}
			out.close();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public void importFramesFromProlog(File f)
	{
		try
		{
			FileInputStream in = new FileInputStream(f);
			Theory th = new Theory(in);
			Prolog prolog1 = new Prolog(), prolog2 = new Prolog();
			prolog1.setTheory(th);
			prolog2.setTheory(th);
			SolveInfo info = prolog1.solve(
				Term.createTerm("frame(Input, inputTransform(TName, TransformId), id(Id))")
			);
			while (info.isSuccess())
			{
				HGHandle frameHandle = graph.getHandleFactory().makeHandle(
						((Struct)info.getTerm("Id")).getName());
				HGHandle transformHandle = graph.getHandleFactory().makeHandle(
						((Struct)info.getTerm("TransformId")).getName());
				InteractionFrame frame = graph.get(frameHandle);
				if (frame == null)
				{
					frame = new InteractionFrame();
					graph.define(frameHandle, frame);
				}
				else
				{
					for (BotRule rule : frame.getRules())
					{
						HGHandle ruleHandle = graph.getHandle(rule);
						HGHandle activatesLink = hg.findOne(graph,
							hg.and(hg.type(Activates.class),
								 hg.orderedLink(frameHandle, ruleHandle))
						);
						graph.remove(rule.getTargetAt(1));
						graph.remove(rule.getTargetAt(0));
						graph.remove(ruleHandle);
						graph.remove(activatesLink);
					}
				}
				frame.setBotOutput(prologNode.add(info.getTerm("Input")));
				frame.setInputTransform(transformHandle);
				graph.update(frame);
				SolveInfo info2 = prolog2.solve(new Struct("rule", 
							new Struct("id", new Struct(frameHandle.getPersistent().toString())), 
							new Var("Condition"), new Var("Action")));
				while (info2.isSuccess())
				{
					Struct condition = (Struct)info2.getTerm("Condition");
					Struct action = (Struct)info2.getTerm("Action");
					BotRule rule = new BotRule(prologNode.add(condition), prologNode.add(action));
					graph.add(new Activates(frameHandle, graph.add(rule)));
					if (!prolog2.hasOpenAlternatives())
						break;
					info2 = prolog2.solveNext();
				}
				prolog2.solveHalt();
				if (!prolog1.hasOpenAlternatives())
					break;
				info = prolog1.solveNext();
			}
			prolog1.solveHalt();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	private static void testSomething()
	{
		PrologNode prologNode = BotApp.get().getPrologNode();
		System.out.println(prologNode.getAll(hg.and(hg.type(Struct.class), hg.eq("name", "test"))));
		HGHandle testh = prologNode.findOne(hg.and(hg.type(Struct.class), hg.eq("name", "test")));
		HGHandle blah = prologNode.findOne(hg.and(hg.type(Struct.class), hg.eq("name", "bla")));
		System.out.println("test: "  + testh);
		System.out.println("bla: "  + blah);
		Struct s = new Struct("test", new Struct("bla"));
		testh = prologNode.add(s);
		blah = prologNode.findOne(hg.and(hg.type(Struct.class), hg.eq("name", "bla")));
		System.out.println(prologNode.getIncidenceSet(blah).size());
//		for (HGHandle h : (List<HGHandle>)prologNode.findAll(hg.incident(testh)))
//			prologNode.release(h);
		if (testh != null)
			prologNode.release(testh);
		if (blah != null)
			prologNode.release(blah);
	}
	
	static void moveFramesToContext(String contextName)
	{
		HyperGraph graph = BotApp.get().getGraph();
		InteractionContext context = hg.getOne(graph, 
					hg.and(hg.type(InteractionContext.class), 
					hg.eq("name", contextName)));
		if (context == null)
			throw new RuntimeException("Non-existing context " + contextName);
		List<HGHandle> allframes = hg.findAll(graph, hg.type(InteractionFrame.class));
		for (HGHandle fh : allframes)
		{
			Set<HGHandle> enclosingContexts = hg.make(HGHandle.class, graph)
					.compile(hg.contains(fh)).findInSet();
			if (enclosingContexts.isEmpty())
				context.add(fh);
		}
	}
	
	public static void main(String [] argv)
	{
		try
		{
			Json config = Json.read(new String(StartUp.getBytesFromFile(new File(argv[0]))));
			BotApp.get().initialize(config);
			//testSomething();
			InteractionFrameService svc = new InteractionFrameService();
			//svc.exportFramesAsProlog(new File("c:/work/servicebot/data/frames.pl"));
			//moveFramesToContext("Economic Development");
			HyperGraph graph = BotApp.get().getGraph();
			InteractionContext context = LookupUtils.findContext("test");
			System.out.println(hg.getOne(graph, hg.and(hg.type(InteractionFrame.class), 
										/*hg.eq("name", "Welcome"),*/
										hg.memberOf(graph.getHandle(context)))));
			
			//svc.exportFramesAsJson(new File(BotApp.config().at("workingDir").asString() +
			//		"/data/frames.json"));
//			svc.importFramesFromJson(new File(BotApp.config().at("workingDir").asString() +
//					"/data/frames.json"));
//			List<InteractionFrame> L = hg.getAll(BotApp.get().getGraph(), 
//					hg.typePlus(InteractionFrame.class));
//			for (InteractionFrame i : L)
//				System.out.println(i.toString() + " - " + svc.ex);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	/**
	 * Comparator for sorting frames by name in ascending order.
	 * 
	 */
	public static Comparator<Json> FrameNameComparator = new Comparator<Json>()
	{
		public int compare(Json json1, Json json2)
		{			
			String name1 = (json1.at("name") != null) ? json1.at("name").toString() : "zz";
			String name2 = (json2.at("name") != null) ? json2.at("name").toString() : "zz";
			return name1.compareToIgnoreCase(name2);
		}
	};
}
