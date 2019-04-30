/*
 * Copyright 2015-2019 the original author or authors.
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

import java.time.Duration;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

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
 * @author Artem Bilan
 */
@Configuration
@Import(HttpclientProcessorFunctionConfiguration.class)
@EnableBinding(Processor.class)
public class HttpclientProcessorConfiguration {

	@Bean
	IntegrationFlow httpClientFlow(Processor processor, Function<Message<?>, Object> httpRequest,
			ObjectProvider<RequestHandlerRetryAdvice> requestHandlerRetryAdvice) {
		return IntegrationFlows
				.from(processor.input())
				.transform(Message.class, httpRequest::apply, (e) -> requestHandlerRetryAdvice.ifAvailable(e::advice))
				.channel(processor.output()).get();
	}

	@Bean
	@ConditionalOnProperty(prefix = "httpclient.retry", name = "enabled")
	RequestHandlerRetryAdvice requestHandlerRetryAdvice(HttpclientProcessorProperties processorProperties) {
		RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
		requestHandlerRetryAdvice.setRetryTemplate(createRetryTemplate(processorProperties.getRetry()));
		return requestHandlerRetryAdvice;
	}

	private RetryTemplate createRetryTemplate(HttpclientProcessorProperties.Retry properties) {
		PropertyMapper map = PropertyMapper.get();
		RetryTemplate template = new RetryTemplate();
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		map.from(properties::getMaxAttempts).to(policy::setMaxAttempts);
		template.setRetryPolicy(policy);
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		map.from(properties::getInitialInterval).whenNonNull().as(Duration::toMillis)
				.to(backOffPolicy::setInitialInterval);
		map.from(properties::getMultiplier).to(backOffPolicy::setMultiplier);
		map.from(properties::getMaxInterval).whenNonNull().as(Duration::toMillis)
				.to(backOffPolicy::setMaxInterval);
		template.setBackOffPolicy(backOffPolicy);
		return template;
	}

}
