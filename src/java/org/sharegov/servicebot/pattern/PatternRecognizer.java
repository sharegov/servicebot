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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleHolder;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.annotation.HGIgnore;

/**
 * 
 * <p>
 * A <code>PatternRecognizer</code> detects and scores patterns from raw input.
 * This could be simply a representation of the input in "pattern form" (with
 * 1.0 scores) or it could some more complicated analysis and inference process
 * such as NLP parsing.
 * </p>
 * 
 * <p>
 * Recognizers that are <em>input only based</em> are going to be executed only
 * once on the current input. Other will be executed iteratively as long as they
 * produce new patterns based on previous iterations.
 * </p>
 * 
 * @author Borislav Iordanov
 * 
 */
public abstract class PatternRecognizer implements HGHandleHolder, HGGraphHolder
{
	protected HyperGraph graph;
	protected HGHandle handle;
	protected boolean enabled = true;
	protected boolean inputOnlyBased = true;

	public abstract Set<PatternOccurrence> detectPatterns(
			Map<HGHandle, Double> context, String input);

	public static Map<HGHandle, Double> asScoreMap(Set<PatternOccurrence> S)
	{
		Map<HGHandle, Double> patterns = new HashMap<HGHandle, Double>();
		for (PatternOccurrence pocc : S)
			patterns.put(pocc.getPattern(), pocc.getScore());
		return patterns;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public boolean isInputOnlyBased()
	{
		return inputOnlyBased;
	}

	public void setInputOnlyBased(boolean inputOnlyBased)
	{
		this.inputOnlyBased = inputOnlyBased;
	}

	public void setHyperGraph(HyperGraph graph)
	{
		this.graph = graph;
	}

	@HGIgnore
	public HGHandle getAtomHandle()
	{
		return handle;
	}

	public void setAtomHandle(HGHandle handle)
	{
		this.handle = handle;
	}	
}
