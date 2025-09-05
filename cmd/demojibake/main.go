package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"os"
	"os/signal"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/growthfolio/demojibake/internal/codec"
	"github.com/growthfolio/demojibake/internal/fsops"
	"github.com/growthfolio/demojibake/internal/ioext"
	"github.com/growthfolio/demojibake/internal/logx"
)

type Config struct {
	Path           string
	Extensions     string
	Recursive      bool
	DetectOnly     bool
	FromEncoding   string
	InPlace        bool
	BackupSuffix   string
	DryRun         bool
	Workers        int
	PreserveTimes  bool
	StripBOM       bool
	AddBOM         bool
	FixMojibake    bool
	Stdout         bool
	FailIfNotUTF8  bool
	ExcludeDirs    string
	Verbose        bool
}

type Result struct {
	Path       string
	Status     string
	From       string
	Confidence int
	Applied    string
	Error      error
}

type Stats struct {
	Total     int
	Changed   int
	NonUTF8   int
	Errors    int
	Skipped   int
}

func main() {
	config := parseFlags()
	
	if config.Verbose {
		logx.SetVerbose()
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Handle signals
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-sigChan
		logx.Info("Received interrupt signal, shutting down...")
		cancel()
	}()

	if err := run(ctx, config); err != nil {
		logx.Error("Error: %v", err)
		os.Exit(1)
	}
}

func parseFlags() *Config {
	config := &Config{}
	
	flag.StringVar(&config.Path, "path", ".", "File or directory path")
	flag.StringVar(&config.Extensions, "ext", ".txt,.md,.java,.xml,.properties,.csv,.html,.js,.ts,.go", "File extensions (CSV)")
	flag.BoolVar(&config.Recursive, "recursive", true, "Process directories recursively")
	flag.BoolVar(&config.DetectOnly, "detect", false, "Only detect encoding, don't convert")
	flag.StringVar(&config.FromEncoding, "from", "", "Force source encoding")
	flag.BoolVar(&config.InPlace, "in-place", false, "Modify files in place")
	flag.StringVar(&config.BackupSuffix, "backup-suffix", ".bak", "Backup suffix (empty to disable)")
	flag.BoolVar(&config.DryRun, "dry-run", false, "Show what would be done without making changes")
	flag.IntVar(&config.Workers, "workers", runtime.NumCPU()/2, "Number of worker goroutines")
	flag.BoolVar(&config.PreserveTimes, "preserve-times", true, "Preserve file modification times")
	flag.BoolVar(&config.StripBOM, "strip-bom", true, "Strip UTF-8 BOM")
	flag.BoolVar(&config.AddBOM, "add-bom", false, "Add UTF-8 BOM")
	flag.BoolVar(&config.FixMojibake, "fix-mojibake", true, "Attempt to fix mojibake")
	flag.BoolVar(&config.Stdout, "stdout", false, "Output to stdout (single file only)")
	flag.BoolVar(&config.FailIfNotUTF8, "fail-if-not-utf8", false, "Exit with error if non-UTF8 files found")
	flag.StringVar(&config.ExcludeDirs, "exclude-dirs", "", "Directories to exclude (CSV)")
	flag.BoolVar(&config.Verbose, "v", false, "Verbose output")
	
	flag.Parse()

	if config.Workers < 1 {
		config.Workers = 2
	}
	if config.AddBOM {
		config.StripBOM = false
	}

	return config
}

