package interactive

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

type Choice struct {
	Key         string
	Description string
}

func Confirm(message string) bool {
	fmt.Printf("%s [y/N]: ", message)
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	response := strings.ToLower(strings.TrimSpace(scanner.Text()))
	return response == "y" || response == "yes"
}

func PromptFileAction(filename, encoding string, confidence int) string {
	fmt.Printf("\n📄 %s\n", filename)
	fmt.Printf("   Detected: %s (confidence: %d%%)\n", encoding, confidence)
	fmt.Println("   c) Convert to UTF-8")
	fmt.Println("   s) Skip this file")
	fmt.Println("   p) Preview changes")
	fmt.Println("   q) Quit interactive mode")
	fmt.Print("Choice [c/s/p/q]: ")
	
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	input := strings.ToLower(strings.TrimSpace(scanner.Text()))
	
	switch input {
	case "c", "convert":
		return "convert"
	case "s", "skip", "":
		return "skip"
	case "p", "preview":
		return "preview"
	case "q", "quit":
		return "quit"
	default:
		return "skip"
	}
}