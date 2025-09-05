package codec

import (
	"fmt"
	"io"
	"regexp"
	"strings"
	"unicode/utf8"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/charmap"
	"golang.org/x/text/transform"
)

var encodingMap = map[string]encoding.Encoding{
	"iso-8859-1":  charmap.ISO8859_1,
	"iso-8859-2":  charmap.ISO8859_2,
	"iso-8859-3":  charmap.ISO8859_3,
	"iso-8859-4":  charmap.ISO8859_4,
	"iso-8859-5":  charmap.ISO8859_5,
	"iso-8859-6":  charmap.ISO8859_6,
	"iso-8859-7":  charmap.ISO8859_7,
	"iso-8859-8":  charmap.ISO8859_8,
	"iso-8859-9":  charmap.ISO8859_9,
	"iso-8859-10": charmap.ISO8859_10,
	"iso-8859-13": charmap.ISO8859_13,
	"iso-8859-14": charmap.ISO8859_14,
	"iso-8859-15": charmap.ISO8859_15,
	"iso-8859-16": charmap.ISO8859_16,
	"windows-1252": charmap.Windows1252,
	"macintosh":   charmap.Macintosh,
	"cp850":       charmap.CodePage850,
}

var mojibakePatterns = []*regexp.Regexp{
	regexp.MustCompile(`Ã[©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ]`),
	regexp.MustCompile(`â€[œžŸ ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿]`),
	regexp.MustCompile(`Â[  ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿]`),
}

func EncodingFromName(name string) (encoding.Encoding, bool) {
	enc, ok := encodingMap[strings.ToLower(name)]
	return enc, ok
}

func ConvertToUTF8Stream(r io.Reader, from string) (io.Reader, string, error) {
	if from == "" || from == "utf-8" {
		return r, "utf-8", nil
	}

	enc, ok := EncodingFromName(from)
	if !ok {
		return nil, "", fmt.Errorf("unsupported encoding: %s", from)
	}

	decoder := enc.NewDecoder()
	return transform.NewReader(r, decoder), fmt.Sprintf("%s->utf-8", from), nil
}

func TryLatin1RoundTrip(s string) (fixed string, applied string, ok bool) {
	if !hasMojibakePatterns(s) {
		return s, "", false
	}

	// Try latin1 round-trip
	var bytes []byte
	for _, r := range s {
		if r > 0xFF {
			return s, "", false // Can't be latin1 mojibake
		}
		bytes = append(bytes, byte(r))
	}

	if !utf8.Valid(bytes) {
		return s, "", false
	}

	candidate := string(bytes)
	if scoreText(candidate) > scoreText(s) {
		return candidate, "mojibake-fix", true
	}

	return s, "", false
}

func hasMojibakePatterns(s string) bool {
	for _, pattern := range mojibakePatterns {
		if pattern.MatchString(s) {
			return true
		}
	}
	return false
}

func scoreText(s string) int {
	score := 0
	replacementCount := strings.Count(s, "\uFFFD")
	score -= replacementCount * 3

	for _, r := range s {
		if r >= 32 && r <= 126 { // ASCII printable
			score += 2
		} else if r == '\t' || r == '\n' || r == '\r' || r == ' ' {
			score += 1
		} else if r > 126 && r != '\uFFFD' {
			score += 1
		}
	}

	// Penalize mojibake patterns
	for _, pattern := range mojibakePatterns {
		matches := pattern.FindAllString(s, -1)
		score -= len(matches) * 2
	}

	return score
}