package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.course.*;
import xyz.jasenon.lab.service.service.ICourseScheduleService;
import xyz.jasenon.lab.service.service.ICourseService;
import xyz.jasenon.lab.service.service.ISemesterService;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@RestController
@RequestMapping("/academic")
@CrossOrigin("*")
public class CourseController {

    @Autowired
    private ISemesterService semesterService;
    @Autowired
    private ICourseService courseService;
    @Autowired
    private ICourseScheduleService courseScheduleService;

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/course")

    public R createCourse(@RequestBody CreateCourse createCourse){
        return courseService.createCourse(createCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/courseSchedule")
    public R createCourseSchedule(@RequestBody CreateCourseSchedule createCourseSchedule){
        return courseScheduleService.createCourseSchedule(createCourseSchedule);
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @PostMapping("/create/semester")
    public R createSemester(@RequestBody CreateSemester createSemester){
        return semesterService.createSemester(createSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/course")
    public R deleteCourse(@RequestBody DeleteCourse deleteCourse){
        return courseService.deleteCourse(deleteCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/courseSchedule")
    public R deleteCourseSchedule(@RequestBody DeleteCourseSchedule deleteCourseSchedule){
        return courseScheduleService.deleteCourseSchedule(deleteCourseSchedule);
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @DeleteMapping("/delete/semester")
    public R deleteSemester(@RequestBody DeleteSemester deleteSemester){
        return semesterService.deleteSemester(deleteSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/course")
    public R editCourse(@RequestBody EditCourse editCourse){
        return courseService.editCourse(editCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/semester")
    public R editSemester(@RequestBody EditSemester editSemester){
        return semesterService.editSemester(editSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/courseSchedule")
    public R listCourseSchedule(@RequestParam List<Long> laboratoryIds){
        return courseScheduleService.listCourseSchedule(laboratoryIds);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/laboratories")
    public R listLaboratory(){
        return courseScheduleService.listLaboratory();
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/course")
    public R listCourse(){
        return courseService.listCourse();
    }
}
