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

package org.apache.camel.component.cxf.jaxrs;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.NoErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CXFTestSupport;
import org.apache.camel.component.cxf.jaxrs.testbean.CustomException;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CxfRsSpringConsumerTest extends CamelSpringTestSupport {
    private static int port1 = CXFTestSupport.getPort1(); 
    
    
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                errorHandler(new NoErrorHandlerBuilder());
                from("cxfrs://bean://rsServer").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // just throw the CustomException here
                        throw new CustomException("Here is the exception");
                    }  
                });
            }
        };
    }
    
    
    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/camel/component/cxf/jaxrs/CxfRsSpringConsumer.xml");
    }
    
    @Test
    public void testMappingException() throws Exception {
        HttpGet get = new HttpGet("http://localhost:" + port1 + "/CxfRsSpringConsumerTest/customerservice/customers/126");
        get.addHeader("Accept" , "application/json");
        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpResponse response = httpclient.execute(get);
            assertEquals("Get a wrong status code", 500, response.getStatusLine().getStatusCode());
            assertEquals("Get a worng message header", "exception: Here is the exception", response.getHeaders("exception")[0].toString());
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

}