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

import java.util.LinkedHashMap;

import org.sharegov.servicebot.BotRule;
import org.sharegov.servicebot.PrologUtils;

import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;

/**
 * 
 * <p>
 * Encapsulates runtime evaluatio of a BotRule, holding variable values, pattern
 * scores etc.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class AppliedRule
{	
	Prolog prolog;
	BotRule botrule;
	
	
	// Evaluation state
	Struct condition;
	LinkedHashMap<String,Term> vars = new LinkedHashMap<String, Term>();
				
	public AppliedRule(Prolog prolog, BotRule botrule)
	{
		this.prolog = prolog;
		this.botrule = botrule;
	}
	
	public double evalCondition()
	{
		double score = 0.0;
		
		// This will create a local deep copy of the full Struct and also make
		// sure all variables with the same name are in fact the same 'Var' object,
		// so that unification works.
		condition = (Struct)botrule.getCondition();
		score = PrologUtils.score(prolog, this, condition);
		//SolveInfo info = prolog.solve(condition);
		//if (info.isSuccess())
		//	score = 1.0;
		return score;
	}
	
	public void applyActions(double score)
	{
		PrologUtils.applyAction(prolog, this, (Struct)botrule.getAction(), score);		
	}
	
	public <T extends Term> T unifyVariables(T t)
	{
		try {
		if (t instanceof Var)
		{
			Var v = (Var)t;
			if (v.isAnonymous())
				return (T)v;
			Term value = vars.get(v.getName());
			if (value != null)
			{
				Var newv = new Var(v.getName());
				newv.unify(prolog, value);
				return (T)newv;
			}
			else
				return t;
		}
		else if (t instanceof Struct)
		{
			Struct s = (Struct)t;
			Term []args = new Term[s.getArity()];
			for (int i = 0; i < s.getArity(); i++)
				args[i] = unifyVariables(s.getArg(i));
			return (T)new Struct(s.getName(), args);
		}
		else
			return t;
		} catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public LinkedHashMap<String, Term> getVariables()
	{
		return vars;
	}
}
