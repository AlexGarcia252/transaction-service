package com.bank.transaction.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.math.BigDecimal;

@RegisterRestClient(configKey = "account-api")
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
public interface AccountRestClient {

    @GET
    @Path("/{number}")
    Response getAccountByNumber(@PathParam("number") String number);

    @PUT
    @Path("/{number}/debit")
    Response debit(@PathParam("number") String number, @QueryParam("amount") BigDecimal amount);

    @PUT
    @Path("/{number}/credit")
    Response credit(@PathParam("number") String number, @QueryParam("amount") BigDecimal amount);
}