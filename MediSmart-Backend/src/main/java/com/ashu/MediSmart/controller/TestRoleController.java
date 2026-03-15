package com.ashu.MediSmart.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRoleController {

    @GetMapping("/admin/test")
    public String adminOnly() {
        return "Hello Admin!";
    }

    @GetMapping("/doctor/test")
    public String doctorOnly() {
        return "Hello Doctor!";
    }

    @GetMapping("/patient/test")
    public String patientOnly() {
        return "Hello Patient!";
    }
}
