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
package org.sharegov.servicebot.user;

import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.restlet.security.SecretVerifier;

/**
 * LDAP Verifier
 * 
 * @author Julian Bonilla <julianb@miamidade.gov>
 */
public class LdapVerifier extends SecretVerifier {
	
	private final String ldapServerUrl;
	private final String ldapBaseDN;

	/**
	 * 
	 * @param ldapServerUrl URL of the LDAP server, for example
	 * @param ldapBaseDN LDAP base DN
	 */
	public LdapVerifier(String ldapServerUrl, String ldapBaseDN) {
		this.ldapServerUrl = ldapServerUrl;
		this.ldapBaseDN = ldapBaseDN;
	}
	

	@Override
	public int verify(String identifier, char[] inputSecret) {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, this.ldapServerUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "none");

		SearchControls searchCtrls = new SearchControls();
		searchCtrls.setReturningAttributes(new String[] {});
		searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		String filter = "(&(cn=" + identifier + "))";

		DirContext ctx = null;
		try {
			ctx = new InitialDirContext(env);
			NamingEnumeration<SearchResult> answer = ctx.search(
					this.ldapBaseDN, filter, searchCtrls);

			String fullname = null;
			if (answer.hasMore()) {
				fullname = answer.next().getNameInNamespace();
			}

			ctx.close();
			ctx = null;

			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, fullname);
			env.put(Context.SECURITY_CREDENTIALS, inputSecret);

			ctx = new InitialDirContext(env);
			return 1;
		} catch (AuthenticationException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}
		}

		return 0;
	}
}
