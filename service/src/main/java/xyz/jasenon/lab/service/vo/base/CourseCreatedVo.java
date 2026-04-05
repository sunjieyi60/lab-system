package xyz.jasenon.lab.service.vo.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 创建课程成功后的响应体
 */
@Getter
@Setter
@Accessors(chain = true)
public class CourseCreatedVo {

    private Long courseId;
}
