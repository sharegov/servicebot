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
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.Utterance;

public class UtteranceRecognizer extends PatternRecognizer
{
	public UtteranceRecognizer()
	{
		this.inputOnlyBased = true;
	}
	
	public Set<PatternOccurrence> detectPatterns(Map<HGHandle, Double> context,
											     String input)
	{
		HGHandle utteranceType = hg.findOne(graph, hg.and(hg.type(PatternType.class), 
				hg.eq("head", "utterance_asis")));		
		HashSet<PatternOccurrence> S = new HashSet<PatternOccurrence>();
		HGHandle patternHandle = hg.assertAtom(graph, new Utterance(input), utteranceType);
		S.add(new PatternOccurrence(patternHandle, 1.0, 0, input.length()));
		return S;
	}
}
