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
package org.sharegov.servicebot.rest;

import java.util.HashSet;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.sharegov.servicebot.rest.ChatResource;

public class ServiceBotApplication extends Application
{
	public Set<Class<?>> getClasses()
	{		
		HashSet<Class<?>> S = new HashSet<Class<?>>();
		S.add(JsonEntityProvider.class);
		S.add(LexicalService.class);
		S.add(ProofReaderManager.class);
		S.add(WordBanService.class);
		S.add(TextPatternManagerService.class);
		S.add(ChatResource.class);
		S.add(InteractionFrameService.class);
		S.add(UserService.class);
		return S;
	}
}
