package codec

import (
	"strings"

	"github.com/saintfish/chardet"
)

func Detect(data []byte) (charset string, confidence int, language string, err error) {
	detector := chardet.NewTextDetector()
	result, err := detector.DetectBest(data)
	if err != nil {
		return "", 0, "", err
	}

	charset = strings.ToLower(result.Charset)
	confidence = int(result.Confidence)
	language = result.Language

	// Normalize common charset names
	switch charset {
	case "iso-8859-1", "latin1":
		charset = "iso-8859-1"
	case "windows-1252", "cp1252":
		charset = "windows-1252"
	case "utf-8", "utf8":
		charset = "utf-8"
	case "ascii":
		charset = "utf-8" // ASCII is valid UTF-8
	}

	return charset, confidence, language, nil
}