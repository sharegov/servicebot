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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sharegov.servicebot.pattern.prolog.AppliedRule;
import org.sharegov.servicebot.pattern.prolog.ArithmeticMeanScore;
import org.sharegov.servicebot.pattern.prolog.FactScore;
import org.sharegov.servicebot.pattern.prolog.MaxTermScore;
import org.sharegov.servicebot.pattern.prolog.MinTermScore;
import org.sharegov.servicebot.pattern.prolog.StructScore;

import mjson.Json;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import alice.tuprolog.clausestore.HGAtomTerm;

public class PrologUtils
{
	static HashMap<String, StructScore> scoreFunctions = new HashMap<String, StructScore>();	
	static 
	{
		scoreFunctions.put(",", new MinTermScore());
		scoreFunctions.put(";", new MaxTermScore());
		scoreFunctions.put("syntax_structure", new ArithmeticMeanScore());
	}
	
	public static StructScore getScoreFunction(String name)
	{
		return scoreFunctions.get(name);
	}
	
	public static double score(Prolog prolog, AppliedRule rule, Struct s)
	{
		// TODO: Handle variables, they are global to the struct.. 
		StructScore f = getScoreFunction(s.getName());
		if (f == null)
			f = new FactScore();
		return f.score(prolog, rule, s);
	}

