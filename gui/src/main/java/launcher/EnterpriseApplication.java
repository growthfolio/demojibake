package launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import ui.MainApplication;
import core.DemojibakelizadorNative;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EnterpriseApplication extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(EnterpriseApplication.class.getName());
    
    // Performance optimization flags
    static {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("prism.order", "d3d,sw");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        System.setProperty("javafx.animation.fullspeed", "true");
        
        // JVM optimization
        System.setProperty("java.vm.options", 
            "-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+UnlockExperimentalVMOptions");
    }
    
    @Override
    public void init() throws Exception {
        LOGGER.info("Initializing Demojibakelizador Enterprise...");
        
        // Async initialization for faster startup
        CompletableFuture<Void> nativeInit = CompletableFuture.runAsync(() -> {
            try {
                int status = DemojibakelizadorNative.INSTANCE.Initialize();
                if (status != 1) {
                    throw new RuntimeException("Native library initialization failed: " + status);
                }
                LOGGER.info("Native library initialized successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize native library", e);
                Platform.exit();
            }
        });
        
        // Preload JavaFX components
        CompletableFuture<Void> fxInit = CompletableFuture.runAsync(() -> {
            // Preload common JavaFX classes
            Platform.runLater(() -> {});
        });
        
        // Wait for both initializations
        CompletableFuture.allOf(nativeInit, fxInit).get();
        
        LOGGER.info("Application initialization completed");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.info("Starting main application window");
        
        // Delegate to main application
        MainApplication mainApp = new MainApplication();
        mainApp.start(primaryStage);
        
        // Setup global exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), exception);
            
            Platform.runLater(() -> {
                // Show error dialog and graceful shutdown
                showCriticalError(exception);
            });
        });
        
        LOGGER.info("Application started successfully");
    }
    
    @Override
    public void stop() throws Exception {
        LOGGER.info("Shutting down application...");
        
        try {
            // Cleanup native resources
            DemojibakelizadorNative.INSTANCE.Shutdown();
            LOGGER.info("Native library shutdown completed");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during native library shutdown", e);
        }
        
        LOGGER.info("Application shutdown completed");
    }
    
    private void showCriticalError(Throwable exception) {
        // Implementation would show error dialog
        LOGGER.severe("Critical error occurred: " + exception.getMessage());
        Platform.exit();
    }
    
    public static void main(String[] args) {
        // Set up logging
        System.setProperty("java.util.logging.config.class", 
            "launcher.LoggingConfiguration");
        
        LOGGER.info("Starting Demojibakelizador Enterprise v2.0");
        
        // Launch JavaFX application
        launch(args);
    }
}