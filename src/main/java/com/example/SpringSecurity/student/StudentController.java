package com.example.SpringSecurity.student;


import com.example.SpringSecurity.dto.ResponseDTO;
import com.example.SpringSecurity.dto.StudentDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.example.SpringSecurity.security.SecurityConfig.CONTEXT_PATH;

@RestController
@RequestMapping(CONTEXT_PATH)
@AllArgsConstructor
public class StudentController {

    private final StudentService studentService;


    @GetMapping("/findAll")
    public ResponseEntity<ResponseDTO> findAll(@RequestParam  Map<String, String> params) {
        return studentService.findAllStudents(params);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> findById(@PathVariable(name = "id") UUID id) {
        var res = studentService.findById(id);
        return res;
    }


    @PostMapping("/createStudent")
    public ResponseEntity<ResponseDTO> save(@RequestBody StudentDto studentDto) {
        return studentService.saveStudent(studentDto);
    }

    @PutMapping("updateStudent/{id}")
    public ResponseEntity<ResponseDTO> update(@PathVariable(name = "id") UUID id,
                                              @RequestBody StudentDto studentDto) {
        studentDto.setId(id);
        return studentService.updateStudent(id, studentDto);
    }

    @DeleteMapping("/deleteStudent/{id}")
    public ResponseEntity<ResponseDTO> delete(@PathVariable(name = "id") UUID id) {
      return studentService.deleteStudent(id);
    }
}
