/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.httpclient.processor;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;

/**
 * A processor app that makes requests to an HTTP resource and emits the
 * response body as a message payload. This processor can be combined, e.g.,
 * with a time source module to periodically poll results from a HTTP resource.
 *
 * @author Waldemar Hummer
 * @author Mark Fisher
 * @author Gary Russell
 * @author Christian Tzolov
 * @author David Turanski
 */
@Configuration
@Import(HttpclientProcessorFunctionConfiguration.class)
@EnableBinding(Processor.class)

public class HttpclientProcessorConfiguration {

	@Autowired
	private Function<Message<?>, Object> httpRequest;

	@Bean
	IntegrationFlow httpClientflow(Processor processor) {
		return IntegrationFlows
			.from(processor.input())
			.transform(Message.class, httpRequest::apply)
			.channel(processor.output()).get();
	}
}
