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
package org.sharegov.servicebot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.pattern.PatternMatcher;
import org.sharegov.servicebot.pattern.PatternRecognizer;

/**
 * <p>
 * Represents an instance of the virtual assistant talking to a user. 
 * </p>
 * 
 * @author boris
 *
 */
public class Bot
{
	private Conversation conversation = new Conversation();
	
	HGHandle getLastBeckyResponse()
	{
		List<DialogAction> actions = conversation.getDialogActions();
		for (int i = actions.size() - 1; i > -1; i--)
		{
			DialogAction a = actions.get(i);
			if (a instanceof BotResponse)
				return BotApp.get().getGraph().getHandle(a);
		}
		return null;
	}
	
	Set<HGHandle> getCandidates(HGHandle context)
	{
		HyperGraph graph = BotApp.get().getGraph();
		Set<HGHandle> C = new HashSet<HGHandle>();
		if (context == null)
		{
			List<HGHandle> scenarios = hg.findAll(graph, 
												  hg.type(Scenario.class));
			for (HGHandle scenario : scenarios)
			{
				List<HGHandle> triggers = hg.findAll(graph, hg.apply(hg.targetAt(graph, 0),
						  hg.and(hg.incident(scenario),
								 hg.type(Activates.class))));
				C.addAll(triggers);
			}
		}
		else
		{
			List<HGHandle> replies = hg.findAll(graph, hg.apply(hg.targetAt(graph, 0),
					 hg.and(hg.orderedLink(hg.anyHandle(), context),
						    hg.type(InReplyTo.class))));
			C.addAll(replies);
		}
		return C;
	}
	
	Map<HGHandle, Double> recognizePatterns(String text)
	{
		Map<HGHandle, Double> map = new HashMap<HGHandle, Double>();
		List<PatternRecognizer> L = hg.getAll(BotApp.get().getGraph(), 
				hg.and(hg.typePlus(PatternRecognizer.class), hg.eq("enabled", true)));
		for (PatternRecognizer rec : L)
			map.putAll(PatternRecognizer.asScoreMap(rec.detectPatterns(map, text)));
		return map;
	}
	
	public BotResponse hear(String text)
	{
		HyperGraph graph = BotApp.get().getGraph();
		Utterance userUtterance = graph.get(hg.assertAtom(graph, 
						new Utterance(text), BotApp.get().findPatternType("utterance_asis")));
		conversation.getDialogActions().add(userUtterance);
		Map<HGHandle, Double> patterns = recognizePatterns(text);
		HGHandle context = getLastBeckyResponse();
		Set<HGHandle> candidates = getCandidates(context);
		if (candidates.isEmpty())
			candidates = getCandidates(null); // try at the top-level if we reached a dead end here...
		HGHandle bestMatch = null;
		double bestScore = 0.0;
		for (HGHandle candidate : candidates)
		{
			PatternMatcher matcher = new PatternMatcher(BotApp.get().getGraph(), patterns);
			double score = matcher.score(candidate);
			if (score > bestScore)
			{
				bestScore = score;
				bestMatch = candidate;
				UPattern pat = graph.get(bestMatch);
				System.out.println("Found best: " + pat.prettyPrint(""));
			}
		}
		BotResponse result = null;
		if (bestMatch != null)
		{
			result = hg.getOne(graph, hg.apply(hg.targetAt(graph, 0), 
							hg.and(hg.type(InReplyTo.class), 
									hg.orderedLink(hg.anyHandle(), bestMatch))));
			conversation.getDialogActions().add(result);
		}
		else
			result = new BotUtterance("I don't understand.");
		return result;
	}
}
