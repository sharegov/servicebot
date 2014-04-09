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

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;

import mjson.Json;

import org.apache.commons.lang.StringEscapeUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.sharegov.servicebot.PrologUtils;
import org.sharegov.servicebot.SmartBot;

import alice.tuprolog.Struct;

public class SmartBotXMPP implements PacketListener
{
	private SmartBot bot;
	private XMPPConnection conn = null;
	private MultiUserChat chatRoom = null;
	
	private Json trace = Json.array();
	
	public SmartBotXMPP(SmartBot becky, ConnectionConfiguration config, String roomId)
	{
		this.bot = becky;
		try
		{
			conn = new XMPPConnection(config);
			conn.connect();
			conn.loginAnonymously();
			chatRoom = new MultiUserChat(conn, roomId);
			chatRoom.join("Bot");			
			chatRoom.addMessageListener(this);
			Message msg = chatRoom.createMessage();
			Json greeting = PrologUtils.toJson(bot.getCurrentFrame().getBotOutput());
			msg.setBody(greeting.toString());
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
	        Json request = Json.read(StringEscapeUtils.unescapeXml(msg.getBody()));
			Json response = PrologUtils.toJson(bot.respondTo((Struct)PrologUtils.fromJson(request)));
			trace.add(request).add(response);
			msg = chatRoom.createMessage();
			msg.setFrom("Bot");
			msg.setBody(response.toString());
			chatRoom.sendMessage(msg);
		}
		catch (Throwable ex)
		{
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}	
	
	public void saveTrace(String filename)
	{
		FileWriter out = null;
		try
		{
			out = new FileWriter(new File(filename));
			out.write(trace.toString());
			out.close();			
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if (out != null) { try {out.close(); } catch (Throwable t) { }}
		}
	}
	
	public SmartBot getBot() { return bot; }
	public MultiUserChat getRoom() { return this.chatRoom; }
}
