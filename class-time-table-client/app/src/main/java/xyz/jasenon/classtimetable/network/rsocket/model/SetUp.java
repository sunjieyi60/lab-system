package xyz.jasenon.classtimetable.network.rsocket.model;

public class SetUp {

    /**
     * uuid
     */
    private String uuid;

    public String getUuid(){
        return uuid;
    }

    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    public SetUp(String uuid){
        this.uuid = uuid;
    }

}
