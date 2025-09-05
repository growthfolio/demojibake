package logx

import (
	"fmt"
	"log"
	"os"
)

type Level int

const (
	ERROR Level = iota
	WARN
	INFO
	DEBUG
)

var (
	currentLevel = INFO
	logger       = log.New(os.Stderr, "", log.LstdFlags)
)

func SetLevel(level Level) {
	currentLevel = level
}

func SetVerbose() {
	currentLevel = DEBUG
}

func Error(format string, args ...interface{}) {
	if currentLevel >= ERROR {
		logger.Printf("[ERROR] "+format, args...)
	}
}

func Warn(format string, args ...interface{}) {
	if currentLevel >= WARN {
		logger.Printf("[WARN] "+format, args...)
	}
}

func Info(format string, args ...interface{}) {
	if currentLevel >= INFO {
		logger.Printf("[INFO] "+format, args...)
	}
}

func Debug(format string, args ...interface{}) {
	if currentLevel >= DEBUG {
		logger.Printf("[DEBUG] "+format, args...)
	}
}

func Printf(format string, args ...interface{}) {
	fmt.Printf(format, args...)
}