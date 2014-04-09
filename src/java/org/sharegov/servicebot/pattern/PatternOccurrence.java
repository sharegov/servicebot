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

public class PatternOccurrence
{
	private HGHandle pattern;
	private double score;
	private int begin, end;
	
	public PatternOccurrence() { }
	public PatternOccurrence(HGHandle pattern, double score, int begin, int end) 
	{ 
		this.pattern = pattern;
		this.score = score;
		this.begin = begin;
		this.end = end;
	}
	
	public HGHandle getPattern()
	{
		return pattern;
	}
	public void setPattern(HGHandle pattern)
	{
		this.pattern = pattern;
	}
	public double getScore()
	{
		return score;
	}
	public void setScore(double score)
	{
		this.score = score;
	}
	public int getBegin()
	{
		return begin;
	}
	public void setBegin(int begin)
	{
		this.begin = begin;
	}
	public int getEnd()
	{
		return end;
	}
	public void setEnd(int end)
	{
		this.end = end;
	}	
}
