package fsops

import (
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"unicode/utf8"
)

var defaultExcludeDirs = []string{
	".git", ".svn", ".hg", "node_modules", "bin", "target", 
	"dist", "build", "out", ".idea", ".vscode",
}

func GatherFiles(root string, recursive bool, exts []string, excludeDirs []string) ([]string, error) {
	if len(excludeDirs) == 0 {
		excludeDirs = defaultExcludeDirs
	}

	var files []string
	extMap := make(map[string]bool)
	for _, ext := range exts {
		extMap[strings.ToLower(ext)] = true
	}

	excludeMap := make(map[string]bool)
	for _, dir := range excludeDirs {
		excludeMap[dir] = true
	}

	err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			if excludeMap[info.Name()] {
				return filepath.SkipDir
			}
			if !recursive && path != root {
				return filepath.SkipDir
			}
			return nil
		}

		if len(extMap) > 0 {
			ext := strings.ToLower(filepath.Ext(path))
			if !extMap[ext] {
				return nil
			}
		}

		files = append(files, path)
		return nil
	})

	return files, err
}

func IsLikelyText(sample []byte) bool {
	// Check for null bytes (binary indicator)
	if bytes.Contains(sample, []byte{0}) {
		return false
	}

	// Check if it's valid UTF-8 or has reasonable text characteristics
	if utf8.Valid(sample) {
		return true
	}

	// Count printable characters
	printable := 0
	for _, b := range sample {
		if (b >= 32 && b <= 126) || b == '\t' || b == '\n' || b == '\r' {
			printable++
		}
	}

	// If more than 70% are printable, consider it text
	return float64(printable)/float64(len(sample)) > 0.7
}

func ParseExtensions(extStr string) []string {
	if extStr == "" {
		return nil
	}

	parts := strings.Split(extStr, ",")
	var exts []string
	for _, part := range parts {
		ext := strings.TrimSpace(part)
		if ext != "" {
			if !strings.HasPrefix(ext, ".") {
				ext = "." + ext
			}
			exts = append(exts, ext)
		}
	}
	return exts
}

func ParseExcludeDirs(excludeStr string) []string {
	if excludeStr == "" {
		return defaultExcludeDirs
	}

	parts := strings.Split(excludeStr, ",")
	var dirs []string
	for _, part := range parts {
		dir := strings.TrimSpace(part)
		if dir != "" {
			dirs = append(dirs, dir)
		}
	}
	return dirs
}