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
package org.sharegov.servicebot.pattern.prolog;

import org.sharegov.servicebot.PrologUtils;

import alice.tuprolog.Prolog;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class MinTermScore implements StructScore
{

	@Override
	public double score(Prolog prolog, AppliedRule rule, Struct s)
	{
		double min = 1.0; // not sure what this should be :(
		for (int i = 0; i < s.getArity(); i++)
		{
			Term t = s.getArg(i);
			if (t instanceof Struct)
			{
				double score = PrologUtils.score(prolog, rule, (Struct)t);
				if (score < min)
					min = score;
			}
		}
		return min;
	}

}
