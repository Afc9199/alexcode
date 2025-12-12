package com.example.employeemanagement.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "attendance.security")
public class AttendanceSecurityProperties {

	private final Location location = new Location();
	private List<String> allowedNetworks = new ArrayList<>();

	public Location getLocation() {
		return location;
	}

	public List<String> getAllowedNetworks() {
		return allowedNetworks;
	}

	public void setAllowedNetworks(List<String> allowedNetworks) {
		this.allowedNetworks = allowedNetworks;
	}

	public static class Location {
		private double latitude = 0;
		private double longitude = 0;
		private double radiusMeters = 100;
		private double maxAccuracyMeters = 150;

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public double getRadiusMeters() {
			return radiusMeters;
		}

		public void setRadiusMeters(double radiusMeters) {
			this.radiusMeters = radiusMeters;
		}

		public double getMaxAccuracyMeters() {
			return maxAccuracyMeters;
		}

		public void setMaxAccuracyMeters(double maxAccuracyMeters) {
			this.maxAccuracyMeters = maxAccuracyMeters;
		}
	}
}

