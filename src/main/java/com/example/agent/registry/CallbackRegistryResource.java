package com.example.agent.registry;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for managing agent callbacks.
 */
@Path("/api/callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CallbackRegistryResource {
    
    private final CallbackRegistry callbackRegistry;
    
    public CallbackRegistryResource(CallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }
    
    /**
     * Get all available callbacks
     */
    @GET
    @Timed
    public Response getAllCallbacks() {
        try {
            List<CallbackMetadata> callbacks = callbackRegistry.getAllCallbacks();
            return Response.ok(callbacks).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch callbacks: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get callbacks by type (before_agent or after_agent)
     */
    @GET
    @Path("/type/{type}")
    @Timed
    public Response getCallbacksByType(@PathParam("type") String type) {
        try {
            List<CallbackMetadata> callbacks = callbackRegistry.getCallbacksByType(type.toUpperCase());
            return Response.ok(callbacks).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch callbacks: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get callback by ID
     */
    @GET
    @Path("/{id}")
    @Timed
    public Response getCallback(@PathParam("id") String id) {
        try {
            return callbackRegistry.getCallback(id)
                    .map(callback -> Response.ok(callback).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Callback not found: " + id))
                            .build());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch callback: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Register a new callback
     */
    @POST
    @Timed
    public Response registerCallback(CallbackMetadata metadata) {
        try {
            CallbackMetadata registered = callbackRegistry.registerCallback(metadata);
            return Response.status(Response.Status.CREATED).entity(registered).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Failed to register callback: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Update callback
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response updateCallback(@PathParam("id") String id, CallbackMetadata metadata) {
        try {
            CallbackMetadata updated = callbackRegistry.updateCallback(id, metadata);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update callback: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Delete callback
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response deleteCallback(@PathParam("id") String id) {
        try {
            boolean deleted = callbackRegistry.deleteCallback(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Callback not found: " + id))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete callback: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Simple error response class
     */
    public static class ErrorResponse {
        public final String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
