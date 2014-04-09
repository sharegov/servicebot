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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleHolder;
import org.hypergraphdb.HGTypeHolder;
import org.hypergraphdb.annotation.HGIgnore;
import org.sharegov.servicebot.pattern.PatternType;

/**
 * 
 * <p>
 * Represents an <em>utterance pattern</em>. Such patterns are constructed
 * from sample utterances following various heuristic rules. The simplest
 * <code>UPattern</code> is an utterance itself, verbatim.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public abstract class UPattern implements HGHandleHolder, HGTypeHolder<PatternType>
{
	private HGHandle atomHandle;
	private PatternType atomType;
	
	public abstract String prettyPrint(String indentLevel);
		
	@HGIgnore
	public PatternType getAtomType()
	{
		return atomType;
	}

	public void setAtomType(PatternType atomType)
	{
		this.atomType = atomType;
	}

	@HGIgnore
	public HGHandle getAtomHandle()
	{
		return atomHandle;
	}

	public void setAtomHandle(HGHandle atomHandle)
	{
		this.atomHandle = atomHandle;
	}
}
