package core;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface MojibakeProcessor extends Library {
    MojibakeProcessor INSTANCE = loadDemojibakelizadorEngine();
    
    static MojibakeProcessor loadDemojibakelizadorEngine() {
        try {
            // Try different library names
            String[] libraryNames = {
                "demojibake",
                "libdemojibake",
                "character_encoding_engine",
                "libcharacter_encoding_engine"
            };
            
            for (String name : libraryNames) {
                try {
                    return Native.load(name, MojibakeProcessor.class);
                } catch (UnsatisfiedLinkError e) {
                    // Continue to next library name
                }
            }
            
            // If all fail, try with full path
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch");
            
            // Normalize architecture names
            if (arch.equals("amd64") || arch.equals("x86_64")) {
                arch = "amd64";
            } else if (arch.equals("aarch64") || arch.equals("arm64")) {
                arch = "arm64";
            }
            
            String libPath;
            if (os.contains("win")) {
                libPath = "../native_libraries/windows/" + arch + "/demojibake";
            } else if (os.contains("mac")) {
                libPath = "../native_libraries/macos/" + arch + "/libdemojibake";
            } else {
                libPath = "../native_libraries/linux/" + arch + "/libdemojibake";
            }
            
            return Native.load(libPath, MojibakeProcessor.class);
            
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load mojibake processor engine: " + e.getMessage(), e);
        }
    }
    
    // Core mojibake processing functions - these map to the Go exports
    int InitializeEncodingEngine();
    String AnalyzeDocumentEncoding(String documentPath, String analysisOptions);
    int ProcessDocumentCollectionConcurrently(String documentPathsJson, String processingOptions);
    String RetrieveLanguageDictionaryMetrics();
    int EnrichLanguageDictionary(String vocabularyTerms);
    void ReleaseAllocatedMemory(Pointer memoryPtr);
    void GracefulEngineShutdown();
    
    // Convenience methods for common operations
    default int Initialize() {
        return InitializeEncodingEngine();
    }
    
    default void Shutdown() {
        GracefulEngineShutdown();
    }
    
    default String ProcessFile(String filePath) {
        return AnalyzeDocumentEncoding(filePath, "{}");
    }
    
    default String ProcessFileWithOptions(String filePath, String options) {
        return AnalyzeDocumentEncoding(filePath, options);
    }
    
    // Callback interface for batch processing progress
    interface ProcessingProgressCallback extends Callback {
        void invoke(int current, int total, String filename, String status);
    }
    
    // Processing options builder
    class ProcessingOptions {
        private boolean aggressiveMode = false;
        private boolean backupFiles = true;
        private double confidenceThreshold = 0.8;
        
        public ProcessingOptions setAggressiveMode(boolean aggressive) {
            this.aggressiveMode = aggressive;
            return this;
        }
        
        public ProcessingOptions setBackupFiles(boolean backup) {
            this.backupFiles = backup;
            return this;
        }
        
        public ProcessingOptions setConfidenceThreshold(double threshold) {
            this.confidenceThreshold = threshold;
            return this;
        }
        
        public String toJson() {
            return String.format(
                "{\"aggressive_mode\":%b,\"backup_files\":%b,\"confidence_threshold\":%.2f}",
                aggressiveMode, backupFiles, confidenceThreshold
            );
        }
    }
}
