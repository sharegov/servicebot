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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.sharegov.servicebot.xmpp.BotXMPP;
import org.sharegov.servicebot.xmpp.SmartBotXMPP;

/**
 * <p>
 * Manages currently active conversations, dispatches incoming requests for help and assigns
 * to Beck clones or human assistants. Manages conferences 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class FrontDesk
{
	private XMPPConnection xmppConnection = null;
	private long roomId = 0;
	private Map<String, HelpBooth> activeRooms = new ConcurrentHashMap<String, HelpBooth>();
	
	private ConnectionConfiguration getConnectionConfiguration()
	{
		BotApp app = BotApp.get();
	    ConnectionConfiguration config = new ConnectionConfiguration(
	    		app.getConfigProperty("xmpp.host", app.config().at("openfireHost").asString()), 
	    		Integer.parseInt(app.getConfigProperty("xmpp.port", "5222")));    
	    return config;
	}
	
	private void init()
	{
		try
		{
			xmppConnection = new XMPPConnection(getConnectionConfiguration());
			xmppConnection.connect();
			xmppConnection.login("admin", BotApp.config().at("openfireAdminPassword").asString());			
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			throw new RuntimeException(t);
		}
	}
	
	private synchronized HelpBooth createHelpRoom()
	{
		try
		{
			String newRoomId = "helproom" + (roomId++)  + "@conference." + xmppConnection.getHost();
			HelpBooth booth = new HelpBooth(newRoomId);
			MultiUserChat muc = new MultiUserChat(xmppConnection, newRoomId);
			muc.create("Assist User");

			muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
//			muc.leave();
			booth.setChatRoom(muc);
			activeRooms.put(newRoomId, booth);
			return booth;
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}
	
	public FrontDesk()
	{
		init();
	}
	
	public void closeHelpBooth(String id)
	{
		HelpBooth booth = activeRooms.remove(id);
		if (booth != null)
		{
			try
			{
				booth.getChatRoom().destroy("Help Booth Closed", null);
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	public HelpBooth getBooth(String id)
	{
		return this.activeRooms.get(id);
	}
	
	public String assignAgent(UserContext userContext)
	{
		HelpBooth booth = createHelpRoom();
		try
		{
			//new BotXMPP(new Bot(), getConnectionConfiguration(), booth.getId());
			booth.setBot(new SmartBotXMPP(new SmartBot(userContext), 
					getConnectionConfiguration(), booth.getId()));
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
		return booth.getId();
	}
	
	public String assignHumanAgent(UserContext userContext)
	{
		HelpBooth booth = createHelpRoom();
		booth.joinHuman();
		return booth.getId();
	}
	
	public String testCall()
	{
		return "Bot says Hi!";
	}
}
