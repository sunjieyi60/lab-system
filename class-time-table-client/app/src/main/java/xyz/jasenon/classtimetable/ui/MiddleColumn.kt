package xyz.jasenon.classtimetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jasenon.classtimetable.dto.CourseScheduleDto
import xyz.jasenon.classtimetable.constants.WeekType

/**
 * 课程表布局常量配置
 * 用于统一管理课程表渲染相关的魔法值
 */
object CourseScheduleConstants {
    // ========== 表格结构常量 ==========
    /** 课程表总行数（节次数），用于：buildCellData()、MergedTableLayout() 的行数计算 */
    const val TOTAL_SECTIONS = 11
    
    /** 课程表总列数（1个节次列 + 7个星期列），用于：MergedTableLayout() 的列数计算 */
    const val TOTAL_COLUMNS = 8
    
    /** 星期列数量（星期一至星期日），用于：TableHeader()、buildCellData() 的星期列渲染 */
    const val WEEKDAY_COUNT = 7
    
    /** 节次列索引，用于：CellData.col == 0 的判断 */
    const val SECTION_COLUMN_INDEX = 0
    
    /** 星期列起始索引，用于：buildCellData() 中星期列的索引计算 */
    const val WEEKDAY_COLUMN_START_INDEX = 1
    
    /** 星期列结束索引，用于：buildCellData() 中星期列的索引范围判断 */
    const val WEEKDAY_COLUMN_END_INDEX = 7
    
    // ========== 尺寸常量 ==========
    /** 节次列固定宽度，用于：TableHeader()、MergedTableLayout() 的节次列宽度设置 */
    val SECTION_COLUMN_WIDTH = 120.dp
    
    /** 单元格高度，用于：MergedTableLayout()、CourseCell()、EmptyCell() 的高度计算 */
    val CELL_HEIGHT = 72.5.dp
    
    /** 表头和表格内容之间的间距，用于：CourseScheduleTable() 中 Spacer 的高度 */
    val HEADER_TABLE_SPACING = 1.dp
    
    /** 表格边框宽度，用于：TableHeader()、MergedTableLayout()、TableCell() 的边框设置 */
    val TABLE_BORDER_WIDTH = 1.dp
    
    /** 卡片内边距，用于：MiddleColumn() 中 Card 的 padding */
    val CARD_PADDING = 16.dp
    
    /** 卡片阴影高度，用于：MiddleColumn() 中 Card 的 elevation */
    val CARD_ELEVATION = 4.dp
    
    /** 表头单元格内边距，用于：TableCell() 的 padding */
    val HEADER_CELL_PADDING = 8.dp
    
    /** 节次单元格内边距，用于：SectionCell() 的 padding */
    val SECTION_CELL_PADDING = 8.dp
    
    /** 课程单元格内边距，用于：CourseCell() 的 padding */
    val COURSE_CELL_PADDING = 4.dp
    
    /** 课程信息行间距，用于：CourseCell() 中 Column 的 spacedBy */
    val COURSE_INFO_SPACING = 1.dp
    
    // ========== 文本尺寸常量 ==========
    /** 课程名称字体大小，用于：CourseCell() 中课程名称的 fontSize */
    val COURSE_NAME_FONT_SIZE = 11.sp
    
    /** 课程名称行高，用于：CourseCell() 中课程名称的 lineHeight */
    val COURSE_NAME_LINE_HEIGHT = 13.sp
    
    /** 课程编号字体大小，用于：CourseCell() 中课程编号的 fontSize */
    val COURSE_ID_FONT_SIZE = 10.sp
    
    /** 课程详情字体大小，用于：CourseCell() 中周次、节次、教师、教室的 fontSize */
    val COURSE_DETAIL_FONT_SIZE = 9.sp
    
    // ========== 索引范围常量 ==========
    /** 节次起始编号，用于：buildCellData() 中节次循环的起始值 */
    const val SECTION_START_NUMBER = 1
    
    /** 行索引起始值（0-based），用于：buildCellData() 中行索引的计算 */
    const val ROW_INDEX_START = 0
    
    // ========== 默认值常量 ==========
    /** 单元格默认跨行数，用于：CellData 的 rowSpan 默认值 */
    const val DEFAULT_ROW_SPAN = 1
    
