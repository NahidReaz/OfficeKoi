package com.example.officekoi.historyRecyclerView;



public class HistoryObject {
    private String spaceId;
    private String time;

    public HistoryObject(String spaceId, String time){
        this.spaceId = spaceId;
        this.time = time;
    }

    public String getSpaceId(){return spaceId;}
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getTime(){return time;}
    public void setTime(String time) {
        this.time = time;
    }
}
