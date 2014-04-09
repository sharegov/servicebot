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
package org.sharegov.servicebot.proofread;

public class RedInk
{
	private int from, to;
	private String explanation;
	private String text;

	public RedInk()
	{		
	}
	
	public RedInk(int from, int to, String text, String explanation)
	{
		this.from = from;
		this.to = to;
		this.text = text;
		this.explanation = explanation;
	}

	public int getFrom()
	{
		return from;
	}

	public void setFrom(int from)
	{
		this.from = from;
	}

	public int getTo()
	{
		return to;
	}

	public void setTo(int to)
	{
		this.to = to;
	}

	public String getExplanation()
	{
		return explanation;
	}

	public void setExplanation(String explanation)
	{
		this.explanation = explanation;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((explanation == null) ? 0 : explanation.hashCode());
		result = prime * result + from;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + to;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RedInk other = (RedInk) obj;
		if (explanation == null)
		{
			if (other.explanation != null)
				return false;
		}
		else if (!explanation.equals(other.explanation))
			return false;
		if (from != other.from)
			return false;
		if (text == null)
		{
			if (other.text != null)
				return false;
		}
		else if (!text.equals(other.text))
			return false;
		if (to != other.to)
			return false;
		return true;
	}	
}