func run(ctx context.Context, config *Config) error {
	startTime := time.Now()
	
	// Gather files
	var files []string
	var err error
	
	info, err := os.Stat(config.Path)
	if err != nil {
		return fmt.Errorf("cannot access path %s: %v", config.Path, err)
	}

	if info.IsDir() {
		exts := fsops.ParseExtensions(config.Extensions)
		excludeDirs := fsops.ParseExcludeDirs(config.ExcludeDirs)
		files, err = fsops.GatherFiles(config.Path, config.Recursive, exts, excludeDirs)
		if err != nil {
			return fmt.Errorf("error gathering files: %v", err)
		}
	} else {
		files = []string{config.Path}
		if config.Stdout && !config.DetectOnly {
			return processStdout(config.Path, config)
		}
	}

	if len(files) == 0 {
		logx.Info("No files found to process")
		return nil
	}

	// Process files
	results := make(chan Result, len(files))
	jobs := make(chan string, len(files))

	// Start workers
	var wg sync.WaitGroup
	for i := 0; i < config.Workers; i++ {
		wg.Add(1)
		go worker(ctx, &wg, jobs, results, config)
	}

	// Send jobs
	go func() {
		defer close(jobs)
		for _, file := range files {
			select {
			case jobs <- file:
			case <-ctx.Done():
				return
			}
		}
	}()

	// Collect results
	go func() {
		wg.Wait()
		close(results)
	}()

	stats := &Stats{}
	for result := range results {
		stats.Total++
		
		if result.Error != nil {
			stats.Errors++
			logx.Printf("ERRO | %s | error=%v\n", result.Path, result.Error)
		} else {
			confidence := ""
			if result.Confidence > 0 {
				confidence = fmt.Sprintf(" conf=%d", result.Confidence)
			}
			
			applied := ""
			if result.Applied != "" {
				applied = fmt.Sprintf(" | applied=%s", result.Applied)
			}
			
			logx.Printf("%s | %s | from=%s%s%s\n", 
				result.Status, result.Path, result.From, confidence, applied)
			
			switch result.Status {
			case "FIX":
				stats.Changed++
			case "WARN":
				stats.NonUTF8++
			case "SKIP":
				stats.Skipped++
			}
		}
	}

	// Print summary
	duration := time.Since(startTime)
	logx.Printf("\nArquivos: %d | Alterados: %d | Restantes não-UTF8: %d | Erros: %d | Ignorados: %d | Tempo: %v\n",
		stats.Total, stats.Changed, stats.NonUTF8, stats.Errors, stats.Skipped, duration)

	if config.FailIfNotUTF8 && stats.NonUTF8 > 0 {
		return fmt.Errorf("found %d non-UTF8 files", stats.NonUTF8)
	}

	if stats.Errors > 0 {
		os.Exit(1)
	}

	return nil
}

func worker(ctx context.Context, wg *sync.WaitGroup, jobs <-chan string, results chan<- Result, config *Config) {
	defer wg.Done()
	
	for {
		select {
		case file, ok := <-jobs:
			if !ok {
				return
			}
			result := processFile(file, config)
			select {
			case results <- result:
			case <-ctx.Done():
				return
			}
		case <-ctx.Done():
			return
		}
	}
}

func processFile(path string, config *Config) Result {
	const sampleSize = 64 * 1024
	
	sample, info, err := ioext.ReadSample(path, sampleSize)
	if err != nil {
		return Result{Path: path, Status: "ERRO", Error: err}
	}

	// Check if binary
	if !fsops.IsLikelyText(sample) {
		return Result{Path: path, Status: "SKIP", From: "binary"}
	}

	// Detect encoding
	var fromEncoding string
	var confidence int
	
	if config.FromEncoding != "" {
		fromEncoding = config.FromEncoding
	} else {
		detected, conf, _, err := codec.Detect(sample)
		if err != nil {
			return Result{Path: path, Status: "ERRO", Error: err}
		}
		fromEncoding = detected
		confidence = conf
	}

	if config.DetectOnly {
		status := "OK"
		if fromEncoding != "utf-8" {
			status = "WARN"
		}
		return Result{
			Path: path, Status: status, From: fromEncoding, 
			Confidence: confidence,
		}
	}

	// Process file
	return convertFile(path, fromEncoding, confidence, info, config)
}

