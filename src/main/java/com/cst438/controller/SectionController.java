package com.cst438.controller;

import java.security.Principal;
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

import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.Term;
import com.cst438.domain.TermRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.SectionDTO;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class SectionController {

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    UserRepository userRepository;

    // ADMIN function to create a new section
    @PostMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public SectionDTO addSection(@RequestBody SectionDTO section) {
        Course course = courseRepository.findById(section.courseId()).orElse(null);
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course not found " + section.courseId());
        }
        Section s = new Section();
        s.setCourse(course);

        Term term = termRepository.findByYearAndSemester(section.year(), section.semester());
        if (term == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year, semester invalid ");
        }
        s.setTerm(term);

        s.setSecId(section.secId());
        s.setBuilding(section.building());
        s.setRoom(section.room());
        s.setTimes(section.times());

        User instructor = null;
        if (section.instructorEmail() == null || section.instructorEmail().equals("")) {
            s.setInstructor_email("");
        } else {
            instructor = userRepository.findByEmail(section.instructorEmail());
            if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email not found or not an instructor " + section.instructorEmail());
            }
            s.setInstructor_email(section.instructorEmail());
        }

        sectionRepository.save(s);
        return new SectionDTO(
                s.getSectionNo(),
                s.getTerm().getYear(),
                s.getTerm().getSemester(),
                s.getCourse().getCourseId(),
                s.getCourse().getTitle(),
                s.getSecId(),
                s.getBuilding(),
                s.getRoom(),
                s.getTimes(),
                (instructor != null) ? instructor.getName() : "",
                (instructor != null) ? instructor.getEmail() : ""
        );
    }

   // ADMIN function to update a section
@PutMapping("/sections")
@PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
public void updateSection(@RequestBody SectionDTO section, Principal principal) {

    // can only change instructor email, sec_id, building, room, times, start, end dates
    Section s = sectionRepository.findById(section.secNo()).orElse(null);
    if (s == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "section not found " + section.secNo());
    }
    s.setSecId(section.secId());
    s.setBuilding(section.building());
    s.setRoom(section.room());
    s.setTimes(section.times());

    User instructor = null;
    if (section.instructorEmail() == null || section.instructorEmail().equals("")) {
        s.setInstructor_email("");
    } else {
        instructor = userRepository.findByEmail(section.instructorEmail());
        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found or not an instructor " + section.instructorEmail());
        }
        s.setInstructor_email(section.instructorEmail());
    }
    sectionRepository.save(s);
}


    // ADMIN function to delete a section
    @DeleteMapping("/sections/{sectionno}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public void deleteSection(@PathVariable int sectionno, Principal principal) {
        Section s = sectionRepository.findById(sectionno).orElse(null);
        if (s != null) {
            sectionRepository.delete(s);
        }
    }

    // get Sections with query params courseId, year, semester
    @GetMapping("/courses/{courseId}/sections")
    public List<SectionDTO> getSections(
            @PathVariable("courseId") String courseId,
            @RequestParam("year") int year,
            @RequestParam("semester") String semester) {

        List<Section> sections = sectionRepository.findByLikeCourseIdAndYearAndSemester(courseId + "%", year, semester);

        List<SectionDTO> dto_list = new ArrayList<>();
        for (Section s : sections) {
            User instructor = null;
            if (s.getInstructorEmail() != null) {
                instructor = userRepository.findByEmail(s.getInstructorEmail());
            }
            dto_list.add(new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSecId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    (instructor != null) ? instructor.getName() : "",
                    (instructor != null) ? instructor.getEmail() : ""
            ));
        }
        return dto_list;
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        String instructorEmail = principal.getName();

        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(instructorEmail, year, semester);

        List<SectionDTO> dto_list = new ArrayList<>();
        for (Section s : sections) {
            User instructor = null;
            if (s.getInstructorEmail() != null) {
                instructor = userRepository.findByEmail(s.getInstructorEmail());
            }
            dto_list.add(new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSecId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    (instructor != null) ? instructor.getName() : "",
                    (instructor != null) ? instructor.getEmail() : ""
            ));
        }
        return dto_list; // Returns an empty JSON array if no sections are found
    }

    @GetMapping("/sections/open")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<SectionDTO> getOpenSectionsForEnrollment() {
        List<Section> sections = sectionRepository.findByOpenOrderByCourseIdSectionId();
        List<SectionDTO> dlist = new ArrayList<>();
        for (Section s : sections) {
            User instructor = userRepository.findByEmail(s.getInstructorEmail());
            dlist.add(new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSecId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    (instructor != null) ? instructor.getName() : "",
                    (instructor != null) ? instructor.getEmail() : ""
            ));
        }
        return dlist;
    }
}
