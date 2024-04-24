package com.example.SpringSecurity.student;

import com.example.SpringSecurity.student.dto.StudentDto;

import java.util.List;
import java.util.UUID;

public interface StudentService {

    List<StudentDto> findAll();

    Student findById(UUID id);

    void saveStudent(StudentDto studentDto);

    Student updateStudent(StudentDto studentDto);

    void deleteById(UUID id);
}
