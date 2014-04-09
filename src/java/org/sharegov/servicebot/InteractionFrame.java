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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.annotation.HGIgnore;

import alice.tuprolog.Struct;
import alice.tuprolog.hgdb.PrologNode;

/**
 * 
 * <p>
 * Represents the coupling of a single bot-user interaction in terms of the bot's
 * output, the user's input, the transformation of the user's input for rule processing
 * and the set of rules to be applied to that input.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class InteractionFrame extends HGPlainLink
{
	private PrologNode pnode = BotApp.get().getPrologNode();
	private String name;
	
	public InteractionFrame()
	{
		this.outgoingSet = new HGHandle[2];
		this.outgoingSet[0] = this.outgoingSet[1] = BotApp.get().getGraph().getHandleFactory().nullHandle();
	}
	
	public InteractionFrame(HGHandle...args)
	{
		super(args);
	}

	public Struct getBotOutput()
	{
		return pnode.get(getTargetAt(0));
	}

	@HGIgnore
	public void setBotOutput(HGHandle h)
	{
		super.outgoingSet[0] = h;
	}
	
	public InputTransform getInputTransform()
	{
		return pnode.get(getTargetAt(1));
	}
	
	public void setInputTransform(HGHandle h)
	{
		super.outgoingSet[1] = h;		
	}
	
	public Set<BotRule> getRules()
	{
		HyperGraph graph = BotApp.get().getGraph();		
		Set<BotRule> rules = new HashSet<BotRule>();
		List<HGHandle> L = hg.findAll(graph, 
				hg.apply(hg.targetAt(graph, 1),
						hg.and(hg.type(Activates.class), 
							   hg.orderedLink(graph.getHandle(this), 
									   graph.getHandleFactory().anyHandle()))));
		for (HGHandle ruleHandle : L)
		{
			try
			{
				rules.add((BotRule)graph.get(ruleHandle));
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
		}
		//rules.addAll(L);
		return rules;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}	
}
