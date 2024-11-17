package com.jsp.upwardIQ.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jsp.upwardIQ.entity.Customer;
import com.jsp.upwardIQ.repo.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public List<Customer> searchCustomers(String name, String gender) {
        return customerRepository.findByNameContainingAndGenderContaining(name, gender);
    }

    public void saveAllCustomers(List<Customer> customers) {
        customerRepository.saveAll(customers);
    }
}

