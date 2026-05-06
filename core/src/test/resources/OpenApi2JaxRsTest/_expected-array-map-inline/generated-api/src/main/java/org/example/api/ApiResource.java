package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.example.api.beans.ApiList;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/api-lists")
public interface ApiResource {
  @Operation(operationId = "createApiLists")
  @POST
  @Consumes("application/json")
  void createApiLists(@NotNull Map<String, List<ApiList>> data);
}
