package com.example.employeemanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.config.AttendanceSecurityProperties;
import com.example.employeemanagement.dto.CreateAttendanceRequest;
import com.example.employeemanagement.model.AttendanceRecord;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.AttendanceRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private CompanySettingsService companySettingsService;

	private AttendanceService attendanceService;
	private AttendanceSecurityProperties securityProperties;

	@BeforeEach
	void setUp() {
		securityProperties = new AttendanceSecurityProperties();
		securityProperties.getLocation().setLatitude(0);
		securityProperties.getLocation().setLongitude(0);
		securityProperties.getLocation().setRadiusMeters(1000);
		securityProperties.getLocation().setMaxAccuracyMeters(200);
		attendanceService = new AttendanceService(attendanceRepository, userAccountRepository, securityProperties, companySettingsService);
	}

	@Test
	void recordAttendancePersistsNewEntry() {
		UserAccount account = new UserAccount();
		account.setId("user-1");
		when(userAccountRepository.findById("user-1")).thenReturn(Optional.of(account));
		when(attendanceRepository.existsByUserIdAndWorkDate("user-1", LocalDate.parse("2025-11-18"))).thenReturn(false);

		doReturn(new AttendanceRecord()).when(attendanceRepository).save(any(AttendanceRecord.class));

		var response = attendanceService.recordAttendance(
				new CreateAttendanceRequest("user-1", LocalDate.parse("2025-11-18"), LocalTime.parse("08:30"), null,
						0.0, 0.0, 5.0, "On site"),
				"127.0.0.1");

		assertThat(response.id()).isNotBlank();
		assertThat(response.status()).isNotNull();
	}

	@Test
	void recordAttendanceRejectsUnknownUser() {
		when(userAccountRepository.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> attendanceService.recordAttendance(
				new CreateAttendanceRequest("missing", LocalDate.now(), LocalTime.NOON, null, 0.0, 0.0, 5.0, null),
				"127.0.0.1"))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

}
