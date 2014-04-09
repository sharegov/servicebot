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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.sharegov.servicebot.UPattern;

public class MinFunction implements MatchFunction
{
	public double match(HyperGraph graph, UPattern pattern, PatternMatcher matcher)
	{
		CompPattern p = (CompPattern)pattern;
		double result = 0.0;
		for (HGHandle x : p)
			result = Math.max(result, matcher.score(x));
		return result;		
	}
}