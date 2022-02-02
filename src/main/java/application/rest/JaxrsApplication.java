package application.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/auth")
@RolesAllowed({ "developers" })
public class JaxrsApplication extends Application {

}
