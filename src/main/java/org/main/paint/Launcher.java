package org.main.paint;

public class Launcher {
    /**
     * Main entry point for the application when running from a packaged JAR.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting Paint App through Launcher...");

        try {
            System.setProperty("java.util.logging.config.file", "logging.properties");
        } catch (Exception e) {
            System.err.println("Could not configure logging: " + e.getMessage());
        }
        
        Main.main(args);
    }
}
