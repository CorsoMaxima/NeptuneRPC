import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.lang.ProcessHandle.allProcesses;

// our main java class
public class MainFunction {
    static Logger classLogger = LoggerFactory.getLogger(MainFunction.class);
    static ProcessHandle classProcess;

    // main function
    public static void main(String[] args) {
        // init
        classLogger.info("Initializing!");
        classLogger.info("Please wait!");

        Stream<ProcessHandle> processList = allProcesses();
        // get it in a loop
        for (ProcessHandle currentProcess : processList.toList()) {
            if (currentProcess.info().command().isEmpty()) continue;
            if (!currentProcess.info().command().get().contains("NeptuniaReBirth1.exe")) continue;

            classProcess = currentProcess;
        }

        // no instance has been found!
        if (classProcess == null) {
            classLogger.error("We haven't found the process instance! Exiting now!");
            System.exit(0);
        }

        // memory read thread :DDD
        MemoryReadFunction memoryFunction = new MemoryReadFunction();
        Thread memoryThread = new Thread(memoryFunction);
        memoryThread.start();

        // discordRPC thread, this is used to tell the discord server about our activity
        discordRPC runnableRPC = new discordRPC();
        Thread discordThread = new Thread(runnableRPC);
        discordThread.start();

        classLogger.info("init completed!");
        
        try {
            classProcess.onExit().get();
            classLogger.info("Goodbye!");

            // interrupts the threads
            discordThread.interrupt();
            discordThread.interrupt();

            classLogger.info("Finished. See you soon!");

        } catch (ExecutionException | InterruptedException | SecurityException e) {
            classLogger.error("Seems we got an error for the executor service! Ending the code now.");
        }
    }
}
