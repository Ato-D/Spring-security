package com.example.SpringSecurity.student;

import com.example.SpringSecurity.dto.ResponseDTO;
import com.example.SpringSecurity.student.dto.StudentDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StudentService {

//    List<StudentDto> findAll();

    ResponseEntity<ResponseDTO> findAllStudents(Map<String, String> params);

    ResponseEntity<ResponseDTO> findById(UUID id);

//    Student findById(UUID id);

    void saveStudent(StudentDto studentDto);

    Student updateStudent(StudentDto studentDto);

    void deleteById(UUID id);
}
