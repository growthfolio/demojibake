package core;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface TextEncodingAnalyzer extends Library {
    TextEncodingAnalyzer INSTANCE = loadCharacterEncodingEngine();
    
    static TextEncodingAnalyzer loadCharacterEncodingEngine() {
        try {
            // Try to load from java.library.path first
            return Native.load("character_encoding_engine", TextEncodingAnalyzer.class);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load character encoding engine: " + e.getMessage(), e);
        }
    }
    
    // Core character encoding analysis functions
    int InitializeEncodingEngine();
    String AnalyzeDocumentEncoding(String documentPath, String analysisOptions);
    int ProcessDocumentCollectionConcurrently(String documentPathsJson, DocumentAnalysisProgressCallback callback, String processingOptions);
    String RetrieveLanguageDictionaryMetrics();
    int EnrichLanguageDictionary(String vocabularyTerms);
    void ReleaseAllocatedMemory(Pointer memoryPtr);
    void GracefulEngineShutdown();
    
    // Callback interface for analysis progress reporting
    interface DocumentAnalysisProgressCallback extends Callback {
        void invoke(int processedCount, int totalDocuments, String currentDocument, String analysisStatus);
    }
    
    // Library path resolution
    static String getLibraryPath() {
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
            libPath = "lib/windows/" + arch + "/demojibake";
        } else if (os.contains("mac")) {
            libPath = "lib/macos/" + arch + "/libdemojibake";
        } else if (os.contains("linux")) {
            libPath = "lib/linux/" + arch + "/libdemojibake";
        } else {
            throw new UnsupportedOperationException("Unsupported platform: " + os + " " + arch);
        }
        
        return libPath;
    }
}

