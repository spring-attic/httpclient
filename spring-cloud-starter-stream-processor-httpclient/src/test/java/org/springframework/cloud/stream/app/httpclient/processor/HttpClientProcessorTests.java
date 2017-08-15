/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.httpclient.processor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tests for Http Client Processor.
 *
 * @author Eric Bottard
 * @author Waldemar Hummer
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public abstract class HttpClientProcessorTests {

	@Autowired
	protected Processor channels;

	@Autowired
	protected MessageCollector messageCollector;

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/greet'"
	})
	public static class TestRequestGETTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("..."));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("Hello World")));
		}

	}

	@TestPropertySource(
		properties = "httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/' + payload")
	public static class TestRequestGETWithUrlExpressionUsingMessageTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("greet"));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(containsString("Hello")));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/greet'",
		"httpclient.body={\"foo\":\"bar\"}",
		"httpclient.httpMethod=POST"
	})
	public static class TestRequestPOSTTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("..."));
			assertThat(messageCollector.forChannel(channels.output()),
				receivesPayloadThat(Matchers.allOf(containsString("foo"), containsString("bar"))));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/greet'",
		"httpclient.httpMethod=POST"
	})
	public static class TestRequestPOSTWithBodyExpressionTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("{\"foo\":\"bar\"}"));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(
				Matchers.allOf(containsString("Hello"), containsString("foo"), containsString("bar"))));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/headers'",
		"httpclient.headersExpression={Key1:'value1',Key2:'value2'}"
	})
	public static class TestRequestWithHeadersTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("..."));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("value1 value2")));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') +'/greet'",
		"httpclient.httpMethod=POST",
		"httpclient.headersExpression={Accept:'application/octet-stream'}",
		"httpclient.expectedResponseType=byte[]"
	})
	public static class TestRequestWithReturnTypeTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("hello"));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(Matchers.isA(byte[].class)));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/greet'",
		"httpclient.httpMethod=POST",
		"httpclient.replyExpression=body.substring(3,8)"
	})
	public static class TestRequestWithResultExtractorTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {
			channels.input().send(new GenericMessage<Object>("hi"));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("lo hi")));
		}

	}

	@TestPropertySource(properties = {
		"httpclient.urlExpression='http://localhost:' + @environment.getProperty('local.server.port') + '/json'",
		"httpclient.httpMethod=POST", "httpclient.headersExpression={'Content-Type':'application/json'}"

	})
	public static class TestRequestWithJsonPostTests extends HttpClientProcessorTests {

		@Test
		public void testRequest() {

			channels.input().send(new GenericMessage<>("{\"name\":\"Fred\",\"age\":41}"));
			assertThat(messageCollector.forChannel(channels.output()), receivesPayloadThat(is("id")));
		}

	}

	@RestController
	public static class AdditionalController {

		@RequestMapping("/greet")
		public String greet(@RequestBody(required = false) String who) {
			if (who == null) {
				who = "World";
			}
			return "Hello " + who;
		}

		@RequestMapping("/headers")
		public String headers(@RequestHeader("Key1") String key1, @RequestHeader("Key2") String key2) {
			return key1 + " " + key2;
		}

		@PostMapping("/json")
		public String json(@RequestBody Map<String, Object> request) {
			return "id";
		}

	}

	@SpringBootApplication
	@EnableWebSecurity
	@Import(AdditionalController.class)
	public static class HttpClientProcessorApplication {

	}

}
