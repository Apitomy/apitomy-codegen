package org.example.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.example.api.beans.Pet;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/pets")
public interface PetsResource {
  @Operation(summary = "Get a pet", operationId = "getPet")
  @GET
  @Produces("application/json")
  Pet getPet();
}
