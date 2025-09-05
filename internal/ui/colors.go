package ui

import (
	"fmt"
	"runtime"
)

// ANSI color codes
const (
	Reset  = "\033[0m"
	Red    = "\033[31m"
	Green  = "\033[32m"
	Yellow = "\033[33m"
	Blue   = "\033[34m"
	Purple = "\033[35m"
	Cyan   = "\033[36m"
	White  = "\033[37m"
	Bold   = "\033[1m"
)

var colorsEnabled = true

func init() {
	// Disable colors on Windows by default (can be overridden)
	if runtime.GOOS == "windows" {
		colorsEnabled = false
	}
}

func EnableColors(enable bool) {
	colorsEnabled = enable
}

func Colorize(color, text string) string {
	if !colorsEnabled {
		return text
	}
	return color + text + Reset
}

func StatusColor(status string) string {
	switch status {
	case "OK":
		return Green
	case "FIX":
		return Blue
	case "WARN":
		return Yellow
	case "ERRO", "ERROR":
		return Red
	case "SKIP":
		return Purple
	default:
		return White
	}
}

func FormatStatus(status, message string) string {
	color := StatusColor(status)
	icon := StatusIcon(status)
	return fmt.Sprintf("%s %s | %s", 
		Colorize(color, icon), 
		Colorize(Bold+color, status), 
		message)
}

func StatusIcon(status string) string {
	switch status {
	case "OK":
		return "✅"
	case "FIX":
		return "🔧"
	case "WARN":
		return "⚠️"
	case "ERRO", "ERROR":
		return "❌"
	case "SKIP":
		return "⏭️"
	default:
		return "ℹ️"
	}
}