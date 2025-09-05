package codec

import (
	"fmt"
	"io"
	"unicode/utf8"

	"golang.org/x/text/encoding"
	"golang.org/x/text/transform"
)

// ConvertFromUTF8Stream converts UTF-8 to target encoding
func ConvertFromUTF8Stream(r io.Reader, to string) (io.Reader, string, error) {
	if to == "" || to == "utf-8" {
		return r, "utf-8", nil
	}

	enc, ok := EncodingFromName(to)
	if !ok {
		return nil, "", fmt.Errorf("unsupported target encoding: %s", to)
	}

	encoder := enc.NewEncoder()
	return transform.NewReader(r, encoder), fmt.Sprintf("utf-8->%s", to), nil
}

// ValidateUTF8ToISO validates if UTF-8 text can be safely converted to ISO-8859-1
func ValidateUTF8ToISO(text string) (bool, []rune, int) {
	if !utf8.ValidString(text) {
		return false, nil, 0
	}

	var invalidRunes []rune
	validCount := 0
	totalRunes := 0

	for _, r := range text {
		totalRunes++
		if r <= 0xFF { // Can be represented in ISO-8859-1
			validCount++
		} else {
			invalidRunes = append(invalidRunes, r)
		}
	}

	canConvert := len(invalidRunes) == 0
	compatibility := int(float64(validCount) / float64(totalRunes) * 100)

	return canConvert, invalidRunes, compatibility
}

// SuggestAlternatives suggests ISO-8859-1 compatible alternatives for Unicode chars
func SuggestAlternatives(r rune) string {
	alternatives := map[rune]string{
		// Smart quotes to regular quotes
		'"': `"`, '"': `"`, ''': `'`, ''': `'`,
		// Em/en dashes to hyphens
		'—': `-`, '–': `-`,
		// Ellipsis to three dots
		'…': `...`,
		// Trademark/copyright symbols
		'™': `(TM)`, '®': `(R)`, '©': `(C)`,
		// Currency symbols
		'€': `EUR`, '£': `GBP`, '¥': `JPY`,
		// Math symbols
		'×': `x`, '÷': `/`, '±': `+/-`,
		// Arrows
		'→': `->`, '←': `<-`, '↑': `^`, '↓': `v`,
		// Bullets
		'•': `*`, '◦': `-`, '▪': `*`,
	}

	if alt, exists := alternatives[r]; exists {
		return alt
	}

	// For other Unicode chars, suggest removal or transliteration
	if r > 0xFF {
		return fmt.Sprintf("[U+%04X]", r)
	}

	return string(r)
}

// PreprocessForISO prepares UTF-8 text for ISO-8859-1 conversion
func PreprocessForISO(text string, autoFix bool) (string, []string, bool) {
	canConvert, invalidRunes, _ := ValidateUTF8ToISO(text)
	
	if canConvert {
		return text, nil, true
	}

	if !autoFix {
		var warnings []string
		for _, r := range invalidRunes {
			warnings = append(warnings, fmt.Sprintf("Character '%c' (U+%04X) cannot be represented in ISO-8859-1", r, r))
		}
		return text, warnings, false
	}

	// Auto-fix by replacing problematic characters
	result := text
	var applied []string

	for _, r := range invalidRunes {
		alt := SuggestAlternatives(r)
		if alt != string(r) {
			oldStr := string(r)
			result = replaceAll(result, oldStr, alt)
			applied = append(applied, fmt.Sprintf("'%c' → '%s'", r, alt))
		}
	}

	return result, applied, true
}

func replaceAll(text, old, new string) string {
	// Simple replacement - could be optimized
	for i := 0; i < len(text); {
		if i+len(old) <= len(text) && text[i:i+len(old)] == old {
			text = text[:i] + new + text[i+len(old):]
			i += len(new)
		} else {
			i++
		}
	}
	return text
}