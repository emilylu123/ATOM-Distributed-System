//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
public class Feed {
    private String content = "";
    private int timer = 0;
    private String source = "unknown";

    public Feed(String content) {
        this.content = content;
    }
    public Feed(String content, String source){
        this.content= content;
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public int getTimer() {
        return timer;
    }

    public String getSource() {
        return source;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
