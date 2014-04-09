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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.WordOccurrence;

import disko.data.relex.RelOccurrence;
import disko.data.relex.SentenceInterpretation;

/**
 * 
 * <p>
 * Parses the input into syntactic relationships.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class SyntaxPatternRecognizer extends PatternRecognizer
{
	private EnglishParser parser = new EnglishParser();
	private boolean bundleAsSinglePattern = true;

	private HGHandle relOccToPattern(RelOccurrence occ)
	{
		String patternHead = graph.get(occ.getTargetAt(0)).toString();
		PatternType patternType = hg.getOne(graph, hg.and(hg.type(PatternType.class), 
												 hg.eq("head", patternHead)));
		HGHandle wordType = hg.findOne(graph, hg.and(hg.type(PatternType.class), 
											hg.eq("head", "word_asis")));
		
		if (patternType == null)
			patternType = graph.get(hg.assertAtom(graph, 
										new PatternType(patternHead, SyntacticPattern.class, null)));
		ArrayList<HGHandle> args = new ArrayList<HGHandle>();
		for (int i = 1; i < occ.getArity(); i++)
		{
			WordOccurrence wordOcc = new WordOccurrence(graph.get(occ.getTargetAt(i)).toString());
			args.add(hg.assertAtom(graph, wordOcc, wordType));
		}
		return hg.assertAtom(graph, patternType.createNew(args), graph.getHandle(patternType));
	}
	
	public EnglishParser getParser()
	{
		return parser;		
	}
	
	public Set<PatternOccurrence> detectPatterns(Map<HGHandle, Double> context,
												 String input)
	{
		List<SentenceInterpretation> parseList = parser.parse(graph, input);
		HashSet<PatternOccurrence> S = new HashSet<PatternOccurrence>();		
		SyntaxStructure pattern = new SyntaxStructure();
		try
		{
			for (SentenceInterpretation si : parseList)
			{
				for (RelOccurrence rocc : si.getRelOccs())
				{					
//					System.out.println(rocc.getComponents(graph));
					HGHandle patternHandle = relOccToPattern(rocc);
					pattern.addAlternative(patternHandle);
					if (!this.bundleAsSinglePattern)
					{
						int min = Integer.MAX_VALUE, max = 0;
						for (int p : rocc.getPositions())
						{	min = Math.min(min, p); max = Math.max(max, p); }
						S.add(new PatternOccurrence(patternHandle, 1.0, min, max));
					}
				}
			}
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		if (this.bundleAsSinglePattern)
		{
			HGHandle patternHandle = hg.assertAtom(graph, pattern, BotApp.get().findPatternType("syntax_structure"));
			S.add(new PatternOccurrence(patternHandle, 1.0, 0, input.length()));
		}
		return S;
	}
	
	public boolean isBundleAsSinglePattern()
	{
		return bundleAsSinglePattern;
	}

	public void setBundleAsSinglePattern(boolean bundleAsSinglePattern)
	{
		this.bundleAsSinglePattern = bundleAsSinglePattern;
	}	
}
