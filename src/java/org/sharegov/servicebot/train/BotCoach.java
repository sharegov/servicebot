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
package org.sharegov.servicebot.train;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.Activates;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.BotResponse;
import org.sharegov.servicebot.BotUtterance;
import org.sharegov.servicebot.InReplyTo;
import org.sharegov.servicebot.Scenario;
import org.sharegov.servicebot.UPattern;
import org.sharegov.servicebot.Utterance;
import org.sharegov.servicebot.UtteranceParser;
import org.sharegov.servicebot.pattern.OrPattern;
import org.sharegov.servicebot.pattern.parser.PatternParser;

/**
 * 
 * <p>
 * Main entry point for managing dialog scenarios. Each scenario is a top-level conversation
 * thread. A conversation is assumed to be a sequence of alternating user and Bot agent
 * utterances. The user utterances are analyzed and translated into "utterance patterns", i.e.
 * {@link UPattern} instances. A UPattern instance corresponding to a user utterance is
 * usually a complex pattern, a logical expression of sub-patterns ultimately bottoming out
 * in "simple patterns". The simplest "simple pattern" is the utterance itself, verbatim. Other
 * possible simple patterns are word occurrences or syntactic relations, or semantic relations
 * inferred from context and from the utterance syntax.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class BotCoach
{ 
	private HyperGraph graph = BotApp.get().getGraph();
	
	public List<Scenario> getAllScenarios()
	{
		return hg.getAll(graph, hg.type(Scenario.class));
	}
	
	public List<Scenario> findScenarios(String title, 
									    String createdBy,
									    Date createdOn,
									    String lastModifiedBy,
									    Date lastModifiedOn)
	{
		try
		{
			List<Scenario> result = hg.getAll(graph, hg.type(Scenario.class));
			return result;
		}
		catch (RuntimeException ex)
		{
			System.err.println("While getting scenarios");
			ex.printStackTrace(System.err);
			throw ex;
		}		
	}
	
	public List<Scenario> findByTrigger(UPattern pattern)
	{
		ArrayList<Scenario> result = new ArrayList<Scenario>();
		return result;		
	}
	
	public List<Scenario> findByTrigger(String utterance)
	{
		return findByTrigger(new Utterance(utterance));
	}
	
	public Scenario createScenario(String title, String description, String user)
	{
		Scenario scenario = new Scenario();
		scenario.setTitle(title);
		scenario.setCreatedBy(user);
		scenario.setCreatedOn(new Date());
		scenario.setDescription(description);
		graph.add(scenario);
		return scenario;
	}
	
	public void saveScenario(HGHandle h, String title, String description, String user)
	{
		Scenario scenario = graph.get(h);
		scenario.setTitle(title);
		scenario.setDescription(description);
		scenario.setLastModifiedBy(user);
		scenario.setLastModifiedOn(new Date());
		graph.update(scenario);		
	}
	
	public void deleteScenario(HGHandle scenario)
	{
		for (UPattern t : getTriggers(scenario))
			deleteBranch(graph.getHandle(t));
		graph.remove(scenario);
	}
	
	public void deleteBeckyResponse(HGHandle beckyResponse)
	{
		for (UPattern t : getReplies(beckyResponse))
			deleteBranch(graph.getHandle(t));
		graph.remove(beckyResponse);		
	}
	
	public void deleteBranch(HGHandle branchHandle)
	{
		BotResponse resp = getBeckyResponse(branchHandle);
		if (resp != null)
			deleteBeckyResponse(graph.getHandle(resp)); 
		graph.remove(branchHandle);		
	}
	
//	public Map<HGHandle, Double> getMatchingPatterns(Utterance utterance)
//	{
//		return UtteranceParser.get().analyze(utterance.getAtomHandle());
//	}
//	
//	private UPattern analyzeUtterance(HGHandle utteranceHandle)
//	{
//		final Map<HGHandle, Double> patterns = UtteranceParser.get().analyze(utteranceHandle);
//		AndPattern and = new AndPattern(new HGHandle[]{});
//		for (Map.Entry<HGHandle, Double> e : patterns.entrySet())
//		{
//			if (!e.getKey().equals(utteranceHandle))
//			{
//				graph.add(new InferredFrom(e.getValue(), e.getKey(), utteranceHandle));
//				and.addAlternative(e.getKey());
//			}
//		}
//		return and;
//	}
	
	public void addToPattern(HGHandle pattern, String text)
	{
		HGHandle patternHandle = UtteranceParser.get().toPatternRepresentation(text);
		OrPattern p = graph.get(pattern);
		// keep this and-or nesting as a proper tree structure so that each
		// utterance is clearly associated with the pattern inferred from it
		// this is just convenient for the UI, e.g. to remove a sample utterance
		// and all ensuing patterns, but clearly it's not the most efficient for
		// evaluation, especially since repeats are possible
		p.addAlternative(patternHandle);
		
		// the other possibility is the following:
//		OrPattern newPattern = graph.get(patternHandle);
//		for (HGHandle alt : newPattern)
//			p.addAlternative(alt);
//		graph.remove(patternHandle);		
		graph.update(p);
	}
	
	public UPattern addTrigger(HGHandle context, String text)	
	{
		HGHandle pattern = UtteranceParser.get().toPatternRepresentation(text);
		hg.assertAtom(graph, new Activates(pattern, context));
		return graph.get(pattern);
	}
	
	public List<UPattern> getTriggers(HGHandle context)
	{
		try
		{
			List<UPattern> L = hg.getAll(graph, hg.apply(hg.targetAt(graph, 0),
														  hg.and(hg.incident(context),
																 hg.type(Activates.class))));
			return L;			
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace(System.err);
			throw ex;
		}
	}
	
	public UPattern addReply(HGHandle beckyResponse, String text)	
    {
    	HGHandle ph = UtteranceParser.get().toPatternRepresentation(text);
    	hg.assertAtom(graph, new InReplyTo(ph, beckyResponse));
    	return graph.get(ph);
    }
	
	public List<UPattern> getReplies(HGHandle beckyResponse)
	{
		List<UPattern> L = hg.getAll(graph, hg.apply(hg.targetAt(graph, 0),
													 hg.and(hg.orderedLink(hg.anyHandle(), beckyResponse),
														    hg.type(InReplyTo.class))));
		return L;
	}
	
	public BotResponse setBeckyResponse(HGHandle pattern, String text)
	{
		BotUtterance result = new BotUtterance(text);
		HGHandle utteranceHandle = hg.assertAtom(graph, result, BotApp.get().findPatternType("becky_utterance"));
		BotResponse response = getBeckyResponse(pattern);
		if (response != null)
			graph.remove(graph.getHandle(response));
		graph.add(new InReplyTo(utteranceHandle, pattern));
		return result;
	}
	
	public BotResponse getBeckyResponse(HGHandle pattern)
	{
		return hg.getOne(graph, hg.apply(hg.targetAt(graph, 0), 
										 hg.and(hg.type(InReplyTo.class), 
												hg.orderedLink(hg.anyHandle(), pattern))));
	}
				
	/**
	 * <p>
	 * Return all utterances that are part of a given pattern. Such utterances are normally
	 * also used to construct other parts of the pattern.
	 * </p>
	 * @param pattern
	 * @return
	 */
	public List<Utterance> getPatternUtterances(HGHandle pattern)
	{
		HashSet<HGHandle> visited = new HashSet<HGHandle>();
		LinkedList<HGHandle> toexplore = new LinkedList<HGHandle>();
		List<Utterance> result = new ArrayList<Utterance>();
		toexplore.add(pattern);
		while (!toexplore.isEmpty())
		{
			HGHandle current = toexplore.removeLast();
			if (visited.contains(current))
				continue;
			Object x = graph.get(current);
			if (x instanceof Utterance)
				result.add((Utterance)x);
			if (x instanceof HGLink)
			{
				HGLink l = (HGLink)x;
				for (int i = 0; i < l.getArity(); i++)
					if (!visited.contains(l.getTargetAt(i)))
						toexplore.add(l.getTargetAt(i));
			}
		}
		return result;
	}	
	
	public String getPatternTextForm(HGHandle pattern)
	{
		UPattern p = graph.get(pattern);
		if (p == null)
			return "null";
		else
			return p.prettyPrint("");
	}
	
	public void deleteUnusedPatterns(HGHandle patternHandle)
	{
		if (!graph.getIncidenceSet(patternHandle).isEmpty())
			return;
		UPattern pattern = graph.get(patternHandle);
		graph.remove(patternHandle);
		if (pattern instanceof HGLink)
		{
			HGLink link =  (HGLink)pattern;
			for (int i = 0; i < link.getArity(); i++)
				deleteUnusedPatterns(link.getTargetAt(i));
		}
	}
	
	public UPattern setPatternFromTextForm(final HGHandle pattern, final String asText)
	{
		final HGHandle result = new PatternParser(asText, graph).parse();
		if (pattern.equals(result)) // no changes detected
			return graph.get(pattern);
		return graph.getTransactionManager().ensureTransaction(new Callable<UPattern>(){
		public UPattern call()
		{
			for (HGHandle linkHandle : graph.getIncidenceSet(pattern))
			{
				HGLink link = graph.get(linkHandle);
				if (link instanceof Activates)
				{
					((Activates)link).changeTrigger(result);
					graph.update(link);
				}
				else if (link instanceof InReplyTo)
				{
					InReplyTo reply = (InReplyTo)link;
					if (pattern.equals(reply.getStimulus()))
						reply.changeStimulus(result);
					else
						reply.changeResponse(result);
					graph.update(link);
				}
			}
			deleteUnusedPatterns(pattern);
			return graph.get(result);
		}});
	}
	
	/**
	 * Gets a handle to an atom in graph from its UUID string.
	 * 
	 * @author Alfonso Boza    <ABOZA@miamidade.gov>
	 * @param  uuid    Handle's UUID string.
	 * @return         Graph handle to node with UUID.
	 */
	public HGHandle getHandleFromUUID(String uuid)
	{
	    return graph.getHandleFactory().makeHandle(uuid);
	}
}
