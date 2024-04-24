package com.example.SpringSecurity.student;


import com.example.SpringSecurity.student.dto.StudentDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.example.SpringSecurity.security.SecurityConfig.CONTEXT_PATH;

@RestController
@RequestMapping(CONTEXT_PATH)
@AllArgsConstructor
public class StudentController {

    private final StudentService studentService;


    @GetMapping()
    public List<StudentDto> findAll() {
        var res = studentService.findAll();
        return res;

    }

    @GetMapping("/{id}")
    public Student student(@PathVariable(name = "id") UUID id) {
        var res = studentService.findById(id);
        return res;
    }



    @PostMapping
    public void save(@RequestBody StudentDto studentDto) {
        studentService.saveStudent(studentDto);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable(name = "id") UUID id,
                          @RequestBody StudentDto studentDto) {

        studentDto.setId(id);
        return studentService.updateStudent(studentDto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable(name = "id") UUID id) {
      studentService.deleteById(id);
    }
}