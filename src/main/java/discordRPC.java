import de.jcm.discordgamesdk.ActivityManager;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// our RPC server
class discordRPC implements Runnable {
    // initialize core features [related to discord]
    static Core basicCore;
    static ActivityManager basicAM;
    static Activity currentActivity;
    static Logger classLogger = MainFunction.classLogger;

    // related to java only
    static ScheduledExecutorService executorService;

    public void RunningRPC() throws InterruptedException {
        // core
        try(CreateParams params = new CreateParams()) {
            executorService = Executors.newScheduledThreadPool(1);

            params.setClientID(1390072222369976530L);
            params.setFlags(CreateParams.getDefaultFlags());

            // init
            try(Core core = new Core(params)) {
                basicCore = core;
                basicAM = core.activityManager();

                // setting up the activity
                try(Activity newActivity = new Activity()) {
                    // details and logo
                    newActivity.assets().setSmallImage("logo"); // dungeon
                    newActivity.assets().setSmallText("In a dungeon");

                    newActivity.assets().setLargeImage("logo"); // idk, character icon?
                    newActivity.assets().setLargeText("<< insert character name here >>");

                    newActivity.setDetails("Roaming around...");
                    newActivity.setState("Party Alive:");

                    // hi timestamp now ty
                    newActivity.timestamps().setStart(Instant.now());

                    // sets the party
                    newActivity.party().size().setCurrentSize(3);
                    newActivity.party().size().setMaxSize(3);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}

                    currentActivity = newActivity;
                    basicAM.updateActivity(currentActivity);
                }

                // hi!
                while (true) {
                    core.runCallbacks();

                    // position check
                    MainFunction.classLogger.info(String.valueOf((int) detailsRecords.Ycoord));
                    MainFunction.classLogger.info(String.valueOf((int) detailsRecords.Zcoord));
                    MainFunction.classLogger.info(String.valueOf((int) detailsRecords.Xcoord));

                    if ((int) detailsRecords.Xcoord == 93 && (int) detailsRecords.Zcoord == 34) {
                        currentActivity.setDetails(detailsRecords.mainMenu);
                    } else if (detailsRecords.Ycoord == 0 && detailsRecords.Zcoord == 0 && detailsRecords.Xcoord == 10) {
                        currentActivity.setDetails(detailsRecords.mapMenu);
                    } else {
                        currentActivity.setDetails(detailsRecords.roaming);
                    }

                    basicAM.updateActivity(currentActivity);

                    try {
                        Thread.sleep(Duration.ofSeconds(5));
                    } catch (InterruptedException e) {
                        throw new InterruptedException(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        classLogger.info("Starting!");
        RunningRPC();
    }
}
