package core;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public interface DemojibakelizadorNative extends Library {
    DemojibakelizadorNative INSTANCE = Native.load(getLibraryPath(), DemojibakelizadorNative.class);
    
    // Core functions
    int Initialize();
    String ProcessFileAdvanced(String path, String options);
    int ProcessBatchParallel(String jsonPaths, ProgressCallback callback, String options);
    String GetDictionaryStats();
    int UpdateDictionary(String words);
    void FreeMemory(Pointer ptr);
    void Shutdown();
    
    // Callback interface for progress reporting
    interface ProgressCallback extends Callback {
        void invoke(int current, int total, String filename, String status);
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
        } else {
            libPath = "lib/linux/" + arch + "/libdemojibake";
        }
        
        return libPath;
    }
}

// Data structures matching Go types
class ProcessingResult extends Structure {
    public String path;
    public String originalEncoding;
    public Issue[] issues;
    public Correction[] corrections;
    public double confidence;
    public long processingTime;
    public String status;
    
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("path", "originalEncoding", "issues", 
                           "corrections", "confidence", "processingTime", "status");
    }
}

class Issue extends Structure {
    public String type;
    public int position;
    public String original;
    public String context;
    
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "position", "original", "context");
    }
}

class Correction extends Structure {
    public int position;
    public String original;
    public String corrected;
    public double confidence;
    public String method;
    
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("position", "original", "corrected", "confidence", "method");
    }
}