    /** 单元格默认跨列数，用于：CellData 的 colSpan 默认值 */
    const val DEFAULT_COL_SPAN = 1
    
    /** 当前周次默认值，用于：MiddleColumn() 的 currentWeek 参数默认值 */
    const val DEFAULT_CURRENT_WEEK = 1
    
    // ========== 计算相关常量 ==========
    /** 节次索引转换偏移量（1-based转0-based），用于：buildCellData() 中 startSection - 1 的计算 */
    const val SECTION_INDEX_OFFSET = 1
    
    /** 节次时间索引除数（每2节为一个时间段），用于：buildCellData() 中 (section - 1) / 2 的计算 */
    const val SECTION_TIME_INDEX_DIVISOR = 2
    
    /** 判断前半节的模数，用于：buildCellData() 中 section % 2 == 1 的判断 */
    const val FIRST_HALF_SECTION_MODULO = 2
    
    /** 列位置计算偏移量，用于：MergedTableLayout() 中 data.col - 1 的计算 */
    const val COL_POSITION_OFFSET = 1
}

/**
 * 中间列组件 - 课程表网格布局（支持真正的单元格合并）
 * 
 * 参考传统课程表样式，实现网格布局：
 * - 表头：节次、星期一至星期日
 * - 行：第1节到第12节，每节显示时间范围
 * - 单元格：显示课程详细信息，支持真正的跨行合并
 */
@Composable
fun MiddleColumn(
    courses: List<CourseScheduleDto> = emptyList(),
    currentWeek: Int = CourseScheduleConstants.DEFAULT_CURRENT_WEEK,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = CourseScheduleConstants.CARD_ELEVATION)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CourseScheduleConstants.CARD_PADDING)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题区域
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2025-2026学年第一学期课程表",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            

            
            // 课程表网格
            CourseScheduleTable(
                courses = courses,
                currentWeek = currentWeek
            )
        }
    }
}

/**
 * 课程表网格组件（支持单元格合并）
 */
@Composable
fun CourseScheduleTable(
    courses: List<CourseScheduleDto>,
    currentWeek: Int
) {
    // 定义节次时间映射
    val sectionTimes = listOf(
        "08:00~08:45" to "08:55~09:40",
        "10:00~10:45" to "10:55~11:50",
        "14:10~14:55" to "15:05~15:50",
        "16:00~16:45" to "16:55~17:40",
        "18:40~19:15" to "19:30~20:15",
        "20:20~21:05" to "21:15~22:00"
    )
    
    // 构建单元格数据（包含合并信息）
    val cellData = buildCellData(courses, currentWeek, sectionTimes)
    
    // 使用BoxWithConstraints确保表头和表格内容使用相同的宽度
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sectionColumnWidth = CourseScheduleConstants.SECTION_COLUMN_WIDTH
            val weekColumnWidth = (maxWidth - sectionColumnWidth) / CourseScheduleConstants.WEEKDAY_COUNT

            // 表头（使用相同的列宽）
            TableHeader(
                sectionColumnWidth = sectionColumnWidth,
                weekColumnWidth = weekColumnWidth
            )
        }
        
        // 表头和表格内容之间的间距
        Spacer(modifier = Modifier.height(CourseScheduleConstants.HEADER_TABLE_SPACING))
        
        // 使用自定义布局实现单元格合并
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sectionColumnWidth = CourseScheduleConstants.SECTION_COLUMN_WIDTH
            val weekColumnWidth = (maxWidth - sectionColumnWidth) / CourseScheduleConstants.WEEKDAY_COUNT
            
            MergedTableLayout(
                cellData = cellData,
                sectionColumnWidth = sectionColumnWidth,
                weekColumnWidth = weekColumnWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray)
            )
        }
    }
}

/**
 * 单元格数据类
 */
data class CellData(
    val row: Int,           // 行索引（0-based，对应第1-N节）
    val col: Int,           // 列索引（0=节次列, 1-7=星期列）
    val rowSpan: Int = CourseScheduleConstants.DEFAULT_ROW_SPAN,   // 跨行数
    val colSpan: Int = CourseScheduleConstants.DEFAULT_COL_SPAN,   // 跨列数（当前不使用）
    val course: CourseScheduleDto? = null,
    val sectionNumber: Int, // 节次编号
    val timeRange: String  // 时间范围
)

