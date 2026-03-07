package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.course.*;
import xyz.jasenon.lab.service.service.ICourseScheduleService;
import xyz.jasenon.lab.service.service.ICourseService;
import xyz.jasenon.lab.service.service.ISemesterService;
import xyz.jasenon.lab.service.service.ITeacherService;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@Api("学期")
@RestController
@RequestMapping("/academic")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CourseController {

    @Autowired
    private ISemesterService semesterService;
    @Autowired
    private ICourseService courseService;
    @Autowired
    private ICourseScheduleService courseScheduleService;
    @Autowired
    private ITeacherService teacherService;

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/course")
    @ApiOperation("创建课程")
    public R createCourse(@Validated @RequestBody CreateCourse createCourse){
        return courseService.createCourse(createCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/courseSchedule")
    @ApiOperation("创建课程表")
    public R createCourseSchedule(@Validated @RequestBody CreateCourseSchedule createCourseSchedule){
        return courseScheduleService.createCourseSchedule(createCourseSchedule);
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @PostMapping("/create/semester")
    @ApiOperation("创建学期")
    public R createSemester(@Validated @RequestBody CreateSemester createSemester){
        return semesterService.createSemester(createSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/teacher")
    @ApiOperation("创建任课教师")
    public R createTeacher(@RequestBody CreateTeacher createTeacher){
        return teacherService.createTeacher(createTeacher);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/course")
    @ApiOperation("删除课程")
    public R deleteCourse(@Validated @RequestBody DeleteCourse deleteCourse){
        return courseService.deleteCourse(deleteCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/courseSchedule")
    @ApiOperation("删除课程表")
    public R deleteCourseSchedule(@Validated @RequestBody DeleteCourseSchedule deleteCourseSchedule){
        return courseScheduleService.deleteCourseSchedule(deleteCourseSchedule);
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @DeleteMapping("/delete/semester")
    @ApiOperation("删除学期")
    public R deleteSemester(@Validated @RequestBody DeleteSemester deleteSemester){
        return semesterService.deleteSemester(deleteSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/teacher")
    @ApiOperation("删除教师")
    public R deleteTeacher(@Validated @RequestBody DeleteTeacher deleteTeacher){
        return teacherService.deleteTeacher(deleteTeacher);
    }


    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/course")
    @ApiOperation("编辑课程")
    public R editCourse(@Validated @RequestBody EditCourse editCourse){
        return courseService.editCourse(editCourse);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/semester")
    @ApiOperation("编辑学期")
    public R editSemester(@Validated @RequestBody EditSemester editSemester){
        return semesterService.editSemester(editSemester);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/teacher")
    @ApiOperation("编辑教师")
    public R editTeacher(@Validated @RequestBody EditTeacher editTeacher){
        return teacherService.editTeacher(editTeacher);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @PostMapping("/list/courseSchedule")
    @ApiOperation("获取课程表列表")
    public R listCourseSchedule(@RequestParam List<Long> laboratoryIds){
        return courseScheduleService.listCourseSchedule(laboratoryIds);
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/laboratories")
    @ApiOperation("获取实验室列表")
    public R listLaboratory(){
        return courseScheduleService.listLaboratory();
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/course")
    @ApiOperation("获取课程列表")
    public R listCourse(){
        return courseService.listCourse();
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/semester")
    @ApiOperation("获取学期列表")
    public R listSemester(){
        return semesterService.listSemester();
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @GetMapping("/list/teacher")
    @ApiOperation("获取教师列表")
    public R listTeacher(){
        return teacherService.getAllTeacher();
    }







}
