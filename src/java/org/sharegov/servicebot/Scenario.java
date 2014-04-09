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

import java.util.Date;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleHolder;

/**
 * 
 * <p>
 * Represents a dialog scenario used to teach Bot. A dialog scenario captures
 * several possible conversational threads captured from a single starting
 * point.
 * </p>
 *
 * <p>
 * A scenario is made of a sequence of dialog exchanges that are based on a common
 * context. The context is enriched as the scenario progresses into more specific and more
 * information is acquired by the user. 
 * </p>
 * 
 * <p>
 * A scenario is triggered by some user input. When there are several possibilities
 * for a given input, Bot must further narrow down the subject with some more
 * questions. 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class Scenario implements HGHandleHolder
{
	private transient HGHandle handle;
	private String title;
	private String description;
	private String createdBy;
	private Date createdOn;
	private String lastModifiedBy;
	private Date lastModifiedOn;
	
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}

	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	public Date getCreatedOn()
	{
		return createdOn;
	}

	public void setCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	public String getLastModifiedBy()
	{
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy)
	{
		this.lastModifiedBy = lastModifiedBy;
	}

	public Date getLastModifiedOn()
	{
		return lastModifiedOn;
	}

	public void setLastModifiedOn(Date lastModifiedOn)
	{
		this.lastModifiedOn = lastModifiedOn;
	}

	public HGHandle getAtomHandle()
	{
		return handle;
	}

	public void setAtomHandle(HGHandle handle)
	{
		this.handle = handle;
	}	
}
