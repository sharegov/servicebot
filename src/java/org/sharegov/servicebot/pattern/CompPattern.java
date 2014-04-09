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
import java.util.Iterator;
import java.util.List;

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.UPattern;

public abstract class CompPattern extends UPattern implements HGLink,
		HGGraphHolder, Iterable<HGHandle>
{
	protected HyperGraph graph;
	protected List<HGHandle> alternatives = new ArrayList<HGHandle>();

	public CompPattern(HGHandle... args)
	{
		for (HGHandle h : args)
			alternatives.add(h);
	}

	public Iterator<HGHandle> iterator()
	{
		return alternatives.iterator();
	}

	public void addAlternative(HGHandle pattern)
	{
		if (!alternatives.contains(pattern))
			alternatives.add(pattern);
		if (getAtomHandle() != null)
			graph.update(this);
	}

	public void addAlternative(UPattern pattern)
	{
		addAlternative(hg.assertAtom(graph, pattern));
	}

	public int getArity()
	{
		return alternatives.size();
	}

	public HGHandle getTargetAt(int i)
	{
		return alternatives.get(i);
	}

	public void notifyTargetHandleUpdate(int i, HGHandle handle)
	{
		alternatives.set(i, handle);
	}

	public void notifyTargetRemoved(int i)
	{
		alternatives.remove(i);
	}

	public void setHyperGraph(HyperGraph graph)
	{
		this.graph = graph;
	}

	public String prettyPrint(String head, String indentLevel)
	{
		StringBuilder sb = new StringBuilder(indentLevel);
		sb.append(head);
		sb.append(" [\n ");
		for (int i = 0; i < getArity(); i++)
		{
			sb.append(indentLevel);
			UPattern sub = graph.get(getTargetAt(i));
			sb.append(sub.prettyPrint(indentLevel + "    "));
			if (i < getArity() - 1)
				sb.append(",\n");
		}
		sb.append(indentLevel);
		sb.append("]");
		return sb.toString();
	}
	
	public String prettyPrint(String indentLevel)
	{
		return prettyPrint(getAtomType().getHead(), indentLevel);
	}	
}
