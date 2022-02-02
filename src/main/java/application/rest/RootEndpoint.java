package application.rest;

import java.io.InputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/login")
@RolesAllowed({ "developers" })
public class RootEndpoint {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getResources(@Context UriInfo uriInfo) {
    System.out.println("Returning authentication status: ");
    return Response.ok("{\"Authentication Status\":\"" + "Successful\"}").build();
  }

  @GET
  @Produces({ MediaType.TEXT_HTML })
  public InputStream getInd() {
    try {
      return this.getClass().getResourceAsStream("/index.html");
    } catch (Exception e) {
      throw new RuntimeException("Exception returning index.html", e);
    }
  }
}
