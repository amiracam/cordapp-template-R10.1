package com.template.api;

import com.template.flow.TemplateFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.ContractsDSL;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.ServiceEntry;
import net.corda.core.serialization.OpaqueBytes;
import net.corda.core.transactions.SignedTransaction;
import net.corda.flows.CashFlowCommand;
import net.corda.flows.IssuerFlow;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.TimeUnit;

// This API is accessible from /api/template. The endpoint paths specified below are relative to it.
@Path("template")
public class TemplateApi {
    private final CordaRPCOps services;

    public TemplateApi(CordaRPCOps services) {
        this.services = services;
    }

    /**
     * Accessible at /api/template/templateGetEndpoint.
     */
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response templateGetEndpoint() {
        return Response.accepted().entity("Template GET endpoint.").build();
    }

    /**
     * Accessible at /api/template/templatePutEndpoint.
     */
    @PUT
    @Path("templatePutEndpoint")
    public Response templatePutEndpoint(Object payload) {
        return Response.accepted().entity("Template PUT endpoint.").build();
    }

    @GET
    @Path("vault")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getAllTransactions() {
        return services.vaultAndUpdates().getFirst();
    }


    @GET
    @Path("issue/{peerName}/{amount}")
    public String issue(@PathParam("peerName") String peerName, @PathParam("amount") int quantity) {
        try {
            return issueMoney(peerName, quantity, ContractsDSL.USD);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @GET
    @Path("test")
    public String test() {

        System.out.println("starting flow");

        Amount<Currency> amount = new Amount<>(100, ContractsDSL.USD);

        FlowHandle flowHandle = services.startFlowDynamic(
                TemplateFlow.Initiator.class,
                services.partyFromName("NodeC"),
                services.partyFromName("NodeB"),
                amount);

        try {
            return ((SignedTransaction) flowHandle.getReturnValue().get()).getId().toString();
        } catch (Exception e) {
            System.out.println("error1: " + e.getMessage());
            e.printStackTrace();
        }

        return "done";
    }

    private String issueMoney(String peerName, long quantity, Currency currency) throws Exception {


        Party party = services.partyFromName(peerName);


        CashFlowCommand.IssueCash cash = new CashFlowCommand.IssueCash(new Amount<>(quantity, currency),
                OpaqueBytes.Companion.of((byte) 1), party, getNotary());

        FlowHandle handle = services.startFlowDynamic(IssuerFlow.IssuanceRequester.class, cash.getAmount(), cash.getRecipient(), cash.getIssueRef(), services.nodeIdentity().getLegalIdentity());
        SignedTransaction signedTransaction = (SignedTransaction) handle.getReturnValue().get(10 * 1000, TimeUnit.MILLISECONDS);

        return signedTransaction.getId().toString();
    }


    private Party getNotary() {
        for (NodeInfo nodeInfo :
                services.networkMapUpdates().getFirst()) {
            for (ServiceEntry serviceEntry :
                    nodeInfo.getAdvertisedServices()) {
                if (serviceEntry.getInfo().getType().isNotary()) {
                    return nodeInfo.getNotaryIdentity();
                }
            }
        }
        return null;
    }
}