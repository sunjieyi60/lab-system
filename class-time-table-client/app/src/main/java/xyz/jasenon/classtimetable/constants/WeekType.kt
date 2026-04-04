package xyz.jasenon.classtimetable.constants

/**
 * 周类型枚举
 * 使用Kotlin自动生成的getter
 * 
 * @author Jasenon_ce
 * @date 2025/9/8
 */
enum class WeekType(
    /**
     * 周类型值
     */
    val value: Int,
    
    /**
     * 周类型描述
     */
    val description: String
) {
    /**
     * 单周
     */
    Single(0, "单周"),
    
    /**
     * 双周
     */
    Double(1, "双周"),
    
    /**
     * 单周以及双周
     */
    Both(2, "单周以及双周");
}
