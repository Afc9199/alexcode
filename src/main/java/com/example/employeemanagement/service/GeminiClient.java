package com.example.employeemanagement.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.employeemanagement.config.GeminiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@Service
public class GeminiClient {

	private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

	private final RestTemplate restTemplate;
	private final GeminiProperties properties;

	public GeminiClient(GeminiProperties properties, RestTemplateBuilder restTemplateBuilder) {
		this.properties = properties;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(30))
				.build();
	}

	public String getModel() {
		return properties.getModel();
	}

	public String generateAnswer(String systemPrompt, String userPrompt) {
		validateConfiguration();

		GeminiRequest request = new GeminiRequest(
				List.of(new GeminiContent("user", List.of(new GeminiPart(userPrompt)))),
				new GeminiContent("system", List.of(new GeminiPart(systemPrompt))),
				new GeminiGenerationConfig(properties.getTemperature(), properties.getTopP(), properties.getTopK(), properties.getMaxOutputTokens()));

		try {
			ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(buildUrl(), request, GeminiResponse.class);
			GeminiResponse body = response.getBody();
			String answer = body != null ? body.primaryText() : null;
			if (!StringUtils.hasText(answer)) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned an empty response");
			}
			return answer.trim();
		} catch (HttpStatusCodeException ex) {
			log.error("Gemini API error: {}", ex.getResponseBodyAsString(), ex);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, extractErrorMessage(ex));
		} catch (RestClientException ex) {
			log.error("Failed to call Gemini API", ex);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to reach Gemini service");
		}
	}

	private void validateConfiguration() {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
		}
		if (!StringUtils.hasText(properties.getModel())) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini model is not configured");
		}
	}

	private String buildUrl() {
		String base = properties.getApiUrl();
		if (!StringUtils.hasText(base)) {
			base = "https://generativelanguage.googleapis.com/v1beta/models";
		}
		String model = properties.getModel();
		return UriComponentsBuilder.fromHttpUrl(base.replaceAll("/+$", ""))
				.pathSegment(model)
				.path(":generateContent")
				.queryParam("key", properties.getApiKey())
				.toUriString();
	}

	private String extractErrorMessage(HttpStatusCodeException ex) {
		String body = ex.getResponseBodyAsString();
		if (StringUtils.hasText(body)) {
			return body;
		}
		return "Gemini request failed with status " + ex.getStatusCode();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record GeminiRequest(
			List<GeminiContent> contents,
			GeminiContent systemInstruction,
			GeminiGenerationConfig generationConfig) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record GeminiGenerationConfig(
			Double temperature,
			Double topP,
			Integer topK,
			Integer maxOutputTokens) {
	}

	private record GeminiContent(
			String role,
			List<GeminiPart> parts) {
	}

	private record GeminiPart(String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiResponse(
			List<GeminiCandidate> candidates) {

		private String primaryText() {
			if (candidates == null || candidates.isEmpty()) {
				return null;
			}
			GeminiContent content = candidates.get(0).content();
			if (content == null || content.parts() == null || content.parts().isEmpty()) {
				return null;
			}
			GeminiPart part = content.parts().get(0);
			return part != null ? part.text() : null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiCandidate(GeminiContent content) {
	}
}

