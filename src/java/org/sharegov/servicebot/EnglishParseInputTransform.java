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

import java.util.ArrayList;
import java.util.List;

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HyperGraph;
import org.sharegov.servicebot.pattern.EnglishParser;

import disko.data.relex.RelOccurrence;
import disko.relex.PrologExporter;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class EnglishParseInputTransform implements InputTransform, HGGraphHolder
{
	private HyperGraph graph;
	
	public void setHyperGraph(HyperGraph graph)
	{
		this.graph = graph;
	}
	
	public Struct transform(InteractionFrame frame, Struct x)
	{
		if (!"utterance".equals(x.getName()))
			return x;
		List<Struct> facts = new ArrayList<Struct>();		
		String text = ((Struct)x.getArg(0)).getName();
		EnglishParser parser = new EnglishParser();
		parser.parse(graph, text);
		if (!parser.getParses().isEmpty())
		{
			PrologExporter exporter = new PrologExporter();
			for (RelOccurrence occ : parser.getParses().get(0).getRelOccs())
				facts.add(exporter.toPlainStruct(graph, occ));				
		}			
		facts.add(new Struct("utterance", new Struct(text)));
		System.out.println("English transform: " + facts);
		return new Struct(",", facts.toArray(new Term[0]));		
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName();
	}
}
