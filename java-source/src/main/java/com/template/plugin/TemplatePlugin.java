package com.template.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.template.api.TemplateApi;
import com.template.flow.TemplateFlow;
import com.template.service.TemplateService;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.node.PluginServiceHub;
import net.corda.core.serialization.OpaqueBytes;
import net.corda.core.serialization.SerializationCustomization;
import net.corda.flows.AbstractCashFlow;
import net.corda.flows.CashPaymentFlow;
import net.corda.flows.IssuerFlow;

import java.util.*;
import java.util.function.Function;

public class TemplatePlugin extends CordaPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(TemplateApi::new);

    /**
     * A list of flows required for this CorDapp.
     */
    private final Map<String, Set<String>> requiredFlows = ImmutableMap.of(
            TemplateFlow.Initiator.class.getName(),
            ImmutableSet.of(
                    Amount.class.getName(),
                    Party.class.getName()
            ),
            CashPaymentFlow.class.getName(),
            ImmutableSet.of(
                    Amount.class.getName(),
                    Party.class.getName()
            ),
            IssuerFlow.IssuanceRequester.class.getName(),
            new HashSet<>(Arrays.asList(
                    AbstractCashFlow.class.getName(),
                    Party.class.getName(),
                    Amount.class.getName(),
                    OpaqueBytes.class.getName()
            ))
    );

    /**
     * A list of long-lived services to be hosted within the node.
     */
    private final List<Function<PluginServiceHub, ?>> servicePlugins = ImmutableList.of(IssuerFlow.Issuer.Service::new, TemplateService::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     * The template's web frontend is accessible at /web/template.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the templateWeb directory in resources to /web/template
            "template", getClass().getClassLoader().getResource("templateWeb").toExternalForm()
    );

    /**
     * Whitelisting the required types for serialisation by the Corda node.
     */
    @Override
    public boolean customizeSerialization(SerializationCustomization custom) {
        return true;
    }

    @Override
    public List<Function<CordaRPCOps, ?>> getWebApis() {
        return webApis;
    }

    @Override
    public Map<String, Set<String>> getRequiredFlows() {
        return requiredFlows;
    }

    @Override
    public List<Function<PluginServiceHub, ?>> getServicePlugins() {
        return servicePlugins;
    }

    @Override
    public Map<String, String> getStaticServeDirs() {
        return staticServeDirs;
    }
}