package gov.noaa.ncei.gis.service

import java.text.SimpleDateFormat;
//import java.util.Date;
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
public class ScheduledTasksService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss")

    @Scheduled(fixedRate = 5000L)
    public void reportCurrentTime() {
        println("The time is now " + dateFormat.format(new Date()))
    }

    @Scheduled(cron="0 */15 * * * *")
    public void checkEvery15Minutes() {
        println("This is your 15 minute check. The time is now " + dateFormat.format(new Date()))
    }

    @Scheduled(cron="0 0 * * * *")
    public void checkHourly() {
        println("This is your hourly check. The time is now " + dateFormat.format(new Date()))
    }

    @Scheduled(cron="0 0 0 * * *")
    public void checkDaily() {
        println("This is your daily check. The time is now " + dateFormat.format(new Date()))
    }

}