package application.redirect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

@Path("/oidcclient/redirect/tutorial")
public class RedirectEndpoint {

	private static Properties props = new Properties();
	private static Logger logger = Logger.getLogger(RedirectEndpoint.class.getName());

	static {
		try {
			ClassLoader classLoader = RedirectEndpoint.class.getClassLoader();
			InputStream input = classLoader.getResourceAsStream("verify.config");
			props.load(input);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error loading Security Verify configuration.");
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@GET
	@Produces({ MediaType.TEXT_HTML })
	public Response getIndex(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		String errorHTML = "Error occurred!";
		try {
			String code = request.getParameter("code");
			errorHTML = FileUtils.readFileToString(
					new File(RedirectEndpoint.class.getClassLoader().getResource("error.html").getPath()),
					StandardCharsets.UTF_8);

			System.out.println("Code:" + code);

			System.out.println("Props:" + props.toString());
			HttpPost post = new HttpPost(props.getProperty("tokenUrl"));
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("client_id", props.getProperty("clientId")));
			urlParameters.add(new BasicNameValuePair("client_secret", props.getProperty("clientSecret")));
			urlParameters.add(new BasicNameValuePair("code", code));
			urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
			urlParameters.add(
					new BasicNameValuePair("redirect_uri", "http://localhost:9080/app/oidcclient/redirect/tutorial"));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			String result = "";
			try (CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse res = httpClient.execute(post)) {
				result = EntityUtils.toString(res.getEntity());
				JSONObject tokenObj = new JSONObject(result);

				String accessToken = tokenObj.getString("access_token");
				logger.log(Level.INFO, "Tokens:" + accessToken);
				NewCookie cookie = new NewCookie("verify_token", accessToken, "/", "localhost",
						"Security Verify Access Token", 10000, true, true);
				String returnHTML = FileUtils.readFileToString(
						new File(RedirectEndpoint.class.getClassLoader().getResource("redirect.html").getPath()),
						StandardCharsets.UTF_8);
				return Response.ok(returnHTML).cookie(cookie).build();
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
			return Response.ok(errorHTML).status(401).build();
		}

	}

}
