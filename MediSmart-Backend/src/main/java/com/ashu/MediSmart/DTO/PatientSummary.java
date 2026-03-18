package com.ashu.MediSmart.DTO;

import java.time.LocalDate;

public class PatientSummary {
    private String id;
    private String name;
    private Integer age;
    private String gender;
    private String phoneNumber;
    private String email;
    private String location;
    private LocalDate lastVisit;

    public PatientSummary() {}

    public PatientSummary(String id, String name, Integer age, String gender,
                          String phoneNumber, String email, String location, LocalDate lastVisit) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.location = location;
        this.lastVisit = lastVisit;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDate lastVisit) { this.lastVisit = lastVisit; }
}
