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
package org.sharegov.servicebot.pattern.parser;

import java.util.ArrayList;


import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.pattern.PatternType;
import org.sharegov.servicebot.pattern.TextPattern;

public class PatternParser
{
	private int pos = 0;
	private HyperGraph graph;
	private String patternExpression;
	
	private boolean match(String s, boolean enforce)
	{
		int p = pos;
		for (int i = 0; i < s.length(); i++)
			if (patternExpression.charAt(p) != s.charAt(i))
				if (enforce)
					throw new ParseError("Expecting " + s, pos);
				else
					return false;
			else
				p++;
		pos = p;
		return true;
	}
	
	private void ws() 
	{ 
		while (Character.isWhitespace(patternExpression.charAt(pos))) pos++ ;
	}

	private String identifier() 
	{
		int end = pos;
		while (Character.isJavaIdentifierPart(patternExpression.charAt(end))) end++ ;
		if (end > pos)
		{
			String result = patternExpression.substring(pos, end);
			pos = end;
			return result;
		}
		return null;
	}
	
	private TextPattern parseText(PatternType patternType)
	{
		match("[", true);
		StringBuilder sb = new StringBuilder();
		while (patternExpression.charAt(pos) != ']')
			sb.append(patternExpression.charAt(pos++));
		match("]", true);
		return patternType.createNew(sb.toString());
	}
	
	public PatternParser(String patternExpression, HyperGraph graph)
	{
		this.patternExpression = patternExpression;
		this.graph = graph;
	}
	
	public HGHandle parse()
	{
		ws();
		String head = identifier();
		if (head == null)
			throw new ParseError("Could identify pattern, or pattern is missing altogether.", pos);
		ws();
		PatternType patternType = hg.getOne(graph, 
											hg.and(hg.type(PatternType.class), 
												   hg.eq("head", head)));
		if (patternType == null)
			throw new ParseError("Unknown pattern type '" + head + "'", pos);
		
		if (TextPattern.class.isAssignableFrom(patternType.getPatternClass()))
			return hg.assertAtom(graph, parseText(patternType), patternType.getAtomHandle());
		ArrayList<HGHandle> subs = new ArrayList<HGHandle>();
		match("[", true);
		ws();
		while(true)
		{
			if (patternExpression.charAt(pos) == ']')
				break;
			subs.add(parse());
			ws();
			if (patternExpression.charAt(pos) != ',')
				break;
			pos++;
		}
		match("]", true);
		return hg.assertAtom(graph, 
							 patternType.createNew(subs), 
							 patternType.getAtomHandle(), 
							 !patternType.getDimensionNames().hasNext());
	}
}
