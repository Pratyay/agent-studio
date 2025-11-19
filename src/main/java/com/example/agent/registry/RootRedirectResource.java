package com.example.agent.registry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Redirects root path to the UI.
 */
@Path("/")
public class RootRedirectResource {
    
    @GET
    public Response redirectToUI() {
        return Response.seeOther(URI.create("/ui")).build();
    }
}
