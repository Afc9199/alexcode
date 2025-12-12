package com.example.employeemanagement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.AttendanceReportResponse;
import com.example.employeemanagement.dto.BenefitsReportResponse;
import com.example.employeemanagement.dto.KPIPerformanceReportResponse;
import com.example.employeemanagement.dto.PayrollReportResponse;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class ReportExportService {

	public byte[] exportAttendanceToExcel(AttendanceReportResponse report) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Attendance Report");

		// Create header style
		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 12);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		// Create data style
		CellStyle dataStyle = workbook.createCellStyle();
		dataStyle.setBorderBottom(BorderStyle.THIN);
		dataStyle.setBorderTop(BorderStyle.THIN);
		dataStyle.setBorderLeft(BorderStyle.THIN);
		dataStyle.setBorderRight(BorderStyle.THIN);

		int rowNum = 0;

		// Title row
		Row titleRow = sheet.createRow(rowNum++);
		org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("Attendance Report");
		titleCell.setCellStyle(headerStyle);
		sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 11));

		// Report info
		Row infoRow1 = sheet.createRow(rowNum++);
		infoRow1.createCell(0).setCellValue("Report Type:");
		infoRow1.createCell(1).setCellValue(report.reportType());
		infoRow1.createCell(3).setCellValue("Department:");
		infoRow1.createCell(4).setCellValue(report.department());
		infoRow1.createCell(6).setCellValue("Date Generated:");
		infoRow1.createCell(7).setCellValue(report.dateGenerated());

		Row infoRow2 = sheet.createRow(rowNum++);
		infoRow2.createCell(0).setCellValue("Start Date:");
		infoRow2.createCell(1).setCellValue(report.startDate());
		infoRow2.createCell(3).setCellValue("End Date:");
		infoRow2.createCell(4).setCellValue(report.endDate());

		Row infoRow3 = sheet.createRow(rowNum++);
		infoRow3.createCell(0).setCellValue("Total Records:");
		infoRow3.createCell(1).setCellValue(report.totalRecords());
		infoRow3.createCell(3).setCellValue("Total Work Hours:");
		infoRow3.createCell(4).setCellValue(String.format("%.2f", report.totalWorkHours()));
		infoRow3.createCell(6).setCellValue("Total Overtime Hours:");
		infoRow3.createCell(7).setCellValue(String.format("%.2f", report.totalOvertimeHours()));

		rowNum++; // Empty row

		// Header row
		Row headerRow = sheet.createRow(rowNum++);
		String[] headers = { "Employee ID", "Employee Name", "Department", "Job Title", "Work Date", "Check In", "Check Out", "Status", "Work Hours", "Overtime Hours", "Notes" };
		for (int i = 0; i < headers.length; i++) {
			org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		// Data rows
		for (AttendanceReportResponse.AttendanceReportItem item : report.items()) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(item.employeeId() != null ? item.employeeId() : "");
			row.createCell(1).setCellValue(item.employeeName() != null ? item.employeeName() : "");
			row.createCell(2).setCellValue(item.department() != null ? item.department() : "");
			row.createCell(3).setCellValue(item.jobTitle() != null ? item.jobTitle() : "");
			row.createCell(4).setCellValue(item.workDate() != null ? item.workDate() : "");
			row.createCell(5).setCellValue(item.checkIn() != null ? item.checkIn() : "");
			row.createCell(6).setCellValue(item.checkOut() != null ? item.checkOut() : "");
			row.createCell(7).setCellValue(item.status() != null ? item.status() : "");
			row.createCell(8).setCellValue(item.workHours() != null ? item.workHours() : 0.0);
			row.createCell(9).setCellValue(item.overtimeHours() != null ? item.overtimeHours() : 0.0);
			row.createCell(10).setCellValue(item.notes() != null ? item.notes() : "");

			for (int i = 0; i < headers.length; i++) {
				row.getCell(i).setCellStyle(dataStyle);
			}
		}

		// Auto-size columns
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();
		return outputStream.toByteArray();
	}

	public byte[] exportAttendanceToPDF(AttendanceReportResponse report) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		// Title
		document.add(new Paragraph("Attendance Report").setFontSize(18).setBold().setMarginBottom(10));

		// Report info
		document.add(new Paragraph("Report Type: " + report.reportType()).setMarginBottom(5));
		document.add(new Paragraph("Period: " + report.startDate() + " to " + report.endDate()).setMarginBottom(5));
		document.add(new Paragraph("Department: " + report.department()).setMarginBottom(5));
		document.add(new Paragraph("Date Generated: " + report.dateGenerated()).setMarginBottom(5));
		document.add(new Paragraph("Total Records: " + report.totalRecords()).setMarginBottom(5));
		document.add(new Paragraph("Total Work Hours: " + String.format("%.2f", report.totalWorkHours())).setMarginBottom(5));
		document.add(new Paragraph("Total Overtime Hours: " + String.format("%.2f", report.totalOvertimeHours())).setMarginBottom(15));

		// Table
		Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 1.5f, 1f, 1f, 1f, 1f, 1f, 0.8f, 0.8f, 0.8f, 1.5f})).useAllAvailableWidth();
		
		// Header
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee ID").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Department").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Job Title").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Work Date").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Check In").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Check Out").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Status").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Work Hours").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("OT Hours").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Notes").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));

		// Data rows
		for (AttendanceReportResponse.AttendanceReportItem item : report.items()) {
			table.addCell(item.employeeId() != null ? item.employeeId() : "");
			table.addCell(item.employeeName() != null ? item.employeeName() : "");
			table.addCell(item.department() != null ? item.department() : "");
			table.addCell(item.jobTitle() != null ? item.jobTitle() : "");
			table.addCell(item.workDate() != null ? item.workDate() : "");
			table.addCell(item.checkIn() != null ? item.checkIn() : "");
			table.addCell(item.checkOut() != null ? item.checkOut() : "");
			table.addCell(item.status() != null ? item.status() : "");
			table.addCell(item.workHours() != null ? String.format("%.2f", item.workHours()) : "0.00");
			table.addCell(item.overtimeHours() != null ? String.format("%.2f", item.overtimeHours()) : "0.00");
			table.addCell(item.notes() != null ? item.notes() : "");
		}

		document.add(table);
		document.close();
		return baos.toByteArray();
	}

	public byte[] exportPayrollToExcel(PayrollReportResponse report) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Payroll Report");

		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 12);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		CellStyle dataStyle = workbook.createCellStyle();
		dataStyle.setBorderBottom(BorderStyle.THIN);
		dataStyle.setBorderTop(BorderStyle.THIN);
		dataStyle.setBorderLeft(BorderStyle.THIN);
		dataStyle.setBorderRight(BorderStyle.THIN);

		CellStyle currencyStyle = workbook.createCellStyle();
		currencyStyle.cloneStyleFrom(dataStyle);
		DataFormat currencyFormat = workbook.createDataFormat();
		currencyStyle.setDataFormat(currencyFormat.getFormat("RM#,##0.00"));

		int rowNum = 0;

		Row titleRow = sheet.createRow(rowNum++);
		org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("Payroll Report");
		titleCell.setCellStyle(headerStyle);
		sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

		Row infoRow1 = sheet.createRow(rowNum++);
		infoRow1.createCell(0).setCellValue("Report Type:");
		infoRow1.createCell(1).setCellValue(report.reportType());
		infoRow1.createCell(3).setCellValue("Month:");
		infoRow1.createCell(4).setCellValue(report.month());

		Row infoRow2 = sheet.createRow(rowNum++);
		infoRow2.createCell(0).setCellValue("Total Employees:");
		infoRow2.createCell(1).setCellValue(report.totalEmployees());

		rowNum++;

		Row headerRow = sheet.createRow(rowNum++);
		String[] headers = { "Employee ID", "Employee Name", "Department", "Basic Salary", "Gross Pay", "Net Pay" };
		for (int i = 0; i < headers.length; i++) {
			org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		for (PayrollReportResponse.PayrollReportItem item : report.items()) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(item.employeeId() != null ? item.employeeId() : "");
			row.createCell(1).setCellValue(item.employeeName() != null ? item.employeeName() : "");
			row.createCell(2).setCellValue(item.department() != null ? item.department() : "");
			
			org.apache.poi.ss.usermodel.Cell basicSalaryCell = row.createCell(3);
			basicSalaryCell.setCellValue(item.basicSalary() != null ? item.basicSalary().doubleValue() : 0.0);
			basicSalaryCell.setCellStyle(currencyStyle);
			
			org.apache.poi.ss.usermodel.Cell grossPayCell = row.createCell(4);
			grossPayCell.setCellValue(item.grossPay() != null ? item.grossPay().doubleValue() : 0.0);
			grossPayCell.setCellStyle(currencyStyle);
			
			org.apache.poi.ss.usermodel.Cell netPayCell = row.createCell(5);
			netPayCell.setCellValue(item.netPay() != null ? item.netPay().doubleValue() : 0.0);
			netPayCell.setCellStyle(currencyStyle);

			for (int i = 0; i < 3; i++) {
				row.getCell(i).setCellStyle(dataStyle);
			}
		}

		// Statutory summary if included
		if (report.includeStatutory() && report.statutorySummary() != null) {
			rowNum += 2;
			Row summaryTitleRow = sheet.createRow(rowNum++);
			org.apache.poi.ss.usermodel.Cell summaryTitleCell = summaryTitleRow.createCell(0);
			summaryTitleCell.setCellValue("Statutory Contributions Summary");
			summaryTitleCell.setCellStyle(headerStyle);
			sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

			PayrollReportResponse.StatutorySummary s = report.statutorySummary();
			String[] summaryLabels = {
				"Total EPF (Employee)", "Total EPF (Employer)",
				"Total SOCSO (Employee)", "Total SOCSO (Employer)",
				"Total EIS (Employee)", "Total EIS (Employer)",
				"Total PCB"
			};
			BigDecimal[] summaryValues = {
				s.totalEpfEmployee(), s.totalEpfEmployer(),
				s.totalSocsoEmployee(), s.totalSocsoEmployer(),
				s.totalEisEmployee(), s.totalEisEmployer(),
				s.totalPcb()
			};

			for (int i = 0; i < summaryLabels.length; i++) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(summaryLabels[i]);
				org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
				valueCell.setCellValue(summaryValues[i] != null ? summaryValues[i].doubleValue() : 0.0);
				valueCell.setCellStyle(currencyStyle);
			}
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();
		return outputStream.toByteArray();
	}

	public byte[] exportPayrollToPDF(PayrollReportResponse report) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		document.add(new Paragraph("Payroll Report").setFontSize(18).setBold().setMarginBottom(10));
		document.add(new Paragraph("Report Type: " + report.reportType()).setMarginBottom(5));
		document.add(new Paragraph("Month: " + report.month()).setMarginBottom(5));
		document.add(new Paragraph("Total Employees: " + report.totalEmployees()).setMarginBottom(15));

		Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 2f, 1.5f, 1.5f, 1.5f, 1.5f})).useAllAvailableWidth();
		
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee ID").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Department").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Basic Salary").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Gross Pay").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Net Pay").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));

		for (PayrollReportResponse.PayrollReportItem item : report.items()) {
			table.addCell(item.employeeId() != null ? item.employeeId() : "");
			table.addCell(item.employeeName() != null ? item.employeeName() : "");
			table.addCell(item.department() != null ? item.department() : "");
			table.addCell("RM " + (item.basicSalary() != null ? String.format("%.2f", item.basicSalary()) : "0.00"));
			table.addCell("RM " + (item.grossPay() != null ? String.format("%.2f", item.grossPay()) : "0.00"));
			table.addCell("RM " + (item.netPay() != null ? String.format("%.2f", item.netPay()) : "0.00"));
		}

		document.add(table);

		if (report.includeStatutory() && report.statutorySummary() != null) {
			document.add(new Paragraph("\nStatutory Contributions Summary").setFontSize(14).setBold().setMarginTop(20));
			PayrollReportResponse.StatutorySummary s = report.statutorySummary();
			document.add(new Paragraph("Total EPF (Employee): RM " + String.format("%.2f", s.totalEpfEmployee())));
			document.add(new Paragraph("Total EPF (Employer): RM " + String.format("%.2f", s.totalEpfEmployer())));
			document.add(new Paragraph("Total SOCSO (Employee): RM " + String.format("%.2f", s.totalSocsoEmployee())));
			document.add(new Paragraph("Total SOCSO (Employer): RM " + String.format("%.2f", s.totalSocsoEmployer())));
			document.add(new Paragraph("Total EIS (Employee): RM " + String.format("%.2f", s.totalEisEmployee())));
			document.add(new Paragraph("Total EIS (Employer): RM " + String.format("%.2f", s.totalEisEmployer())));
			document.add(new Paragraph("Total PCB: RM " + String.format("%.2f", s.totalPcb())));
		}

		document.close();
		return baos.toByteArray();
	}

	public byte[] exportBenefitsToExcel(BenefitsReportResponse report) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Benefits Report");

		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 12);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		CellStyle dataStyle = workbook.createCellStyle();
		dataStyle.setBorderBottom(BorderStyle.THIN);
		dataStyle.setBorderTop(BorderStyle.THIN);
		dataStyle.setBorderLeft(BorderStyle.THIN);
		dataStyle.setBorderRight(BorderStyle.THIN);

		int rowNum = 0;

		Row titleRow = sheet.createRow(rowNum++);
		org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("Benefits Report");
		titleCell.setCellStyle(headerStyle);
		sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

		Row infoRow = sheet.createRow(rowNum++);
		infoRow.createCell(0).setCellValue("Total Records:");
		infoRow.createCell(1).setCellValue(report.totalRecords());

		rowNum++;

		Row headerRow = sheet.createRow(rowNum++);
		String[] headers = { "Employee ID", "Employee Name", "Department", "Benefit Name", "Category", "Status" };
		for (int i = 0; i < headers.length; i++) {
			org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		for (BenefitsReportResponse.BenefitsReportItem item : report.items()) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(item.employeeId() != null ? item.employeeId() : "");
			row.createCell(1).setCellValue(item.employeeName() != null ? item.employeeName() : "");
			row.createCell(2).setCellValue(item.department() != null ? item.department() : "");
			row.createCell(3).setCellValue(item.benefitName() != null ? item.benefitName() : "");
			row.createCell(4).setCellValue(item.benefitCategory() != null ? item.benefitCategory() : "");
			row.createCell(5).setCellValue(item.status() != null ? item.status() : "");

			for (int i = 0; i < headers.length; i++) {
				row.getCell(i).setCellStyle(dataStyle);
			}
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();
		return outputStream.toByteArray();
	}

	public byte[] exportBenefitsToPDF(BenefitsReportResponse report) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		document.add(new Paragraph("Benefits Report").setFontSize(18).setBold().setMarginBottom(10));
		document.add(new Paragraph("Total Records: " + report.totalRecords()).setMarginBottom(15));

		Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 2f, 1.5f, 2f, 1.5f, 1f})).useAllAvailableWidth();
		
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee ID").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Department").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Benefit Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Category").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Status").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));

		for (BenefitsReportResponse.BenefitsReportItem item : report.items()) {
			table.addCell(item.employeeId() != null ? item.employeeId() : "");
			table.addCell(item.employeeName() != null ? item.employeeName() : "");
			table.addCell(item.department() != null ? item.department() : "");
			table.addCell(item.benefitName() != null ? item.benefitName() : "");
			table.addCell(item.benefitCategory() != null ? item.benefitCategory() : "");
			table.addCell(item.status() != null ? item.status() : "");
		}

		document.add(table);
		document.close();
		return baos.toByteArray();
	}

	public byte[] exportKPIPerformanceToExcel(KPIPerformanceReportResponse report) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("KPI Performance Report");

		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 12);
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		CellStyle dataStyle = workbook.createCellStyle();
		dataStyle.setBorderBottom(BorderStyle.THIN);
		dataStyle.setBorderTop(BorderStyle.THIN);
		dataStyle.setBorderLeft(BorderStyle.THIN);
		dataStyle.setBorderRight(BorderStyle.THIN);

		CellStyle currencyStyle = workbook.createCellStyle();
		currencyStyle.cloneStyleFrom(dataStyle);
		DataFormat currencyFormat = workbook.createDataFormat();
		currencyStyle.setDataFormat(currencyFormat.getFormat("RM#,##0.00"));

		int rowNum = 0;

		Row titleRow = sheet.createRow(rowNum++);
		org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("KPI Performance Report");
		titleCell.setCellStyle(headerStyle);
		sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

		Row infoRow = sheet.createRow(rowNum++);
		infoRow.createCell(0).setCellValue("Year:");
		infoRow.createCell(1).setCellValue(report.year());
		infoRow.createCell(3).setCellValue("Total Records:");
		infoRow.createCell(4).setCellValue(report.totalRecords());

		rowNum++;

		Row headerRow = sheet.createRow(rowNum++);
		String[] headers = { "Employee ID", "Employee Name", "Department", "KPI Name", "Target", "Actual", "Achievement %", "Status", "Bonus" };
		for (int i = 0; i < headers.length; i++) {
			org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		for (KPIPerformanceReportResponse.KPIPerformanceReportItem item : report.items()) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(item.employeeId() != null ? item.employeeId() : "");
			row.createCell(1).setCellValue(item.employeeName() != null ? item.employeeName() : "");
			row.createCell(2).setCellValue(item.department() != null ? item.department() : "");
			row.createCell(3).setCellValue(item.kpiName() != null ? item.kpiName() : "");
			row.createCell(4).setCellValue(item.targetValue() != null ? item.targetValue().doubleValue() : 0.0);
			row.createCell(5).setCellValue(item.actualValue() != null ? item.actualValue().doubleValue() : 0.0);
			row.createCell(6).setCellValue(item.achievementPercentage() != null ? item.achievementPercentage().doubleValue() : 0.0);
			row.createCell(7).setCellValue(item.status() != null ? item.status() : "");
			
			org.apache.poi.ss.usermodel.Cell bonusCell = row.createCell(8);
			bonusCell.setCellValue(item.bonusAmount() != null ? item.bonusAmount().doubleValue() : 0.0);
			bonusCell.setCellStyle(currencyStyle);

			for (int i = 0; i < 8; i++) {
				row.getCell(i).setCellStyle(dataStyle);
			}
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();
		return outputStream.toByteArray();
	}

	public byte[] exportKPIPerformanceToPDF(KPIPerformanceReportResponse report) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		document.add(new Paragraph("KPI Performance Report").setFontSize(18).setBold().setMarginBottom(10));
		document.add(new Paragraph("Year: " + report.year()).setMarginBottom(5));
		document.add(new Paragraph("Total Records: " + report.totalRecords()).setMarginBottom(15));

		Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 2f, 1.5f, 2f, 1f, 1f, 1f, 1f, 1f})).useAllAvailableWidth();
		
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee ID").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Employee Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Department").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("KPI Name").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Target").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Actual").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Achievement %").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Status").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
		table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Bonus").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));

		for (KPIPerformanceReportResponse.KPIPerformanceReportItem item : report.items()) {
			table.addCell(item.employeeId() != null ? item.employeeId() : "");
			table.addCell(item.employeeName() != null ? item.employeeName() : "");
			table.addCell(item.department() != null ? item.department() : "");
			table.addCell(item.kpiName() != null ? item.kpiName() : "");
			table.addCell(item.targetValue() != null ? String.format("%.2f", item.targetValue()) : "0.00");
			table.addCell(item.actualValue() != null ? String.format("%.2f", item.actualValue()) : "0.00");
			table.addCell(item.achievementPercentage() != null ? String.format("%.2f%%", item.achievementPercentage()) : "0.00%");
			table.addCell(item.status() != null ? item.status() : "");
			table.addCell("RM " + (item.bonusAmount() != null ? String.format("%.2f", item.bonusAmount()) : "0.00"));
		}

		document.add(table);
		document.close();
		return baos.toByteArray();
	}
}

