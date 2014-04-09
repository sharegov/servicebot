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
package org.sharegov.servicebot.pattern;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.sharegov.servicebot.UPattern;

public class PatternMatcher
{
	private HyperGraph graph;
	private ConcurrentHashMap<HGHandle, Double> matches = new ConcurrentHashMap<HGHandle, Double>();
	
	public PatternMatcher(HyperGraph graph)
	{
		this.graph = graph;
	}
	
	public PatternMatcher(HyperGraph graph, Map<HGHandle, Double> basePatterns)
	{
		this.graph = graph;
		matches.putAll(basePatterns);
		for (HGHandle topPattern : basePatterns.keySet())
		{
			UPattern p = graph.get(topPattern);
			if (p instanceof CompPattern)
				for (HGHandle subPattern : (CompPattern)p)
					matches.put(subPattern, 1.0);					
		}
	}
	
	public Double score(HGHandle patternHandle)
	{
		Double result = matches.get(patternHandle);
		if (result == null)
		{
			UPattern pattern = graph.get(patternHandle);
			PatternType patternType = pattern.getAtomType();
			MatchFunction f = patternType.getMatcher();
			if (f == null)
			{
				Double value = matches.get(patternHandle);
//				if (value == null)
//					System.out.println("didn't find " + pattern.prettyPrint(""));
				return value == null ? 0.0 : value;
			}
			result = f.match(graph, pattern, this);
			Double r2 = matches.putIfAbsent(patternHandle, result);
			if (r2 != null)
				result = r2;
		}
		return result;
	}
}
