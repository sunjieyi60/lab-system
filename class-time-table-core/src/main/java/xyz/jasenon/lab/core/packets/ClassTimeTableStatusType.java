package xyz.jasenon.lab.core.packets;

import lombok.Getter;

/**
 * @author Jasenon_ce
 * @date 2026/2/28
 */
@Getter
public enum ClassTimeTableStatusType {

    /**
     * <pre>
     *     在线
     * </pre>
     * <code>ONLINE = 0</code>
     */
    ONLINE(0,"online","在线"),

    /**
     * <pre>
     *     离线
     * </pre>
     * <code>OFFLINE = 1</code>
     */
    OFFLINE(1,"offline","离线"),

    /**
     * <pre>
     *     ALL所有
     * </pre>
     * <code>ALL = 2</code>
     */
    ALL(2,"all","所有")
    ;


    ClassTimeTableStatusType(Integer value, String status, String desc){
        this.number = value;
        this.status = status;
        this.desc = desc;
    }

    public static ClassTimeTableStatusType valueOf(Integer value) {
        for (ClassTimeTableStatusType type : ClassTimeTableStatusType.values()) {
            if (type.number == value) {
                return type;
            }
        }
        return null;
    }

    private final Integer number;
    private final String status;
    private final String desc;

}
