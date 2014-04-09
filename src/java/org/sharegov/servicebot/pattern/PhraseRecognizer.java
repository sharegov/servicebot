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

import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.util.Pair;

public class PhraseRecognizer extends PatternRecognizer
{
	Set<Pair<Integer, Integer>> findAll(String phrase, String input)
	{
		Set<Pair<Integer, Integer>> S = new HashSet<Pair<Integer, Integer>>();
		Matcher matcher = Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE).matcher(input);
		while (matcher.find())
			S.add(new Pair<Integer, Integer>(matcher.start(), matcher.end()));
		return S;
	}
	
	public Set<PatternOccurrence> detectPatterns(Map<HGHandle, Double> context,
												 String input)
	{
		Set<PatternOccurrence> S = new HashSet<PatternOccurrence>();
		HGHandle patternType = hg.findOne(graph, hg.and(hg.type(PatternType.class), 
				 					hg.eq("head", "phrase")));
		
		List<PhrasePattern> all = hg.getAll(graph, hg.type(patternType));
		for (PhrasePattern p : all)
		{
			Set<Pair<Integer, Integer>> occurrences = findAll(p.getText(), input);
			for (Pair<Integer, Integer> match : occurrences)
				S.add(new PatternOccurrence(p.getAtomHandle(), 1.0, match.getFirst(), match.getSecond()));
		}
		return S;
	}
}
