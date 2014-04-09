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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.annotation.HGIgnore;

import alice.tuprolog.Struct;
import alice.tuprolog.hgdb.PrologNode;

public class BotRule extends HGPlainLink
{
	private PrologNode pnode = BotApp.get().getPrologNode();
	
	public BotRule()
	{
		super(new HGHandle[2]);
	}
	
	public BotRule(HGHandle...args)
	{
		super(args);
	}
		
	@HGIgnore
	public void setCondition(Struct condition)
	{
		this.outgoingSet[0] = pnode.add(condition);
	}

	@HGIgnore
	public void setAction(Struct action)
	{
		this.outgoingSet[1] = pnode.add(action);
	}
	
	public Struct getCondition()
	{
		return pnode.get(getTargetAt(0));
	}
	
	public Struct getAction()
	{
		return pnode.get(getTargetAt(1));
	}	
}
