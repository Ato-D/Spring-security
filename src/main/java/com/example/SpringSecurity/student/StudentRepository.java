package com.example.SpringSecurity.student;

import com.example.SpringSecurity.dto.ResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

}
