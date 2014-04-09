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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.pattern.AndPattern;
import org.sharegov.servicebot.pattern.OrPattern;
import org.sharegov.servicebot.pattern.PatternRecognizer;

public class UtteranceParser
{
	private static final UtteranceParser parser = new UtteranceParser();
	
	public static UtteranceParser get() { return parser; }
		
	/**
	 * <p>
	 * Infer matching patterns and returned them as a map to their scores.
	 * </p>
	 * 
	 * @param text
	 * @return
	 */
	public HGHandle toPatternRepresentation(String text)
	{
		HyperGraph graph = BotApp.get().getGraph();
		HashMap<HGHandle, Double> globalMap = new HashMap<HGHandle, Double>();
		
		List<PatternRecognizer> L = hg.getAll(graph, 
				hg.and(hg.typePlus(PatternRecognizer.class), hg.eq("enabled", true)));
		
		HashMap<HGHandle, Map<HGHandle, Double>> alternatives = 
			new HashMap<HGHandle, Map<HGHandle, Double>>();
		
		for (boolean done = false; !done; )
		{
			// Start afresh with all the patterns found so far and bundle them in one
			// global map
			HashMap<HGHandle, Double> newMap = new HashMap<HGHandle, Double>();
			for (Map<HGHandle, Double> M : alternatives.values())
				newMap.putAll(M);
			
			// Run each recognizer with this map as basis
			for (Iterator<PatternRecognizer> i = L.iterator(); i.hasNext(); )
			{
				PatternRecognizer recognizer = i.next();
				Map<HGHandle, Double> patterns = PatternRecognizer.asScoreMap(recognizer.detectPatterns(newMap, text));
				alternatives.put(recognizer.getAtomHandle(), patterns);
				newMap.putAll(patterns);
				if (recognizer.isInputOnlyBased())
					i.remove();
			}
			// if no more recognizers to run, we stop
			if (L.isEmpty())
				done = true;
			// if no new patterns have been discovered, we stop as well
			// perhaps a deeper, element by element, comparison will be needed here
			if (newMap.size() == globalMap.size())  
				done = true;
			globalMap = newMap;
		}	

		// could calculate and store the strength of the top or pattern here
		OrPattern or = new OrPattern();
		for (Map<HGHandle, Double> patterns : alternatives.values())
		{
			// could calculate and assign inference strength of the and alternative here
			if (patterns.size() > 1)
			{
				AndPattern and = new AndPattern();
				for (HGHandle ph : patterns.keySet())
					and.addAlternative(ph);			
				or.addAlternative(hg.assertAtom(graph, and, BotApp.get().findPatternType("all_of")));
			}
			else if (patterns.size() == 1)
				or.addAlternative(patterns.keySet().iterator().next());
		}
		return hg.assertAtom(graph, or, BotApp.get().findPatternType("one_of"));
	}	
}
