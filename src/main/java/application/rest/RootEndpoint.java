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
