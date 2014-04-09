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
import java.util.List;

/**
 * <p>
 * Represents a conversation at a particular point in time. All actions are logged. 
 * An instance serves as a runtime data-structure that is also persisted for 
 * future reference.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class Conversation
{
	private List<DialogAction> dialogActions = new ArrayList<DialogAction>();

	public List<DialogAction> getDialogActions()
	{
		return dialogActions;
	}

	public void setDialogActions(List<DialogAction> dialogActions)
	{
		this.dialogActions = dialogActions;
	}	
}
