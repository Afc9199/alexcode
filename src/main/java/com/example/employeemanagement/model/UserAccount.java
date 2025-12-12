package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class UserAccount {

	@Id
	private String id;

	@Indexed(unique = true)
	private String username;

	private String employeeId;

	private String passwordHash;

	private Role role = Role.EMPLOYEE;

	private boolean active = true;

	private Instant createdAt = Instant.now();

	private String fullName = "";

	private String email; // null for empty to avoid unique index conflicts

	private String department = "";

	private String jobTitle = "";

	private Double basicSalary = 0.0;

	private String contactNumber; // null for empty to avoid unique index conflicts

	private String gender = "";

	private Integer age;

	private String race = "";

	private String religion = "";

	private String address = "";

	private String maritalStatus = "";

	private String bankName = "";

	private String bankAccountNumber; // null for empty to avoid unique index conflicts

	private String epfNumber; // null for empty to avoid unique index conflicts

	private String icNumber; // null for empty to avoid unique index conflicts

	private String passportNumber; // null for empty to avoid unique index conflicts

	private String taxNumber; // null for empty to avoid unique index conflicts

	private Integer numberOfChildren = 0;

	private String nationality = "";

	private String residentStatus = "";

	private String spouseWorking = "";

	// Compliance & Key Identity Information
	private LocalDate dateOfBirth;
	private String socsoNumber; // null for empty to avoid unique index conflicts

	// Position & Employment Information
	private LocalDate dateOfHire;
	private Integer probationPeriodLength;
	private String employmentType = "";
	private String reportingManagerId; // Store as String (userId)
	private String location = "";

	// Emergency Contact
	private String emergencyContactName = "";
	private String emergencyContactRelationship = "";
	private String emergencyContactNumber; // null for empty to avoid unique index conflicts

	public UserAccount() {
	}

	public UserAccount(String id, String username, String employeeId, String passwordHash, Role role, boolean active, Instant createdAt,
			String fullName, String email, String department, String jobTitle, Double basicSalary, String contactNumber, String gender, Integer age,
			String race, String religion, String address, String maritalStatus, String bankName, String bankAccountNumber, String epfNumber, String icNumber, String taxNumber, Integer numberOfChildren) {
		this.id = id;
		this.username = username;
		this.employeeId = employeeId;
		this.passwordHash = passwordHash;
		this.role = role;
		this.active = active;
		this.createdAt = createdAt;
		this.fullName = fullName;
		this.email = email;
		this.department = department;
		this.jobTitle = jobTitle;
		this.basicSalary = basicSalary;
		this.contactNumber = contactNumber;
		this.gender = gender;
		this.age = age;
		this.race = race;
		this.religion = religion;
		this.address = address;
		this.maritalStatus = maritalStatus;
		this.bankName = bankName;
		this.bankAccountNumber = bankAccountNumber;
		this.epfNumber = epfNumber;
		this.icNumber = icNumber;
		this.taxNumber = taxNumber;
		this.numberOfChildren = numberOfChildren;
	}

	public static UserAccount of(String username, String passwordHash, Role role) {
		return new UserAccount(null, username, null, passwordHash, role, true, Instant.now(), "", "", "", "", 0.0, "", "", null, "", "", "", "", "", "", "", "", "", 0);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public Double getBasicSalary() {
		return basicSalary;
	}

	public void setBasicSalary(Double basicSalary) {
		this.basicSalary = basicSalary;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getRace() {
		return race;
	}

	public void setRace(String race) {
		this.race = race;
	}

	public String getReligion() {
		return religion;
	}

	public void setReligion(String religion) {
		this.religion = religion;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getBankAccountNumber() {
		return bankAccountNumber;
	}

	public void setBankAccountNumber(String bankAccountNumber) {
		this.bankAccountNumber = bankAccountNumber;
	}

	public String getEpfNumber() {
		return epfNumber;
	}

	public void setEpfNumber(String epfNumber) {
		this.epfNumber = epfNumber;
	}

	public String getIcNumber() {
		return icNumber;
	}

	public void setIcNumber(String icNumber) {
		this.icNumber = icNumber;
	}

	public String getTaxNumber() {
		return taxNumber;
	}

	public void setTaxNumber(String taxNumber) {
		this.taxNumber = taxNumber;
	}

	public Integer getNumberOfChildren() {
		return numberOfChildren;
	}

	public void setNumberOfChildren(Integer numberOfChildren) {
		this.numberOfChildren = numberOfChildren;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getResidentStatus() {
		return residentStatus;
	}

	public void setResidentStatus(String residentStatus) {
		this.residentStatus = residentStatus;
	}

	public String getSpouseWorking() {
		return spouseWorking;
	}

	public void setSpouseWorking(String spouseWorking) {
		this.spouseWorking = spouseWorking;
	}

	public String getPassportNumber() {
		return passportNumber;
	}

	public void setPassportNumber(String passportNumber) {
		this.passportNumber = passportNumber;
	}

	// Compliance & Key Identity Information
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getSocsoNumber() {
		return socsoNumber;
	}

	public void setSocsoNumber(String socsoNumber) {
		this.socsoNumber = socsoNumber;
	}

	// Position & Employment Information
	public LocalDate getDateOfHire() {
		return dateOfHire;
	}

	public void setDateOfHire(LocalDate dateOfHire) {
		this.dateOfHire = dateOfHire;
	}

	public Integer getProbationPeriodLength() {
		return probationPeriodLength;
	}

	public void setProbationPeriodLength(Integer probationPeriodLength) {
		this.probationPeriodLength = probationPeriodLength;
	}

	public String getEmploymentType() {
		return employmentType;
	}

	public void setEmploymentType(String employmentType) {
		this.employmentType = employmentType;
	}

	public String getReportingManagerId() {
		return reportingManagerId;
	}

	public void setReportingManagerId(String reportingManagerId) {
		this.reportingManagerId = reportingManagerId;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	// Emergency Contact
	public String getEmergencyContactName() {
		return emergencyContactName;
	}

	public void setEmergencyContactName(String emergencyContactName) {
		this.emergencyContactName = emergencyContactName;
	}

	public String getEmergencyContactRelationship() {
		return emergencyContactRelationship;
	}

	public void setEmergencyContactRelationship(String emergencyContactRelationship) {
		this.emergencyContactRelationship = emergencyContactRelationship;
	}

	public String getEmergencyContactNumber() {
		return emergencyContactNumber;
	}

	public void setEmergencyContactNumber(String emergencyContactNumber) {
		this.emergencyContactNumber = emergencyContactNumber;
	}
}

