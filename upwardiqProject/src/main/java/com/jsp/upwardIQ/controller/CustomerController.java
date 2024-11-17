package com.jsp.upwardIQ.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsp.upwardIQ.entity.Customer;
import com.jsp.upwardIQ.service.CustomerService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerController {

	@Autowired
	private CustomerService customerService;
	// Temporary storage for loaded data
	private List<Customer> temporaryCustomers = new ArrayList<Customer>();

	@GetMapping("/")
	public String home() {
		return "home";
	}

	@GetMapping("/upload")
	public String showUploadPage(Model model) {
		model.addAttribute("customers", temporaryCustomers); // Pre-load any temp data if needed
		return "upload"; // Thymeleaf template
	}

	@PostMapping("/load")
	public String loadData(@RequestParam("data") String data, Model model) {
		try {
			// Parse JSON string to List<Customer>
			ObjectMapper objectMapper = new ObjectMapper();
			temporaryCustomers = objectMapper.readValue(data, new TypeReference<List<Customer>>() {
			});
			model.addAttribute("message", "Data loaded successfully! Review below.");
		} catch (Exception e) {
			model.addAttribute("message", "Error loading data: " + e.getMessage());
		}

		// Display loaded data
		model.addAttribute("customers", temporaryCustomers);
		return "upload";
	}

	@PostMapping("/save")
	public String saveData(Model model) {
		try {
			// Save all temporary data to the database
			customerService.saveAllCustomers(temporaryCustomers);
			model.addAttribute("message", "Data saved successfully!");
			temporaryCustomers.clear(); // Clear temporary storage after saving
		} catch (Exception e) {
			model.addAttribute("message", "Error saving data: " + e.getMessage());
		}

		// Refresh the page
		model.addAttribute("customers", temporaryCustomers);
		return "upload";
	}

	@GetMapping("/search")
	public String searchPage() {
		return "search"; // Return the search page
	}

	@PostMapping("/search")
	public String searchCustomers(@RequestParam("name") String name, @RequestParam("gender") String gender, Model model,
			HttpSession session) {
		List<Customer> customers = customerService.searchCustomers(name, gender);
		model.addAttribute("customers", customers);

		// Store filtered customers in session so it can be used later for exporting
		session.setAttribute("filteredCustomers", customers);
		return "search";
	}

	// Export filtered data to Excel
	@GetMapping("/downloadExcel")
	public ResponseEntity<byte[]> downloadExcel(HttpSession session) throws IOException {
		// Retrieve filtered customer data from session
		List<Customer> customers = (List<Customer>) session.getAttribute("filteredCustomers");

		if (customers == null || customers.isEmpty()) {
			return ResponseEntity.badRequest().build(); // No data to export
		}

		// Create an Excel workbook
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Customers");

		// Create header row
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("ID");
		headerRow.createCell(1).setCellValue("Name");
		headerRow.createCell(2).setCellValue("Gender");
		headerRow.createCell(3).setCellValue("Address");

		// Add filtered customer data to rows
		int rowNum = 1;
		for (Customer customer : customers) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(customer.getId());
			row.createCell(1).setCellValue(customer.getName());
			row.createCell(2).setCellValue(customer.getGender());
			row.createCell(3).setCellValue(customer.getAddress());
		}

		// Write the data to a byte array output stream
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		workbook.write(byteArrayOutputStream);
		workbook.close();

		// Set response headers for download
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=filtered_customers.xlsx");

		return ResponseEntity.ok().headers(headers).body(byteArrayOutputStream.toByteArray());
	}
}