	public static void applyAction(Prolog prolog, AppliedRule rule, Struct s, double score)
	{
		if (s.getName().equals(","))
		{
			for (int i = 0; i < s.getArity(); i++)
				applyAction(prolog, rule, (Struct)s.getArg(i), score);
			return;
		}
		if (rule != null)
			s = rule.unifyVariables(s);				
		if (s.getName().equals("assert"))
		{
			for (int i = 0; i < s.getArity(); i++)
				prolog.getTheoryManager().assertA((Struct)s.getArg(i), true, null, false);
				//assertStruct(prolog, (Struct)s.getArg(i), score);
			return;			
		}
		else if (s.getName().equals("retract"))
		{
			for (int i = 0; i < s.getArity(); i++)
				prolog.getTheoryManager().retract((Struct)s.getArg(i));
			return;						
		}
		else if (s.getName().equals("retractall"))
		{
			for (int i = 0; i < s.getArity(); i++)
				try
				{
					prolog.solve("retractall(" + ((Struct)s.getArg(i)).toString() + ").");
				}
				catch (MalformedGoalException e)
				{
					throw new RuntimeException(e);
				}
			return;						
		}
		// maybe we can have a predefined set of predicates that are interpreted as assertions automatically
		// 'next' for the next frame is clearly such a candidate 
		else if (s.getName().equals("next"))
		{
			assertStruct(prolog, rule, s, score);
		}
		else
		{
			SolveInfo info = prolog.solve(s);
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
	}
	
	public static void autoClear(Prolog prolog)
	{
		try
		{
			ArrayList<Struct> toretract = new ArrayList<Struct>();
			SolveInfo info = prolog.solve("autoClear(X).");
			while (info.isSuccess())
			{
				toretract.add((Struct)info.getVarValue("X"));
				if (!info.hasOpenAlternatives())
					break;
				info = prolog.solveNext();
			}
			for (Struct clause : toretract)
			{
				prolog.solve("retractall(" + clause.toString()+ ").");
				prolog.solve("retractall(" + appendTerm(clause, new alice.tuprolog.Var("_")).toString() + ").");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
	}
	
	public static void assertStruct(Prolog prolog, AppliedRule rule, Struct s, double score)
	{
		if (!s.getName().equals(","))
		{
			if (rule != null)
				s = rule.unifyVariables(s);
			prolog.getTheoryManager().assertA(PrologUtils.assignScore(s, score), true, null, false);
			if (score == 1.0)
				prolog.getTheoryManager().assertA(s, true, null, false);
		}
		else for (int i = 0; i < s.getArity(); i++)
			assertStruct(prolog, rule, (Struct)s.getArg(i), score);				
	}
	
	public static Struct appendTerm(Struct s, Term t)
	{
		Term []A = new Term[s.getArity() + 1];
		for (int i = 0; i < s.getArity(); i++)
			A[i] = s.getArg(i);
		A[A.length - 1] = t;
		return new Struct(s.getName(), A);
	}
	
	public static Struct assignScore(Struct s, double score)
	{
		return appendTerm(s, new alice.tuprolog.Double(score));
	}
	
	/**
	 * <p>Parses a JSON represention into a Prolog term. The representatio is expected to follow the 
	 * format described in the {@link #toJson(Term)} method.</p>
	 * @param j The Json representation of a Prolog term.
	 * @return The Prolog term obtained from the JSON.
	 */
	public static Term fromJson(Json j)
	{
		if (j.isNumber())
			return new alice.tuprolog.Double(j.asDouble());
		else if (j.isString())
		{
			return new Struct(j.asString());
		}
		else if (j.isObject())
		{
			if (j.has("handle"))
				return new HGAtomTerm(BotApp.get().getGraph().getHandleFactory().makeHandle(j.at("handle").asString()),
						BotApp.get().getGraph());
			else if (j.has("varname"))
				try { return new Var(j.at("varname").asString()); }
				catch (Exception ex) { throw new RuntimeException(ex); }
			else
			{
				List<Json> jargs = j.at("args").asJsonList();
				Term [] args = new Term[jargs.size()];
				for (int i = 0; i < jargs.size(); i++)
					args[i] = fromJson(jargs.get(i));
				return new Struct(j.at("name").asString(), args);
			}
		}
		else if (j.isArray())
		{
			// This is a list
			if (j.asJsonList().isEmpty())
				return Struct.EMPTY_LIST;
			else
			{
				Struct result = Struct.EMPTY_LIST;
				for (int i = j.asJsonList().size() - 1; i >= 0; i--)
					result = new Struct(".", fromJson(j.asJsonList().get(i)), result);
				return result;
			}
				
		}
		else
			throw new IllegalArgumentException();
	}
	
	/**
	 * <p>Convert a Prolog term into Json format. The conversion yields the following JSON
	 * representation:</p>
	 * <ul>
	 * <li>Numbers are translated into JSON numbers.</li>
	 * <li>Variables are translated into JSON objects of the form <code>{"varname":the_name_of_Prolog_var}</code></li>
	 * <li>HGDB Atoms are translated into JSON objects of the form <code>{"handle":the handle of the atom}</code></li>
	 * <li>0 arity Structs are translated into JSON strings.</li>
	 * <li>> 0 arity Structs are translated into JSON objects of the form
	 *  {"name": the name of the Struct, "args":a JSON array of the Struct arguments.}</li>
	 * <li>Structs representing lists, i.e. empty list "[]" and the "." special Struct are translated
	 * into JSON arrays</li> 
	 * </ul>
	 * 
	 * @param t The Prolog <code>Term</code>.
	 * @return The JSON representatio of the <code>t</code> parameter.
	 */
	public static Json toJson(Term t)
	{
		if (t == null)
			return Json.nil();
		else if (t instanceof alice.tuprolog.Number)
			return Json.make(((alice.tuprolog.Number)t).doubleValue());
		else if (t instanceof Var)
			return Json.object("varname", ((Var)t).getName());
		else if (t instanceof HGAtomTerm)
			return Json.object().set("handle", 
					((HGAtomTerm)t).getHandle().getPersistent());
		else if (t instanceof Struct)
		{
			Struct s = (Struct)t;
			if (s.isEmptyList())
				return Json.array();
			else if (s.getName().equals(".")) // a list
			{
				Json A = Json.array();
				while (!s.isEmptyList()) {
					A.add(toJson(s.listHead()));
					s = (Struct) s.listTail();
				}				
				return A;
			}
			else if (s.getArity() == 0)
				return Json.make(s.getName());
			else 
			{
				Json args = Json.array();
				for (int i = 0; i < s.getArity(); i++)
					args.add(toJson(s.getArg(i)));
				return Json.object("name", s.getName(), "args", args);				
			}
		}
		else
			throw new IllegalArgumentException();
	}
}
