/*******************************************************************************
 * Copyright 2022 IBM Corp. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *******************************************************************************/

package application.userops;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

@Path("/userInfo")
public class UserEndpoint {

	private static Properties props = new Properties();
	private static Logger logger = Logger.getLogger(UserEndpoint.class.getName());

	static {
		try {
			ClassLoader classLoader = UserEndpoint.class.getClassLoader();
			InputStream input = classLoader.getResourceAsStream("verify.config");
			props.load(input);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error loading Security Verify configuration.");
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getUserDetails(@CookieParam("verify_token") String token) {
		String errorHTML = "Error occurred!";
		String username = "username";
		String name= "displayname";
		
		try {
			boolean isValidRequest = true;
			String userInfo = "";
			File errorFile = new File(UserEndpoint.class.getClassLoader().getResource("error.html").getPath());
			errorHTML = FileUtils.readFileToString(errorFile, StandardCharsets.UTF_8);

			if (token == null) {
				isValidRequest = false;
			}
			if (token != null) {
				HttpPost post = new HttpPost(props.getProperty("introspectionUrl"));
				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("client_id", props.getProperty("clientId")));
				urlParameters.add(new BasicNameValuePair("client_secret", props.getProperty("clientSecret")));
				urlParameters.add(new BasicNameValuePair("token", token));

				post.setEntity(new UrlEncodedFormEntity(urlParameters));
				String result = "";
				try (CloseableHttpClient httpClient = HttpClients.createDefault();
						CloseableHttpResponse res = httpClient.execute(post)) {
					result = EntityUtils.toString(res.getEntity());
					logger.log(Level.INFO, "Token introspection results:" + result);
					JSONObject tokenIntro = new JSONObject(result);
					if (tokenIntro.getBoolean("active") == false) {
						isValidRequest = false;
					}
				}
			}

			if (isValidRequest) {
				// Invoke userInfo endpoint
				HttpPost postUserInfo = new HttpPost(props.getProperty("userInfoUrl"));

				String authHeader = "Bearer " + token;
				postUserInfo.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

				try (CloseableHttpClient httpClient1 = HttpClients.createDefault();
						CloseableHttpResponse res = httpClient1.execute(postUserInfo)) {
					String result = EntityUtils.toString(res.getEntity());
					logger.log(Level.INFO, "User info results:" + result);
					JSONObject userInfoObj = new JSONObject(result);
					username = userInfoObj.getString("preferred_username");
					name = userInfoObj.getString("displayName");
					userInfo = result;
				}

				// Free marker template
				Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
				
				File profileTemplateFile = new File(UserEndpoint.class.getClassLoader().getResource("profile.ftlh").getPath());
				cfg.setDirectoryForTemplateLoading(new File(profileTemplateFile.getParent()));
				cfg.setDefaultEncoding("UTF-8");
				cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
				cfg.setLogTemplateExceptions(false);
				cfg.setWrapUncheckedExceptions(true);
				cfg.setFallbackOnNullLoopVariable(false);

				Map<String, Object> root = new HashMap<>();
				root.put("username", username);
				root.put("name", name);
			
				Template temp = cfg.getTemplate("profile.ftlh");
				File profileHTMLFile = new File("profile.html");
				Writer out = new FileWriter(profileHTMLFile);
		        temp.process(root, out);
		        out.flush();
		        out.close();
                userInfo = FileUtils.readFileToString(profileHTMLFile, StandardCharsets.UTF_8);
				return Response.ok(userInfo).build();
			} else {
				return Response.ok(errorHTML).status(401).build();
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
			return Response.ok(errorHTML).status(401).build();
		}
	}

}
