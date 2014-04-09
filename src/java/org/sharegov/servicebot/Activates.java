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
import org.hypergraphdb.HGPlainLink;

/**
 * 
 * <p>
 * Represents between a trigger of something and that something. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class Activates extends HGPlainLink
{
	public Activates(HGHandle...args)
	{
		super(args);
	}
	
	public void changeTrigger(HGHandle h)
	{
		super.outgoingSet[0] = h;
	}
	
	public HGHandle getTrigger()
	{
		return getTargetAt(0);
	}

	public void changeTarget(HGHandle h)
	{
		super.outgoingSet[1] = h;
	}
	
	public HGHandle getTarget()
	{
		return getTargetAt(1);
	}
}
