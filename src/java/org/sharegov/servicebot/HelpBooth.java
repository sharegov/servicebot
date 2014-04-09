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

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.sharegov.servicebot.xmpp.SmartBotXMPP;


/**
 * 
 * <p>
 * Represents a chat room with one or more participants. At minimum, we have one 
 * end user (citizen/customer) asking questions and at least one human agent or a 
 * Bot clone interacting with them.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class HelpBooth
{
	private String id;
	private SmartBotXMPP bot;
	private MultiUserChat chatRoom;
	
	public HelpBooth(String boothId)
	{
		this.id = boothId;
	}

	public void joinHuman()
	{
		
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public MultiUserChat getChatRoom()
	{
		return chatRoom;
	}

	public void setChatRoom(MultiUserChat chatRoom)
	{
		this.chatRoom = chatRoom;
	}

	public SmartBotXMPP getBot()
	{
		return bot;
	}

	public void setBot(SmartBotXMPP bot)
	{
		this.bot = bot;
	}		
}
