package com.bank.transaction.resource;

import com.bank.transaction.client.AccountRestClient;
import com.bank.transaction.dto.TransactionRequestDTO;
import com.bank.transaction.entity.Transaction;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.time.LocalDateTime;
import java.util.List;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    @RestClient
    AccountRestClient accountClient;

    // RF3: Iniciar Transferencia
    @POST
    @Transactional
    public Response createTransaction(@Valid TransactionRequestDTO request) {
        if (request.sourceAccount().equals(request.targetAccount())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"La cuenta origen y destino no pueden ser la misma\"}").build();
        }

        // 1. Verificar existencia
        try {
            accountClient.getAccountByNumber(request.sourceAccount());
            accountClient.getAccountByNumber(request.targetAccount());
        } catch (jakarta.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Una o ambas cuentas no existen\"}").build();
            }
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"Error al validar cuentas\"}").build();
        }

        // 2. Registrar transacción PENDING
        Transaction tx = new Transaction();
        tx.sourceAccount = request.sourceAccount();
        tx.targetAccount = request.targetAccount();
        tx.amount = request.amount();
        tx.description = request.description();
        tx.transactionDate = LocalDateTime.now();
        tx.status = "PENDING";
        tx.persist();

        // 3. Debitar
        try {
            accountClient.debit(request.sourceAccount(), request.amount());
        } catch (jakarta.ws.rs.WebApplicationException e) {
            tx.status = "FAILED";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Saldo insuficiente o cuenta inválida\"}").build();
        }

        // 4. Acreditar (Saga)
        try {
            accountClient.credit(request.targetAccount(), request.amount());
        } catch (Exception e) {
            accountClient.credit(request.sourceAccount(), request.amount());
            tx.status = "FAILED";
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Fallo en el crédito. Se ha revertido el dinero a la cuenta origen.\"}").build();
        }

        tx.status = "COMPLETED";
        return Response.status(Response.Status.CREATED).entity(tx).build();
    }

    // RF4: Consultar historial de una cuenta
    @GET
    @Path("/account/{accountNumber}")
    public List<Transaction> getHistory(@PathParam("accountNumber") String accountNumber) {
        return Transaction.find("sourceAccount = ?1 or targetAccount = ?1", accountNumber).list();
    }

    // RF4: Consultar detalle de una transacción
    @GET
    @Path("/{id}")
    public Response getTransactionById(@PathParam("id") Long id) {
        Transaction tx = Transaction.findById(id);
        if (tx == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Transacción no encontrada\"}").build();
        }
        return Response.ok(tx).build();
    }
}