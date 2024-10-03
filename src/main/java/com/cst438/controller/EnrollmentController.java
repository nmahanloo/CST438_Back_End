package com.cst438.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.EnrollmentDTO;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class EnrollmentController {

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    SectionRepository sectionRepository;

    @GetMapping("/sections/{sectionNo}/enrollments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo, Principal principal) {

        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found " + sectionNo);
        }
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to view enrollments for this section");
        }

        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
        if (enrollments.size() < 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nobody has enrolled in section " + sectionNo);
        } else {
            List<EnrollmentDTO> dto_list = new ArrayList<>();
            for (Enrollment e : enrollments) {
                dto_list.add(new EnrollmentDTO(e.getEnrollmentId(),
                        e.getGrade(),
                        e.getStudent().getId(),
                        e.getStudent().getName(),
                        e.getStudent().getEmail(),
                        e.getSection().getCourse().getCourseId(),
                        e.getSection().getCourse().getTitle(),
                        e.getSection().getSecId(),
                        e.getSection().getSectionNo(),
                        e.getSection().getBuilding(),
                        e.getSection().getRoom(),
                        e.getSection().getTimes(),
                        e.getSection().getCourse().getCredits(),
                        e.getSection().getTerm().getYear(),
                        e.getSection().getTerm().getSemester()
                ));
            }
            return dto_list;
        }
    }

    @PutMapping("/enrollments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateEnrollmentGrade(@RequestBody List<EnrollmentDTO> dlist, Principal principal) {
        if (dlist.size() < 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Please provide the enrollments");
        } else {
            for (EnrollmentDTO enrollment : dlist) {
                Enrollment e = enrollmentRepository.findById(enrollment.enrollmentId()).orElse(null);
                if (e == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found " + enrollment.enrollmentId());
                }

                Section section = e.getSection();
                if (!section.getInstructorEmail().equals(principal.getName())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to update grades for this section");
                }

                e.setGrade(enrollment.grade());
                enrollmentRepository.save(e);
            }
        }
    }
}
