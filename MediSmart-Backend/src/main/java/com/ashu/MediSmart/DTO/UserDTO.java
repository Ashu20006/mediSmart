package com.ashu.MediSmart.DTO;

import java.util.List;

public class UserDTO {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String email;
    private String password;
    private String confirmPassword; //  added
    private String role;

    // Common fields
    private String phoneNumber;
    private String gender;

    // Doctor-specific fields
    private String specialty;
    private String location;
    private int yearsOfExperience;
    private double rating;
    private List<String> availability;
    private String qualification;
    private String bio;

    // Patient-specific field
    private Integer age;

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public List<String> getAvailability() { return availability; }
    public void setAvailability(List<String> availability) { this.availability = availability; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    // Constructor for doctor recommendation response
    public UserDTO(String id, String name, String specialty, String location,
                   int yearsOfExperience, double rating, List<String> availability) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.location = location;
        this.yearsOfExperience = yearsOfExperience;
        this.rating = rating;
        this.availability = availability;
    }

    public UserDTO() {}
}
