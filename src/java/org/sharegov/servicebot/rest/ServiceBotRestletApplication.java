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

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;

public class ServiceBotRestletApplication extends JaxRsApplication
{
	
    @Override
    public Restlet createInboundRoot() 
    {
    	this.add(new ServiceBotApplication());
        Restlet restlet = super.createInboundRoot();
        this.setStatusService(new StatusService() {

			@Override
			public Representation getRepresentation(Status status,
					Request request, Response response)
			{
				if (status.getThrowable() != null)
					status.getThrowable().printStackTrace(System.err);
				return super.getRepresentation(status, request, response);
			}        	
        });
        return restlet;
    }

/*	@Override
	public Restlet createRoot()
	{

		Restlet restlet = new Restlet()
		{
			@Override
			public void handle(Request request, Response response)
			{
				StringBuilder stringBuilder = new StringBuilder();

				stringBuilder.append("<html>");
				stringBuilder
						.append("<head><title>Sample Application Servlet Page</title></head>");
				stringBuilder.append("<body bgcolor=white>");

				stringBuilder.append("<table border=\"0\">");
				stringBuilder.append("<tr>");
				stringBuilder.append("<td>");
				stringBuilder.append("<h1>Sample Application Restlet</h1>");
				stringBuilder
						.append("This is the output of a restlet that is part of");
				stringBuilder
						.append("the testServlet application.  It displays the");
				stringBuilder
						.append("request headers from the request we are currently");
				stringBuilder.append("processing.");
				stringBuilder.append("</td>");
				stringBuilder.append("</tr>");
				stringBuilder.append("</table>");

				stringBuilder.append("<table border=\"0\" width=\"100%\">");
				stringBuilder.append("<tr>");
				stringBuilder
						.append("  <th align=\"right\">Accepted character sets :</th>");
				stringBuilder.append("  <td>"
						+ request.getClientInfo().getAcceptedCharacterSets()
						+ "</td>");
				stringBuilder.append("</tr>");
				stringBuilder.append("<tr>");
				stringBuilder
						.append("  <th align=\"right\">Accepted encodings :</th>");
				stringBuilder.append("  <td>"
						+ request.getClientInfo().getAcceptedEncodings()
						+ "</td>");
				stringBuilder.append("</tr>");

				stringBuilder.append("<tr>");
				stringBuilder
						.append("  <th align=\"right\">Accepted media types :</th>");
				stringBuilder.append("  <td>"
						+ request.getClientInfo().getAcceptedMediaTypes()
						+ "</td>");
				stringBuilder.append("</tr>");

				stringBuilder.append("<tr>");
				stringBuilder.append("  <th align=\"right\">Address :</th>");
				stringBuilder.append("  <td>"
						+ request.getClientInfo().getAddress() + "</td>");
				stringBuilder.append("</tr>");

				stringBuilder.append("<tr>");
				stringBuilder.append("  <th align=\"right\">Agent :</th>");
				stringBuilder.append("  <td>"
						+ request.getClientInfo().getAgent() + "</td>");
				stringBuilder.append("</tr>");

				stringBuilder.append("</table>");

				stringBuilder.append("</body>");
				stringBuilder.append("</html>");

				response.setEntity(new StringRepresentation(stringBuilder
						.toString(), MediaType.TEXT_HTML));

			}
		};

		return restlet;
	} */
}
