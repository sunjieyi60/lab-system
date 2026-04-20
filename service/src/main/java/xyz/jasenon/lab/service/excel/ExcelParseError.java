package xyz.jasenon.lab.service.excel;


public record ExcelParseError(int rowIndex, int columnIndex, String msg) {

    @Override
    public String toString(){
        return String.format("第%d行，第%d列，解析异常:%s", rowIndex, columnIndex, msg);
    }

}
