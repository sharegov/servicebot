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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;

/**
 * 
 * <p>
 * Utility methods to manage the graph of scenarios.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class ScenarioManager
{
	public Map<HGHandle, Double> triggerScenarios(Utterance utterance)
	{
		HyperGraph graph = BotApp.get().getGraph();
		Map<HGHandle, Double> scenarios = new HashMap<HGHandle, Double>();		
//		Map<HGHandle, Double> patterns = UtteranceParser.get().analyze(utterance.getAtomHandle());
//		for (Map.Entry<HGHandle, Double> e : patterns.entrySet())
//		{
//			List<HGHandle> L = hg.findAll(graph, 
//					hg.apply(hg.targetAt(graph, 1), 
//						   hg.and(hg.type(Activates.class), hg.incident(e.getKey())))); 
//			
//			for (HGHandle scenarioHandle : L)
//			{
//				Double current = scenarios.get(scenarioHandle);
//				if (current == null)
//					scenarios.put(scenarioHandle, e.getValue());
//				else
//					scenarios.put(scenarioHandle, current + e.getValue());
//			}
//		}
		return scenarios;
	}
}
