package com.ashu.MediSmart.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Field(name = "name")
    private String name;

    @Field(name = "email")
    private String email;

    @Field(name = "password")
    private String password;

    // Common fields
    private String phoneNumber;
    private String gender;

    // Role as String (PATIENT, DOCTOR, ADMIN)
    private String role;

    // Doctor-specific fields
    private String specialty;
    private String location;
    private int yearsOfExperience;
    private double rating;
    private List<Availability> availability;
    private String qualification;   //  new
    private String bio;             //  new

    // Patient-specific field
    private Integer age;

    // Constructors
    public User() {}

    public User(String id, String name, String email, String password, String phoneNumber, String gender,
                String role, String specialty, String location, int yearsOfExperience,
                double rating, List<Availability> availability, String qualification, String bio, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.role = role;
        this.specialty = specialty;
        this.location = location;
        this.yearsOfExperience = yearsOfExperience;
        this.rating = rating;
        this.availability = availability;
        this.qualification = qualification;
        this.bio = bio;
        this.age = age;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public List<Availability> getAvailability() { return availability; }
    public void setAvailability(List<Availability> availability) { this.availability = availability; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public static class Availability {
        private String day;

        public Availability() {
        }

        public Availability(String day) {
            this.day = day;
        }

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }
    }
}
