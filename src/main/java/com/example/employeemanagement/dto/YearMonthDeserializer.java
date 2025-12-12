package com.example.employeemanagement.dto;

import java.io.IOException;
import java.time.YearMonth;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class YearMonthDeserializer extends JsonDeserializer<YearMonth> {
	@Override
	public YearMonth deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String value = p.getText();
		if (value == null || value.isEmpty()) {
			return null;
		}
		return YearMonth.parse(value);
	}
}

