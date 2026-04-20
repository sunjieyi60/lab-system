package xyz.jasenon.lab.service.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {

    /** 成功条数 */
    private Integer ok;

    /** 失败条数 */
    private Integer fail;

    /** 详细错误信息 */
    private List<ErrorItem> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorItem {
        /** Excel 行索引（从 0 开始） */
        private Integer rowIndex;
        /** Excel 列索引（从 0 开始） */
        private Integer columnIndex;
        /** 原始单元格内容 */
        private String rawContent;
        /** 错误原因 */
        private String reason;
    }
}