/**
 * 构建单元格数据（包含合并信息）
 */
fun buildCellData(
    courses: List<CourseScheduleDto>,
    currentWeek: Int,
    sectionTimes: List<Pair<String, String>>
): List<CellData> {
    val cellDataList = mutableListOf<CellData>()
    
    // 过滤当前周的课程
    val filteredCourses = courses.filter { isCourseInCurrentWeek(it, currentWeek) }
    
    // 记录已被占用的单元格
    val occupiedCells = mutableSetOf<Pair<Int, Int>>() // Pair<row, col>
    
    // 先处理课程单元格
    filteredCourses.forEach { course ->
        val weekdays = course.weekdays ?: return@forEach
        val startSection = course.startSection ?: return@forEach
        val endSection = course.endSection ?: startSection
        
        weekdays.forEach { dayOfWeek ->
            val day = dayOfWeek ?: return@forEach
            if (day in CourseScheduleConstants.WEEKDAY_COLUMN_START_INDEX..CourseScheduleConstants.WEEKDAY_COLUMN_END_INDEX) {
                val col = day // 1-7 对应星期一到星期日
                val row = startSection - CourseScheduleConstants.SECTION_INDEX_OFFSET // 转换为0-based索引
                val rowSpan = endSection - startSection + CourseScheduleConstants.DEFAULT_ROW_SPAN
                
                // 检查是否有冲突
                var hasConflict = false
                for (r in row until row + rowSpan) {
                    if (occupiedCells.contains(Pair(r, col))) {
                        hasConflict = true
                        break
                    }
                }
                
                if (!hasConflict) {
                    // 标记占用的单元格
                    for (r in row until row + rowSpan) {
                        occupiedCells.add(Pair(r, col))
                    }
                    
                    // 添加课程单元格
                    val rowIndex = (startSection - CourseScheduleConstants.SECTION_INDEX_OFFSET) / CourseScheduleConstants.SECTION_TIME_INDEX_DIVISOR
                    val isFirstHalf = startSection % CourseScheduleConstants.FIRST_HALF_SECTION_MODULO == CourseScheduleConstants.SECTION_START_NUMBER
                    val timeRange = if (isFirstHalf) {
                        sectionTimes[rowIndex].first
                    } else {
                        sectionTimes[rowIndex].second
                    }
                    
                    cellDataList.add(
                        CellData(
                            row = row,
                            col = col,
                            rowSpan = rowSpan,
                            course = course,
                            sectionNumber = startSection,
                            timeRange = timeRange
                        )
                    )
                }
            }
        }
    }
    
    // 填充节次列和时间列
    for (section in CourseScheduleConstants.SECTION_START_NUMBER..CourseScheduleConstants.TOTAL_SECTIONS) {
        val row = section - CourseScheduleConstants.SECTION_INDEX_OFFSET
        val rowIndex = (section - CourseScheduleConstants.SECTION_INDEX_OFFSET) / CourseScheduleConstants.SECTION_TIME_INDEX_DIVISOR
        val isFirstHalf = section % CourseScheduleConstants.FIRST_HALF_SECTION_MODULO == CourseScheduleConstants.SECTION_START_NUMBER
        val timeRange = if (isFirstHalf) {
            sectionTimes[rowIndex].first
        } else {
            sectionTimes[rowIndex].second
        }
        
        cellDataList.add(
            CellData(
                row = row,
                col = 0, // 节次列
                course = null,
                sectionNumber = section,
                timeRange = timeRange
            )
        )
    }
    
    // 填充空的星期单元格
    for (row in CourseScheduleConstants.ROW_INDEX_START until CourseScheduleConstants.TOTAL_SECTIONS) {
        for (col in CourseScheduleConstants.WEEKDAY_COLUMN_START_INDEX..CourseScheduleConstants.WEEKDAY_COLUMN_END_INDEX) {
            if (!occupiedCells.contains(Pair(row, col))) {
                val section = row + CourseScheduleConstants.SECTION_INDEX_OFFSET
                val rowIndex = (section - CourseScheduleConstants.SECTION_INDEX_OFFSET) / CourseScheduleConstants.SECTION_TIME_INDEX_DIVISOR
                val isFirstHalf = section % CourseScheduleConstants.FIRST_HALF_SECTION_MODULO == CourseScheduleConstants.SECTION_START_NUMBER
                val timeRange = if (isFirstHalf) {
                    sectionTimes[rowIndex].first
                } else {
                    sectionTimes[rowIndex].second
                }
                
                cellDataList.add(
                    CellData(
                        row = row,
                        col = col,
                        course = null,
                        sectionNumber = section,
                        timeRange = timeRange
                    )
                )
            }
        }
    }
    
    return cellDataList
}

