package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.example.api.beans.Beer;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/beers")
public interface BeersResource {
  /**
   * <p>
   * Returns full information about a single beer.
   * </p>
   * 
   */
  @Operation(description = "Returns full information about a single beer.", summary = "Get Info About a Beer", operationId = "getBeer")
  @Path("/{beerId}")
  @GET
  @Produces("application/json")
  RestResponse<Beer> getBeer(
      @PathParam("beerId") @Positive(message = "The beerId must be a natural number!") int beerId);

  /**
   * <p>
   * Updates information about a single beer.
   * </p>
   * 
   */
  @Operation(description = "Updates information about a single beer.", summary = "Update a Beer", operationId = "updateBeer")
  @Path("/{beerId}")
  @PUT
  @Consumes("application/json")
  RestResponse<Void> updateBeer(
      @PathParam("beerId") @Positive(message = "The beerId must be a natural number!") int beerId, @NotNull Beer data);

  /**
   * <p>
   * Removes a single beer from the data set.
   * </p>
   * 
   */
  @Operation(description = "Removes a single beer from the data set.", summary = "Delete a Beer", operationId = "deleteBeer")
  @Path("/{beerId}")
  @DELETE
  RestResponse<Void> deleteBeer(
      @PathParam("beerId") @Positive(message = "The beerId must be a natural number!") int beerId);

  /**
   * <p>
   * Returns all of the beers in the database.
   * </p>
   * 
   */
  @Operation(description = "Returns all of the beers in the database.", summary = "Get All Beers", operationId = "listAllBeers")
  @GET
  @Produces("application/json")
  RestResponse<List<Beer>> listAllBeers();

  /**
   * <p>
   * Adds a single beer to the dataset.
   * </p>
   * 
   */
  @Operation(description = "Adds a single beer to the dataset.", summary = "Add a Beer", operationId = "addBeer")
  @POST
  @Consumes("application/json")
  RestResponse<Void> addBeer(@NotNull Beer data);
}
