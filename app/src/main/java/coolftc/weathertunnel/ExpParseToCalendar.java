package coolftc.weathertunnel;

class ExpParseToCalendar extends ExpClass {

    private static final long serialVersionUID = 1564387184956823057L;
    private static final int KTIME_ERR_CODE = 18001;
    private static final String ExpParseToCalendar_NAME = "ParseToCalendarErr";

    ExpParseToCalendar(String desc, Throwable source) {
        super(KTIME_ERR_CODE, ExpParseToCalendar_NAME, desc, source);
    }

}
