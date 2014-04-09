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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import mjson.Json;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.resource.Directory;
import org.sharegov.servicebot.rest.ServiceBotApplication;

public class StartUp
{
    private static void startup(final Json config) throws Exception
    {
		Component server = new Component();
	    server.getServers().add(Protocol.HTTP, config.at("port").asInteger());
	    server.getClients().add(Protocol.HTTP);
	    server.getClients().add(Protocol.FILE);
	    
	    final JaxRsApplication app = new JaxRsApplication(server.getContext().createChildContext());
	        	
	    ServiceBotApplication appImpl = new ServiceBotApplication();
	    app.add(appImpl);
	    server.getDefaultHost().attach(app);
	    
//	    Application application = new Application(server.getContext().createChildContext()) {  
//	        @Override  
//	        public Restlet createInboundRoot() {  
//	            return new Directory(getContext().createChildContext(), 
//	            		"file:///" + config.at("workingDir").asString() + "/client/");  
//	        }  
//	    }; 
//	    server.getDefaultHost().attach("/client", application);
	    
	    BotApp.get().initialize(config);
	    
	    server.start();	    
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) 
        {
            System.err.println("Usage: StartUp configFile");
            System.exit(-1);
        }
        try
        {        	
            startup(Json.read(new String(getBytesFromFile(new File(args[0])))));
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }
    
    public static byte[] getBytesFromFile(File file) throws IOException
    {
        return getBytesFromStream(new FileInputStream(file), true);
    }    
    
    // Returns the contents of the file in a byte array.
    public static byte[] getBytesFromStream(InputStream is, boolean close) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            byte [] A = new byte[4096];
            // Read in the bytes
            for (int cnt = is.read(A); cnt > -1; cnt = is.read(A))
                out.write(A, 0, cnt);
            return out.toByteArray();
            // Close the input stream and return bytes
        }
        finally
        {
            if (close) is.close();
        }
    }
    
}
