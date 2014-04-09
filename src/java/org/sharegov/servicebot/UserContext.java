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

/**
 * 
 * <p>
 * Represents information about the user currently interacting with the system.
 * Potential users don't necessarily have to be logged in, but there's conceivably
 * all kinds of contextual information that might help.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class UserContext
{
	private String firstName, lastName, username, email;
	private String frameContext;
	private String currentFrame;
		
	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getCurrentFrame()
	{
		return currentFrame;
	}

	public void setCurrentFrame(String currentFrame)
	{
		this.currentFrame = currentFrame;
	}
	
	public String getFrameContext()
	{
		return frameContext;
	}

	public void setFrameContext(String frameContext)
	{
		this.frameContext = frameContext;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}
}
