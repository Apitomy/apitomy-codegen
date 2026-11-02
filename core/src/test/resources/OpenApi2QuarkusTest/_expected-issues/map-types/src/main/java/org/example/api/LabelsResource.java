package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/labels")
public interface LabelsResource {
  @Operation(operationId = "createLabels")
  @POST
  @Consumes("application/json")
  void createLabels(@NotNull Map<String, String> data);
}
