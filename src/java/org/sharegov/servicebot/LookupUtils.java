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

import java.util.regex.Pattern;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;

public class LookupUtils
{
    public static final Pattern HANDLE_REGEX = Pattern.compile("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}", Pattern.CASE_INSENSITIVE);
	
	public static InteractionContext findContext(String name)
	{
		return hg.getOne(BotApp.get().getGraph(),
				hg.and(hg.type(InteractionContext.class), hg.eq("name",name)));
	}
	
	public static InteractionFrame findFrame(String nameOrId)
	{
		HyperGraph graph = BotApp.get().getGraph();
		if (HANDLE_REGEX.matcher(nameOrId).matches())
			return (InteractionFrame)graph.get(graph.getHandleFactory().makeHandle(nameOrId));
		else
			return (InteractionFrame)hg.getOne(graph, hg.and(hg.type(InteractionFrame.class),
					hg.eq("name", nameOrId)));
	}
	
	public static InteractionFrame findTopLevelFrame(InteractionContext context)
	{
		HyperGraph graph = BotApp.get().getGraph();
		return hg.getOne(graph, hg.and(hg.type(InteractionFrame.class), 
									hg.eq("name", "Welcome"),
									hg.memberOf(graph.getHandle(context))));
		// Set current frame to top level greeting frame
//		List<Activates> L = hg.getAll(graph,
//				hg.and(hg.type(Activates.class), 
//					   hg.orderedLink(graph.getHandleFactory().nullHandle(),
//							   		  graph.getHandleFactory().anyHandle())));
//		HGHandle iftype = graph.getTypeSystem().getTypeHandle(InteractionFrame.class);
//		for (Activates a : L)
//		{
//			if (iftype.equals(graph.getType(a.getTarget())))
//			{
//				return a.getTarget();
//			}
//		}
//		return null;
	}
}
