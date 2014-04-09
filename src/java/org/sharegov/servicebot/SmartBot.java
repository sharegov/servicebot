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

import java.io.FileInputStream;

import org.sharegov.servicebot.pattern.prolog.*;
import alice.tuprolog.*;

public class SmartBot
{		
	InteractionContext context = null;
	private InteractionFrame currentFrame;
	private Prolog prolog;
	private UserContext ucontext;
	
	private void init()
	{
		FileInputStream programFile = null;
		try
		{
			programFile = new FileInputStream(BotApp.config().at("workingDir").asString() + "/data/program.pl"); 
			Theory program = new Theory(programFile);
			prolog = new Prolog();
			prolog.setTheory(program);
			if (ucontext.getFrameContext() != null)
				context = LookupUtils.findContext(ucontext.getFrameContext());
			else
				context = LookupUtils.findContext(BotApp.config().at("defaultContext").asString());
			if (context == null)
				throw new NullPointerException("No context " + ucontext.getFrameContext() + " found.");
			currentFrame = ucontext.getCurrentFrame() == null ?
					LookupUtils.findTopLevelFrame(context) :
					LookupUtils.findFrame(ucontext.getCurrentFrame());
			if (currentFrame == null)
				currentFrame = new InteractionFrame();
		}
		catch (Exception ex)
		{
			if (programFile != null) try{ programFile.close(); } catch (Exception t) { }
			throw new RuntimeException(ex);
		}		
	}
	
	public SmartBot()
	{
		init();
	}
	
	public SmartBot(UserContext ucontext)
	{
		this.ucontext = ucontext;
		init();
	}
	
	public InteractionFrame getCurrentFrame()
	{
		return this.currentFrame;
	}
	
	public InteractionFrame findNextFrame(Prolog prolog) throws PrologException
	{
		double maxScore = 0.0;
		String frameId = null;
		SolveInfo info = prolog.solve(new Struct("next", new Var("Frame"), new Var("Score")));
		while (info.isSuccess())
		{
			double score = 0.0;
			Term scoreTerm = info.getVarValue("Score");
			if (scoreTerm instanceof alice.tuprolog.Number)
			{
				score = ((alice.tuprolog.Number)scoreTerm).doubleValue();
			}
			else
			{
				throw new RuntimeException("Invalid score for next frame in assertion " + info.getSolution());
			}
			if (score > maxScore)
			{
				maxScore = score;
				frameId = ((Struct)info.getVarValue("Frame")).getName();
			}
			if (!info.hasOpenAlternatives())
				break;
			info = prolog.solveNext();
		}
		prolog.solveHalt();
		return (frameId != null) ? LookupUtils.findFrame(frameId) :	null;
	}
			
	private Struct prepareOutput(Struct output, Prolog prolog) throws Exception
	{
		if (!output.getName().equals("utterance"))
			return output;
		String utt = ((Struct)output.getArg(0)).getName();
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\$\\{(\\w+)\\}");
		java.util.regex.Matcher m = p.matcher(utt);
		StringBuilder newUtt = new StringBuilder();
		int pos = 0;
		while (m.find())
		{
			newUtt.append(utt.substring(pos, m.start()));
			SolveInfo info = prolog.solve(new Struct(m.group(1), new Var("Value")));
			if (info.isSuccess())
			{			
				Term templateValue = info.getVarValue("Value");
				if (templateValue instanceof Struct)
					newUtt.append(((Struct)templateValue).getName());
				else
					newUtt.append(templateValue.toString());
			}
			else
				newUtt.append(m.group(1));
			pos = m.end();
		}
		newUtt.append(utt.substring(pos));
		return new Struct("utterance", new Struct(newUtt.toString()));
	}

	public synchronized Struct respondTo(Struct input) throws Exception
	{
		while (true)
		{
			PrologUtils.autoClear(prolog);
			if (findNextFrame(prolog) != null)
				throw new RuntimeException("next clause is reserved and should be used only to define the next frame.");
			PrologUtils.assertStruct(prolog, null, currentFrame.getInputTransform().transform(currentFrame, input), 1.0);
			//PrologUtils.assertStruct(prolog, currentFrame.getInputTransform().transform(currentFrame, input), 1.0);
			for (BotRule rule : currentFrame.getRules())
			{
				AppliedRule applied = new AppliedRule(prolog, rule);
				double score = applied.evalCondition();
				if (score > 0)
				{
					//System.out.println("match....");
					applied.applyActions(score);
				}
			}
			InteractionFrame nextFrame = findNextFrame(prolog);
			if (nextFrame != null)
				currentFrame = nextFrame;
			Struct output = currentFrame.getBotOutput();			
			output = prepareOutput(output, prolog);
			if (!output.getName().equals("continue"))
				return output;
		}
	}
	
	public synchronized Term queryOne(String predicate)
	{
		try
		{
			SolveInfo info = prolog.solve(new Struct(predicate, new Var("X")));
			if (info.isSuccess())
				return info.getVarValue("X");
			else
				return null;
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
