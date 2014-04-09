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

import java.util.List;

import org.sharegov.servicebot.PrologUtils;

import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;

public class FactScore implements StructScore
{
	private void saveVariables(AppliedRule rule, SolveInfo info)
	{ 
		try {
		for (Var v : (List<Var>)(List<?>)info.getBindingVars())
		{
			rule.getVariables().put(v.getName(), v.getTerm());
		}
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public double score(Prolog prolog, AppliedRule rule, Struct s)
	{
		try
		{
			s = rule.unifyVariables(s);
			Struct s1 = PrologUtils.appendTerm(s, new Var("Score"));
			SolveInfo info = prolog.solve(s1); //.copyGoal(rule.getVariables(), -1));
			if (info.isSuccess())
			{
				saveVariables(rule, info);
				Term value = info.getVarValue("Score");
				if (value instanceof Var)
					System.err.println("No score for fact " + s1);
				else
					return ((alice.tuprolog.Number)value).doubleValue();
			}
			info = prolog.solve(s);//s.copyGoal(rule.getVariables(),-1));
			if (info.isSuccess())
			{
				saveVariables(rule, info);
				return 1.0;
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return 0;
	}
}
