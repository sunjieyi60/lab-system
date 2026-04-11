package xyz.jasenon.lab.common;

public interface Const {

    String TRACE_ID_KEY = "trace";

    interface Key {
        String SUFFIX = ":";

        String SEMSTER = "semster";

        String INFO = "info";

        default String semesterInfo(Long semsterId){
            return SEMSTER + SUFFIX + semsterId + SUFFIX + INFO;
        }
    }

    interface Mysql {
        String LOCK = "for update";

        String SKIP_LOCK = "skip locked";

        default String lockWithSkip(){
            return LOCK + SKIP_LOCK;
        }
    }

}
