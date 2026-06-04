package org.example.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.example.api.beans.Beer;
import org.example.api.beans.Brewery;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/breweries")
public interface BreweriesResource {
  /**
   * <p>
   * Returns a list of all the breweries.
   * </p>
   * 
   */
  @Operation(description = "Returns a list of all the breweries.", summary = "Get All Breweries", operationId = "listAllBreweries")
  @GET
  @Produces("application/json")
  CompletionStage<RestResponse<Set<Brewery>>> listAllBreweries();

  /**
   * <p>
   * Adds a single brewery to the data set.
   * </p>
   * 
   */
  @Operation(description = "Adds a single brewery to the data set.", summary = "Add a Brewery", operationId = "addBrewery")
  @POST
  @Consumes("application/json")
  CompletionStage<RestResponse<Void>> addBrewery(@NotNull Brewery data);

  /**
   * <p>
   * Returns full information about a single brewery.
   * </p>
   * 
   */
  @Operation(description = "Returns full information about a single brewery.", summary = "Gets Info About a Brewery", operationId = "getBrewery")
  @Path("/{breweryId}")
  @GET
  @Produces("application/json")
  CompletionStage<RestResponse<Brewery>> getBrewery(@PathParam("breweryId") int breweryId);

  /**
   * <p>
   * Updates information about a single brewery.
   * </p>
   * 
   */
  @Operation(description = "Updates information about a single brewery.", summary = "Update a Brewery", operationId = "updateBrewery")
  @Path("/{breweryId}")
  @PUT
  @Consumes("application/json")
  CompletionStage<RestResponse<Void>> updateBrewery(@PathParam("breweryId") int breweryId, @NotNull Brewery data);

  /**
   * <p>
   * Removes a single brewery from the data set.
   * </p>
   * 
   */
  @Operation(description = "Removes a single brewery from the data set.", summary = "Delete a Brewery", operationId = "deleteBrewery")
  @Path("/{breweryId}")
  @DELETE
  CompletionStage<RestResponse<Void>> deleteBrewery(@PathParam("breweryId") int breweryId);

  /**
   * <p>
   * Returns all of the beers made by the brewery.
   * </p>
   * 
   */
  @Operation(description = "Returns all of the beers made by the brewery.", summary = "Gets Beers From a Brewery", operationId = "listBreweryBeers")
  @Path("/{breweryId}/beers")
  @GET
  @Produces("application/json")
  CompletionStage<RestResponse<List<Beer>>> listBreweryBeers(@PathParam("breweryId") int breweryId);

  /**
   * <p>
   * Adds a single beer to the data set for this brewery.
   * </p>
   * 
   */
  @Operation(description = "Adds a single beer to the data set for this brewery.", summary = "Adds a Beer to the Brewery", operationId = "addBeerToBrewery")
  @Path("/{breweryId}/beers")
  @POST
  @Consumes("application/json")
  RestResponse<Void> addBeerToBrewery(@PathParam("breweryId") int breweryId, @NotNull Beer data);
}
