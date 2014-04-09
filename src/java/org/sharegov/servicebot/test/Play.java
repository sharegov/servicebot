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
package org.sharegov.servicebot.test;


import static mjson.Json.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import mjson.Json;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGIndex;
import org.hypergraphdb.HGSearchResult;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.storage.BAtoHandle;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.BotResponse;
import org.sharegov.servicebot.BotUtterance;
import org.sharegov.servicebot.EnglishParseInputTransform;
import org.sharegov.servicebot.HtmlFormSubmitTransform;
import org.sharegov.servicebot.InputTransform;
import org.sharegov.servicebot.InteractionFrame;
import org.sharegov.servicebot.LookupUtils;
import org.sharegov.servicebot.Scenario;
import org.sharegov.servicebot.UPattern;
import org.sharegov.servicebot.pattern.CompPattern;
import org.sharegov.servicebot.pattern.TextPattern;
import org.sharegov.servicebot.rest.InteractionFrameService;
import org.sharegov.servicebot.train.BotCoach;

import alice.tuprolog.Struct;
import alice.tuprolog.hgdb.PrologNode;

public class Play
{
	public static final String dblocation = BotApp.config().at("workingDir").asString() + "/data/db";
	
	private static final boolean includeOnlyUtteranceSamples = true;
	
	private static Json patternToJson(UPattern p)
	{
		Json result = object().set("head", p.getClass().getSimpleName());
		if (p instanceof CompPattern)
		{
			Json args = result.at("arguments", array());
			for (HGHandle sub : ((CompPattern)p))
			{
				Json subPattern = patternToJson((UPattern)BotApp.get().getGraph().get(sub));
				if (!includeOnlyUtteranceSamples || subPattern.is("head", "Utterance"))
					args.add(subPattern);
			}
		}
		else
			result.set("text", ((TextPattern)p).getText());
		return result;
	}
	
	private static Json conversationBranch(BotCoach coach, HGHandle pattern)
	{
		HyperGraph graph = BotApp.get().getGraph();
		Json result = object();
		BotUtterance resp = (BotUtterance)coach.getBeckyResponse(pattern);
		if (resp == null)
			return nil();
		result.set("response", resp.getText());
		Json replies = result.at("replies", array());
		for (UPattern t : coach.getReplies(graph.getHandle(resp)))
		{
			Json p = patternToJson(t);
			replies.add(object().set("pattern", p)
						.set("branch", conversationBranch(coach, graph.getHandle(t))));
		}		
		return result;
	}
	
	static void exportScenariosToJson()
	{
		BotApp.get().setup(dblocation, false);
		HyperGraph graph = BotApp.get().getGraph();
		BotCoach coach = new BotCoach();
		Json all = array();
		for (Scenario sc : coach.getAllScenarios())
		{
			Json j = object()
				.set("title", sc.getTitle())
				.set("description", sc.getDescription()); 
			Json triggers = j.at("triggers", array());
			for (UPattern t : coach.getTriggers(sc.getAtomHandle()))
			{
				triggers.add(object().set("pattern", patternToJson(t))
						.set("branch", conversationBranch(coach, graph.getHandle(t))));
			}
			System.out.println(j);
			all.add(j);
		}
		try
		{
			FileWriter out = new FileWriter("c:/temp/jaimescenarios.json");
			out.write(all.toString());
			out.close();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}		
	}
	
	static void importJsonScenariosAsRules(File f) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuilder sb = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			sb.append(line);
		reader.close();
		Json json = Json.read(sb.toString());
		BotApp.get().initialize(BotApp.config());
		HyperGraph graph = BotApp.get().getGraph();
		PrologNode prologNode = BotApp.get().getPrologNode();
		InteractionFrameService ifservice = new InteractionFrameService();
		
		HGHandle parserTransform = hg.findOne(graph, hg.type(EnglishParseInputTransform.class));
		InteractionFrame topframe = LookupUtils.findTopLevelFrame(null);
		
		System.out.println("topframe=" + topframe + ", parsetransform=" + parserTransform);
		
		for (Json scenario : json.asJsonList())
		{
			List<Json> samples = new ArrayList<Json>();
			String response = null;
			for (Json trigger : scenario.at("triggers").asJsonList())
			{
				samples.add(trigger.at("pattern").at("arguments").at(0));
				if (trigger.has("branch") && !trigger.at("branch").isNull())
					response = trigger.at("branch").at("response").asString();
			}
			System.out.println(samples);
			if (samples.isEmpty() || response == null)
				continue;
			System.out.println(scenario);
			System.out.println("Resp: " + response);
			
			// Add frame and rule leading to it:
			InteractionFrame nextFrame = new InteractionFrame();
			nextFrame.setBotOutput(prologNode.add(new Struct("utterance", new Struct(response))));		
			nextFrame.setInputTransform(parserTransform);
			HGHandle nextHandle = graph.add(nextFrame);
			HGHandle topHandle = graph.getHandle(topframe);			
			for (Json s : samples)
				ifservice.addRuleFromUtterance(topHandle.getPersistent().toString(), 
											   nextHandle.getPersistent().toString(), 
											   Json.make(s));
		}
	}
	
	public static void main(String [] argv)
	{		
		//HyperGraph graph = HGEnvironment.get(dblocation);
		
//		HGIndex typeIndex = graph.getStore().getIndex(HyperGraph.TYPES_INDEX_NAME, 
//                BAtoHandle.getInstance(graph.getHandleFactory()), 
//                BAtoHandle.getInstance(graph.getHandleFactory()), 
//                null,
//                false);	       
//		
//		HGSearchResult<HGHandle> rs = typeIndex.scanKeys();
//		while (rs.hasNext())
//			System.out.println(graph.get(rs.next()));
//		rs.close();
		try
		{
			//exportScenariosToJson();
			
			//importJsonScenariosAsRules(new File(BotApp.config().at("workingDir").asString() + "/data/jaimescenarios.json"));
			
			BotApp.get().initialize(BotApp.config());
			HyperGraph graph = BotApp.get().getGraph();
			InteractionFrame f = LookupUtils.findTopLevelFrame(null);
			//f.setName("Welcome Frame");
			System.out.println("Name:" + f.getName());
			//graph.remove(graph.getTypeSystem().getTypeHandle(HtmlFormSubmitTransform.class));
			graph.add(new HtmlFormSubmitTransform());
			//graph.remove((HGHandle)hg.findOne(graph, hg.type(HtmlFormSubmitTransform.class)));
			System.out.println(hg.getAll(graph, 
					hg.subsumed(graph.getTypeSystem().getTypeHandle(InputTransform.class))));
			
			System.out.println("Transforms: " + hg.getAll(BotApp.get().getGraph(), hg.typePlus(InputTransform.class)));
			//BotApp.get().getGraph().update(f);
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
		}
	}
}
