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
package org.sharegov.servicebot.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.sharegov.servicebot.Bot;
import org.sharegov.servicebot.BotResponse;
import org.sharegov.servicebot.BotUtterance;

public class BotXMPP implements PacketListener
{
	private Bot becky;
	private XMPPConnection conn = null;
	private MultiUserChat chatRoom = null;
	
	public BotXMPP(Bot becky, ConnectionConfiguration config, String roomId)
	{
		this.becky = becky;
		try
		{
			conn = new XMPPConnection(config);
			conn.connect();
			conn.loginAnonymously();
			chatRoom = new MultiUserChat(conn, roomId);
			chatRoom.join("Bot");			
			chatRoom.addMessageListener(this);
			Message msg = chatRoom.createMessage();
			msg.setBody("Hi,\nThis is Bot, your virtual (but smart!) assistant.\nGo ahead...");
			chatRoom.sendMessage(msg);			
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public void processPacket(Packet packet)
	{
		try
		{
	        Message msg = (Message) packet;        
	        if (msg.getFrom().contains("Bot") || chatRoom.getRoom().equals(packet.getFrom()))
	        	return;
			BotResponse response = becky.hear(msg.getBody());
			msg = chatRoom.createMessage();
			msg.setFrom("Bot");
			if (response instanceof BotUtterance)
				msg.setBody(((BotUtterance)response).getText());
			//msg.setBody("Stock Bot Response");
			chatRoom.sendMessage(msg);
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}	
}
