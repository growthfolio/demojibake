package core;

public class EncodingAnalysisResult {
    private String path;
    private String originalEncoding;
    private String status;
    private long processingTime;
    private double confidence;
    private int issuesFound;
    private int correctionsApplied;
    
    public EncodingAnalysisResult() {
        // Construtor padrão para JSON deserialization
    }
    
    public EncodingAnalysisResult(String path, String originalEncoding, String status) {
        this.path = path;
        this.originalEncoding = originalEncoding;
        this.status = status;
        this.processingTime = 0;
        this.confidence = 0.0;
        this.issuesFound = 0;
        this.correctionsApplied = 0;
    }
    
    // Getters e Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getOriginalEncoding() { return originalEncoding; }
    public void setOriginalEncoding(String originalEncoding) { this.originalEncoding = originalEncoding; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public int getIssuesFound() { return issuesFound; }
    public void setIssuesFound(int issuesFound) { this.issuesFound = issuesFound; }
    
    public int getCorrectionsApplied() { return correctionsApplied; }
    public void setCorrectionsApplied(int correctionsApplied) { this.correctionsApplied = correctionsApplied; }
}