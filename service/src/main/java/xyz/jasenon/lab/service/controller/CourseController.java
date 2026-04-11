package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.course.*;
import xyz.jasenon.lab.service.service.ICourseScheduleService;
import xyz.jasenon.lab.service.vo.base.CourseCreatedVo;
import xyz.jasenon.lab.service.vo.base.CourseScheduleVo;
import xyz.jasenon.lab.service.service.ICourseService;
import xyz.jasenon.lab.service.service.ISemesterService;
import xyz.jasenon.lab.service.service.ITeacherService;

import java.util.List;
import java.util.Map;

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
    public DiyResponseEntity<R<CourseCreatedVo>> createCourse(@Validated @RequestBody CreateCourse createCourse){
        return DiyResponseEntity.of(R.success(courseService.createCourse(createCourse)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/courseSchedule")
    @ApiOperation("创建课程表")
    public DiyResponseEntity<R<CourseSchedule>> createCourseSchedule(@Validated @RequestBody CreateCourseSchedule createCourseSchedule){
        return DiyResponseEntity.of(R.success(courseScheduleService.createCourseSchedule(createCourseSchedule)));
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @PostMapping("/create/semester")
    @ApiOperation("创建学期")
    public DiyResponseEntity<R<Semester>> createSemester(@Validated @RequestBody CreateSemester createSemester){
        return DiyResponseEntity.of(R.success(semesterService.createSemester(createSemester)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PostMapping("/create/teacher")
    @ApiOperation("创建任课教师")
    public DiyResponseEntity<R<Teacher>> createTeacher(@RequestBody CreateTeacher createTeacher){
        return DiyResponseEntity.of(R.success(teacherService.createTeacher(createTeacher)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/course")
    @ApiOperation("删除课程")
    public DiyResponseEntity<R<Void>> deleteCourse(@Validated @RequestBody DeleteCourse deleteCourse){
        courseService.deleteCourse(deleteCourse);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/courseSchedule")
    @ApiOperation("删除特定时间的课程")
    public DiyResponseEntity<R<Void>> deleteCourseSchedule(@Validated @RequestBody DeleteCourseSchedule deleteCourseSchedule){
        courseScheduleService.deleteCourseSchedule(deleteCourseSchedule);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.SEMESTER_SETTINGS})
    @DeleteMapping("/delete/semester")
    @ApiOperation("删除学期")
    public DiyResponseEntity<R<Void>> deleteSemester(@Validated @RequestBody DeleteSemester deleteSemester){
        semesterService.deleteSemester(deleteSemester);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/teacher")
    @ApiOperation("删除教师")
    public DiyResponseEntity<R<Void>> deleteTeacher(@Validated @RequestBody DeleteTeacher deleteTeacher){
        teacherService.deleteTeacher(deleteTeacher);
        return DiyResponseEntity.of(R.success());
    }


    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/course")
    @ApiOperation("编辑课程")
    public DiyResponseEntity<R<Course>> editCourse(@Validated @RequestBody EditCourse editCourse){
        return DiyResponseEntity.of(R.success(courseService.editCourse(editCourse)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/semester")
    @ApiOperation("编辑学期")
    public DiyResponseEntity<R<Semester>> editSemester(@Validated @RequestBody EditSemester editSemester){
        return DiyResponseEntity.of(R.success(semesterService.editSemester(editSemester)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/teacher")
    @ApiOperation("编辑教师")
    public DiyResponseEntity<R<Teacher>> editTeacher(@Validated @RequestBody EditTeacher editTeacher){
        return DiyResponseEntity.of(R.success(teacherService.editTeacher(editTeacher)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @PutMapping("/edit/courseSchedule")
    @ApiOperation("编辑课程表")
    public DiyResponseEntity<R<CourseSchedule>> editCourseSchedule(@Validated @RequestBody EditCourseSchedule editCourseSchedule){
        return DiyResponseEntity.of(R.success(courseScheduleService.editCourseScheduleWeekdays(editCourseSchedule)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @DeleteMapping("/delete/courseScheduleByLaboratory")
    @ApiOperation("删除实验室全部课程表")
    public DiyResponseEntity<R<Void>> deleteCourseScheduleByLaboratory(@RequestParam Long laboratoryId) {
        courseScheduleService.deleteCourseScheduleByLaboratoryId(laboratoryId);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @PostMapping("/list/courseSchedule")
    @ApiOperation("获取教室课程表列表")
    public DiyResponseEntity<R<Map<Long, List<CourseScheduleVo>>>> listCourseSchedule(@RequestParam List<Long> laboratoryIds){
        return DiyResponseEntity.of(R.success(courseScheduleService.listCourseSchedule(laboratoryIds)));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/laboratories")
    @ApiOperation("获取实验室列表")
    public DiyResponseEntity<R<List<xyz.jasenon.lab.common.entity.base.Laboratory>>> listLaboratory(){
        return DiyResponseEntity.of(R.success(courseScheduleService.listLaboratory()));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/course")
    @ApiOperation("获取课程列表")
    public DiyResponseEntity<R<List<Course>>> listCourse(){
        return DiyResponseEntity.of(R.success(courseService.listCourse()));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES,Permissions.SCHEDULE_CLASSES_VIEW})
    @GetMapping("/list/semester")
    @ApiOperation("获取学期列表")
    public DiyResponseEntity<R<List<Semester>>> listSemester(){
        return DiyResponseEntity.of(R.success(semesterService.listSemester()));
    }

    @RequestPermission(allowed = {Permissions.SCHEDULE_CLASSES})
    @GetMapping("/list/teacher")
    @ApiOperation("获取教师列表")
    public DiyResponseEntity<R<List<Teacher>>> listTeacher(){
        return DiyResponseEntity.of(R.success(teacherService.getAllTeacher()));
    }





}
