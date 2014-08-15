/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.metrics.routepolicy;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.impl.RoutePolicySupport;
import org.apache.camel.util.ObjectHelper;

/**
 * A {@link org.apache.camel.spi.RoutePolicy} which gathers statistics and reports them using {@link com.codahale.metrics.MetricRegistry}.
 * <p/>
 * The metrics is reported in JMX by default, but this can be configured.
 */
public class MetricsRoutePolicy extends RoutePolicySupport {

    private MetricRegistry registry;
    private MetricsRegistryService registryService;
    private boolean useJmx = true;
    private String jmxDomain = "org.apache.camel.metrics";
    private MetricsStatistics statistics;
    private Route route;

    private static final class MetricsStatistics {
        private Counter total;
        private Counter inflight;
        private Meter requests;
        private Timer responses;

        private MetricsStatistics(Counter total, Counter inflight, Meter requests, Timer responses) {
            this.total = total;
            this.inflight = inflight;
            this.requests = requests;
            this.responses = responses;
        }

        public void onExchangeBegin(Exchange exchange) {
            total.inc();
            inflight.inc();
            requests.mark();

            Timer.Context context = responses.time();
            exchange.setProperty("MetricsRoutePolicy", context);
        }

        public void onExchangeDone(Exchange exchange) {
            inflight.dec();

            Timer.Context context = exchange.getProperty("MetricsRoutePolicy", Timer.Context.class);
            if (context != null) {
                context.stop();
            }
        }
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }

    public boolean isUseJmx() {
        return useJmx;
    }

    public void setUseJmx(boolean useJmx) {
        this.useJmx = useJmx;
    }

    public String getJmxDomain() {
        return jmxDomain;
    }

    public void setJmxDomain(String jmxDomain) {
        this.jmxDomain = jmxDomain;
    }

    @Override
    public void onInit(Route route) {
        super.onInit(route);

        this.route = route;
        try {
            registryService = route.getRouteContext().getCamelContext().hasService(MetricsRegistryService.class);
            if (registryService == null) {
                registryService = new MetricsRegistryService();
                registryService.setRegistry(getRegistry());
                registryService.setUseJmx(isUseJmx());
                registryService.setJmxDomain(getJmxDomain());
                route.getRouteContext().getCamelContext().addService(registryService);
            }
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }

        // create statistics holder
        Counter total = registryService.getRegistry().counter(createName("total"));
        Counter inflight = registryService.getRegistry().counter(createName("inflight"));
        Meter requests = registryService.getRegistry().meter(createName("requests"));
        Timer responses = registryService.getRegistry().timer(createName("responses"));
        statistics = new MetricsStatistics(total, inflight, requests, responses);
    }

    private String createName(String type) {
        CamelContext context = route.getRouteContext().getCamelContext();
        String name = context.getManagementName() != null ? context.getManagementName() : context.getName();
        return name + ":" + route.getId() + ":" + type;
    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        if (statistics != null) {
            statistics.onExchangeBegin(exchange);
        }
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        if (statistics != null) {
            statistics.onExchangeDone(exchange);
        }
    }

}