func convertFile(path, fromEncoding string, confidence int, info os.FileInfo, config *Config) Result {
	file, err := os.Open(path)
	if err != nil {
		return Result{Path: path, Status: "ERRO", Error: err}
	}
	defer file.Close()

	// Create reader chain
	var reader io.Reader = file
	var applied []string

	// Convert encoding if needed
	if fromEncoding != "" && fromEncoding != "utf-8" {
		convertedReader, appliedConv, err := codec.ConvertToUTF8Stream(reader, fromEncoding)
		if err != nil {
			return Result{Path: path, Status: "ERRO", Error: err}
		}
		reader = convertedReader
		if appliedConv != "" {
			applied = append(applied, appliedConv)
		}
	}

	// Read content for mojibake fixing
	content, err := io.ReadAll(reader)
	if err != nil {
		return Result{Path: path, Status: "ERRO", Error: err}
	}

	// Fix mojibake if enabled
	contentStr := string(content)
	if config.FixMojibake {
		fixed, appliedFix, ok := codec.TryLatin1RoundTrip(contentStr)
		if ok {
			contentStr = fixed
			applied = append(applied, appliedFix)
		}
	}

	// Handle BOM
	contentBytes := []byte(contentStr)
	hasBOM := ioext.HasUTF8BOM(contentBytes)
	
	if config.StripBOM && hasBOM {
		contentBytes = ioext.StripUTF8BOM(contentBytes)
		applied = append(applied, "strip-bom")
	} else if config.AddBOM && !hasBOM {
		contentBytes = ioext.AddUTF8BOM(contentBytes)
		applied = append(applied, "add-bom")
	}

	// Check if content changed
	originalContent, _ := os.ReadFile(path)
	if string(contentBytes) == string(originalContent) {
		return Result{
			Path: path, Status: "OK", From: fromEncoding, 
			Confidence: confidence, Applied: strings.Join(applied, ","),
		}
	}

	if config.DryRun {
		return Result{
			Path: path, Status: "FIX", From: fromEncoding,
			Confidence: confidence, Applied: strings.Join(applied, ","),
		}
	}

	// Write file
	if config.InPlace {
		err = writeFileInPlace(path, contentBytes, info, config)
	} else {
		err = writeFileToStdout(contentBytes)
	}

	if err != nil {
		return Result{Path: path, Status: "ERRO", Error: err}
	}

	return Result{
		Path: path, Status: "FIX", From: fromEncoding,
		Confidence: confidence, Applied: strings.Join(applied, ","),
	}
}

func writeFileInPlace(path string, content []byte, info os.FileInfo, config *Config) error {
	// Create backup if needed
	if config.BackupSuffix != "" {
		backupPath := path + config.BackupSuffix
		if _, err := os.Stat(backupPath); os.IsNotExist(err) {
			if err := copyFile(path, backupPath); err != nil {
				return fmt.Errorf("failed to create backup: %v", err)
			}
		}
	}

	// Write atomically
	tmpPath, tmpFile, cleanup, err := ioext.OpenAtomicWrite(path)
	if err != nil {
		return err
	}
	defer cleanup()

	if _, err := tmpFile.Write(content); err != nil {
		return err
	}

	if err := tmpFile.Close(); err != nil {
		return err
	}

	// Set permissions
	if err := os.Chmod(tmpPath, info.Mode()); err != nil {
		return err
	}

	// Atomic rename
	if err := ioext.AtomicRename(tmpPath, path); err != nil {
		return err
	}

	// Preserve times if requested
	if config.PreserveTimes {
		if err := os.Chtimes(path, info.ModTime(), info.ModTime()); err != nil {
			logx.Warn("Failed to preserve times for %s: %v", path, err)
		}
	}

	return nil
}

func writeFileToStdout(content []byte) error {
	_, err := os.Stdout.Write(content)
	return err
}

func processStdout(path string, config *Config) error {
	result := processFile(path, config)
	if result.Error != nil {
		return result.Error
	}
	return nil
}

func copyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer sourceFile.Close()

	destFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	return err
}