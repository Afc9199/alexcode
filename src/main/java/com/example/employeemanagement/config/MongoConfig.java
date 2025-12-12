package com.example.employeemanagement.config;

import java.time.YearMonth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.lang.NonNull;

@Configuration
public class MongoConfig {

	@Bean
	public MongoCustomConversions customConversions() {
		return new MongoCustomConversions(
				java.util.Arrays.asList(
						new YearMonthToStringConverter(),
						new StringToYearMonthConverter()));
	}

	@WritingConverter
	static class YearMonthToStringConverter implements Converter<YearMonth, String> {
		@Override
		public String convert(@NonNull YearMonth source) {
			return source.toString();
		}
	}

	@ReadingConverter
	static class StringToYearMonthConverter implements Converter<String, YearMonth> {
		@Override
		public YearMonth convert(@NonNull String source) {
			return source.isEmpty() ? null : YearMonth.parse(source);
		}
	}
}

