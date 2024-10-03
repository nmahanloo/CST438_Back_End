package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentRepository extends CrudRepository<Assignment, Integer> {

    @Query("SELECT a FROM Assignment a JOIN a.section s JOIN s.enrollments e JOIN e.student st WHERE st.email = :email AND s.term.year = :year AND s.term.semester = :semester ORDER BY a.dueDate")
    List<Assignment> findByStudentEmailAndYearAndSemesterOrderByDueDate(@Param("email") String email, @Param("year") int year, @Param("semester") String semester);

    @Query("select a from Assignment a where a.section.sectionNo=:sectionNo order by a.dueDate")
    List<Assignment> findBySectionNoOrderByDueDate(int sectionNo);

    @Query("select a from Assignment a join a.section.enrollments e " +
            "where a.section.term.year=:year and a.section.term.semester=:semester and" +
            " e.student.id=:studentId order by a.dueDate")
    List<Assignment> findByStudentIdAndYearAndSemesterOrderByDueDate(int studentId, int year, String semester);
}
