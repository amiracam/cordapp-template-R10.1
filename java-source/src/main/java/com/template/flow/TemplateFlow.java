package com.template.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.contracts.asset.Cash;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractsDSL;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.flows.CashPaymentFlow;
import net.corda.flows.TwoPartyTradeFlow;

import java.util.Currency;

/**
 * Define your flow here.
 */
public class TemplateFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    public static class Initiator extends FlowLogic<SignedTransaction> {


        private final Party fxTrader;
        private final Party receiver;
        private final Amount<Currency> amount;

        public Initiator(Party fxTrader, Party receiver, Amount<Currency> amount) {
            System.out.println("creating flow");
            this.fxTrader = fxTrader;
            this.receiver = receiver;
            this.amount = amount;
        }

        /**
         * Define the initiator's flow logic here.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            System.out.println("in flow");

            NodeInfo notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0);

            SignedTransaction signedTransaction = subFlow(new CashPaymentFlow(amount, getServiceHub().getMyInfo().getLegalIdentity()));

            //waitForLedgerCommit(signedTransaction.getId());

            System.out.println("self give money " + signedTransaction);

            Amount<Currency> amount = new Amount<>(100, ContractsDSL.USD);

            System.out.println("sending to buer");

            send(fxTrader, amount);

            Object o = subFlow(new TwoPartyTradeFlow.Seller(
                            fxTrader,
                            notary,
                            signedTransaction.getTx().outRef(0),
                            amount,
                            getServiceHub().getLegalIdentityKey(),
                            TwoPartyTradeFlow.Seller.Companion.tracker()),
                    true

            );


            System.out.println("DONE: " + o);
            return (SignedTransaction) o;
        }
    }

    public static class Acceptor extends FlowLogic<Void> {
        private Party counterparty;

        public Acceptor(Party counterparty) {
            this.counterparty = counterparty;
        }

        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        @Override
        public Void call() throws FlowException {

            NodeInfo notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0);


            Amount o = receive(Amount.class, counterparty).unwrap(amount -> amount);

            System.out.println("REC: " + o);

            //Amount<Currency> amount = new Amount<>(10, ContractsDSL.USD);

            TwoPartyTradeFlow.Buyer buyer = new TwoPartyTradeFlow.Buyer(
                    counterparty,
                    notary.getNotaryIdentity(),
                    o,
                    Cash.State.class
            );

            System.out.println("sending to buyer");


            try {

                SignedTransaction tx = subFlow(buyer, true);

                System.out.println("Buyer Flow: " + tx);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
