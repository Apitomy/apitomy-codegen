package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.io.InputStream;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/untagged")
public interface UntaggedResource {
  @Operation(operationId = "createUntagged")
  @POST
  @Consumes("application/json")
  void createUntagged(@NotNull InputStream data);
}
