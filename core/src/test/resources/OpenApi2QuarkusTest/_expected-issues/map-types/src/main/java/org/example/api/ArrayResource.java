package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.example.api.beans.Widget;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/array-maps")
public interface ArrayResource {
  @Operation(operationId = "getArrayMap")
  @GET
  @Produces("application/json")
  Map<String, List<Widget>> getArrayMap();

  @Operation(operationId = "updateArrayMap")
  @PUT
  @Consumes("text/plain")
  void updateArrayMap(@NotNull String data);

  @Operation(operationId = "updateArrayMap")
  @PUT
  @Consumes("application/json")
  void updateArrayMap(@NotNull Map<String, List<Widget>> data);

  @Operation(operationId = "createArrayMap")
  @POST
  @Consumes("application/json")
  void createArrayMap(@NotNull Map<String, List<Widget>> data);
}