/**
 * 自定义布局：支持单元格合并的表格
 */
@Composable
fun MergedTableLayout(
    cellData: List<CellData>,
    sectionColumnWidth: androidx.compose.ui.unit.Dp,
    weekColumnWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val rowCount = CourseScheduleConstants.TOTAL_SECTIONS
    val cellHeight = CourseScheduleConstants.CELL_HEIGHT
    val totalHeight = cellHeight * rowCount
    
    // 使用Box作为容器
    Box(modifier = modifier.height(totalHeight)) {
        // 按行列顺序渲染单元格
        val sortedData = cellData.sortedWith(compareBy<CellData> { it.row }.thenBy { it.col })
        val placedCells = mutableSetOf<Pair<Int, Int>>()
        
        sortedData.forEach { data ->
            // 检查是否已被占用（用于跨行合并）
            if (!placedCells.contains(Pair(data.row, data.col))) {
                // 根据列索引计算宽度和位置
                val cellWidth = if (data.col == 0) sectionColumnWidth else weekColumnWidth
                val x = if (data.col == 0) {
                    0.dp
                } else {
                    sectionColumnWidth + weekColumnWidth * (data.col - CourseScheduleConstants.COL_POSITION_OFFSET)
                }
                val y = cellHeight * data.row
                val cellW = cellWidth * data.colSpan
                val cellH = cellHeight * data.rowSpan
                
                // 标记所有被占用的单元格
                for (r in data.row until data.row + data.rowSpan) {
                    for (c in data.col until data.col + data.colSpan) {
                        placedCells.add(Pair(r, c))
                    }
                }

                // 渲染单元格
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(cellW, cellH)
                ) {
                when {
                            data.col == CourseScheduleConstants.SECTION_COLUMN_INDEX -> {
                            // 节次列
                            SectionCell(
                                sectionNumber = data.sectionNumber,
                                timeRange = data.timeRange
                            )
                        }
                        data.course != null -> {
                            // 课程单元格
                            CourseCell(
                                course = data.course,
                                rowSpan = data.rowSpan
                            )
                        }
                        else -> {
                            // 空单元格
                            EmptyCell()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 表头组件
 */
@Composable
fun TableHeader(
    sectionColumnWidth: androidx.compose.ui.unit.Dp,
    weekColumnWidth: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
    ) {
        // 节次列
        TableCell(
            text = "节次",
            modifier = Modifier.width(sectionColumnWidth),
            isHeader = true
        )
        
        // 星期列（使用固定宽度确保对齐）
        val weekDays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        weekDays.forEach { day ->
            TableCell(
                text = day,
                modifier = Modifier.width(weekColumnWidth),
                isHeader = true
            )
        }
    }
}

/**
 * 节次单元格组件
 */
@Composable
fun SectionCell(
    sectionNumber: Int,
    timeRange: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray))
            .padding(CourseScheduleConstants.SECTION_CELL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "第${sectionNumber}节",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = timeRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 课程单元格组件
 */
@Composable
fun CourseCell(
    course: CourseScheduleDto,
    rowSpan: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray))
            .padding(CourseScheduleConstants.COURSE_CELL_PADDING),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CourseScheduleConstants.COURSE_INFO_SPACING)
        ) {
            // 课程名称（加粗）
            Text(
                text = course.courseName ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                    fontSize = CourseScheduleConstants.COURSE_NAME_FONT_SIZE,
                    lineHeight = CourseScheduleConstants.COURSE_NAME_LINE_HEIGHT
            )
            
            // 周次范围
            val weekRange = buildWeekRangeString(course)
            if (weekRange.isNotEmpty()) {
                Text(
                    text = weekRange,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = CourseScheduleConstants.COURSE_DETAIL_FONT_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 节次范围
            val sectionRange = buildSectionRangeString(course)
            if (sectionRange.isNotEmpty()) {
                Text(
                    text = sectionRange,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = CourseScheduleConstants.COURSE_DETAIL_FONT_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 教师姓名
            course.teacherName?.let { teacherName ->
                Text(
                    text = teacherName,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = CourseScheduleConstants.COURSE_DETAIL_FONT_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 教室位置
            course.laboratoryName?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = CourseScheduleConstants.COURSE_DETAIL_FONT_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 空单元格组件
 */
@Composable
fun EmptyCell() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray))
    )
}

/**
 * 表头单元格组件
 */
@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false
) {
    Box(
        modifier = modifier
            .border(BorderStroke(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray))
            .background(
                if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(CourseScheduleConstants.HEADER_CELL_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 检查课程是否在当前周
 */
fun isCourseInCurrentWeek(course: CourseScheduleDto, currentWeek: Int): Boolean {
    val startWeek = course.startWeek ?: return false
    val endWeek = course.endWeek ?: return false
    
    if (currentWeek !in startWeek..endWeek) {
        return false
    }
    
    // 检查周类型
    val weekType = course.weekType
    return when (weekType) {
        WeekType.Single -> currentWeek % 2 == 1
        WeekType.Double -> currentWeek % 2 == 0
        WeekType.Both -> true
        null -> true
    }
}

/**
 * 构建周次范围字符串
 */
fun buildWeekRangeString(course: CourseScheduleDto): String {
    val startWeek = course.startWeek ?: return ""
    val endWeek = course.endWeek ?: return ""
    val weekType = course.weekType
    
    val weekTypeStr = when (weekType) {
        WeekType.Single -> "(单)"
        WeekType.Double -> "(双)"
        else -> ""
    }
    
    return if (startWeek == endWeek) {
        "${startWeek}周$weekTypeStr"
    } else {
        "${startWeek}-${endWeek}周$weekTypeStr"
    }
}

/**
 * 构建节次范围字符串
 */
fun buildSectionRangeString(course: CourseScheduleDto): String {
    val startSection = course.startSection ?: return ""
    val endSection = course.endSection ?: startSection
    
    return if (startSection == endSection) {
        "(第${startSection}节)"
    } else {
        "(第${startSection}-${endSection}节)"
    }
}

/**
 * 预览组件
 */
@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
fun MiddleColumnPreview() {
    MaterialTheme {
        MiddleColumn(
            courses = getSampleCourses(),
            currentWeek = 3
        )
    }
}

/**
 * 生成示例课程数据
 */
fun getSampleCourses(): List<CourseScheduleDto> {
    return listOf(
        CourseScheduleDto(
            id = 1L,
            courseId = 1L,
            courseName = "计算机网络(B)",
            teacherName = "周斌",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(2), // 星期二
            startSection = 1,
            endSection = 2
        ),
        CourseScheduleDto(
            id = 2L,
            courseId = 2L,
            courseName = "UML与软件工程建模",
            teacherName = "刘卫平",
            laboratoryName = "9号楼 S090205",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(2), // 星期二
            startSection = 3,
            endSection = 4
        ),
        CourseScheduleDto(
            id = 3L,
            courseId = 3L,
            courseName = "数据库原理与应用",
            teacherName = "曾广平",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(3), // 星期三
            startSection = 1,
            endSection = 2
        ),
        CourseScheduleDto(
            id = 4L,
            courseId = 4L,
            courseName = "最优化理论与方法",
            teacherName = "王曦照",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 15,
            weekdays = mutableListOf(5), // 星期四
            startSection = 3,
            endSection = 4
        ),
        CourseScheduleDto(
            id = 5L,
            courseId = 5L,
            courseName = "Android平台智能移动开发",
            teacherName = "张世华",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(6), // 星期五
            startSection = 9,
            endSection = 10
        )
    )
}
