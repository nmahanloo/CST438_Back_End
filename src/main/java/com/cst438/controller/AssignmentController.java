package com.cst438.controller;

import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Grade;
import com.cst438.domain.GradeRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.GradeDTO;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class AssignmentController {

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(@PathVariable("secNo") int secNo, Principal principal) {
        List<Assignment> assignments = assignmentRepository.findBySectionNoOrderByDueDate(secNo);
        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found or no assignments for section");
        }
        List<AssignmentDTO> assignmentDTOs = new ArrayList<>();
        for (Assignment a : assignments) {
            if (!a.getSection().getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to view assignments for this section");
            }
            AssignmentDTO dto = new AssignmentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate().toString(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSecId(),
                    a.getSection().getSectionNo()
            );
            assignmentDTOs.add(dto);
        }
        return assignmentDTOs;
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(@RequestBody AssignmentDTO dto, Principal principal) {
        String principalEmail = principal.getName();
        System.out.println("Instructor email from Principal: " + principalEmail);

        Section section = sectionRepository.findById(dto.secNo()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not found"));

        System.out.println("Section Instructor Email: " + section.getInstructorEmail());

        if (!section.getInstructorEmail().equals(principalEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to add assignment to this section");
        }

        Date dueDate = Date.valueOf(dto.dueDate());
        if (dueDate.before(section.getTerm().getStartDate()) || dueDate.after(section.getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date is outside the course dates");
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDueDate(dueDate);
        assignment.setSection(section);

        assignment = assignmentRepository.save(assignment);

        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSecId(),
                assignment.getSection().getSectionNo()
        );
    }

    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@RequestBody AssignmentDTO dto, Principal principal) {
        Assignment assignment = assignmentRepository.findById(dto.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment not found"));

        Section section = assignment.getSection();
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to update assignment in this section");
        }

        Date dueDate = Date.valueOf(dto.dueDate());
        if (dueDate.before(assignment.getSection().getTerm().getStartDate()) || dueDate.after(assignment.getSection().getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date is outside the course dates");
        }

        assignment.setTitle(dto.title());
        assignment.setDueDate(dueDate);

        assignment = assignmentRepository.save(assignment);

        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSecId(),
                assignment.getSection().getSectionNo()
        );
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        Section section = assignment.getSection();
        System.out.println("Instructor email from Principal: " + principal.getName());
        System.out.println("Section Instructor Email: " + section.getInstructorEmail());

        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to delete assignment in this section");
        }

         List<Grade> grades = gradeRepository.findByAssignmentId(assignmentId);
        for (Grade grade : grades) {
            gradeRepository.delete(grade);
        }

         assignmentRepository.delete(assignment);
    }



    @GetMapping("/assignments/{assignmentId}/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment not found"));

        // Check if  user is instructor for this assignment's section
        if (!assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to view grades for this assignment");
        }

         List<GradeDTO> gradeDTOs = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(assignment.getSection().getSectionNo());
        for (Enrollment e : enrollments) {
            Grade grade = gradeRepository.findByEnrollmentIdAndAssignmentId(e.getEnrollmentId(), assignment.getAssignmentId());
            if (grade == null) {
                grade = new Grade();
                grade.setAssignment(assignment);
                grade.setEnrollment(e);
                grade.setScore(0);
                grade = gradeRepository.save(grade);
            }
            gradeDTOs.add(new GradeDTO(grade.getGradeId(), grade.getEnrollment().getStudent().getName(),
                    grade.getEnrollment().getStudent().getEmail(), grade.getAssignment().getTitle(),
                    grade.getAssignment().getSection().getCourse().getCourseId(), grade.getAssignment().getSection().getSecId(), grade.getScore()));
        }

        return gradeDTOs;
    }


    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(@RequestBody List<GradeDTO> dlist, Principal principal) {
        for (GradeDTO dto : dlist) {
            Grade grade = gradeRepository.findById(dto.gradeId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade not found " + dto.gradeId()));

            Section section = grade.getAssignment().getSection();
            if (!section.getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to update grades in this section");
            }

            grade.setScore(dto.score());
            gradeRepository.save(grade);
        }
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(Principal principal,
                                                            @RequestParam("year") int year,
                                                            @RequestParam("semester") String semester) {

        User user = userRepository.findByEmail(principal.getName());
        if (user==null) {
                throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "user id not found");
        }
        else if (!user.getType().equals("STUDENT")) {
                throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "user is not a student");
        }

        List<Assignment> assignments = assignmentRepository.findByStudentIdAndYearAndSemesterOrderByDueDate(user.getId(), year, semester);
        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No assignments found for the given criteria");
        }
        List<AssignmentStudentDTO> assignmentStudentDTOs = new ArrayList<>();
        for (Assignment a : assignments) {
            Grade grade = gradeRepository.findByEnrollmentIdAndAssignmentId(a.getSection().getEnrollments().stream()
                    .filter(e -> e.getStudent().getId() == user.getId())
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found for student")).getEnrollmentId(), a.getAssignmentId());
            Integer score = (grade != null) ? grade.getScore() : null;
            assignmentStudentDTOs.add(new AssignmentStudentDTO(a.getAssignmentId(), a.getTitle(), a.getDueDate(),
                    a.getSection().getCourse().getCourseId(), a.getSection().getSecId(), score));
        }
        return assignmentStudentDTOs;
    }
}
