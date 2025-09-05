package config

import (
	"encoding/json"
	"os"
	"path/filepath"
)

type Preset struct {
	Name        string   `json:"name"`
	Extensions  []string `json:"extensions"`
	ExcludeDirs []string `json:"exclude_dirs"`
	Backup      bool     `json:"backup"`
	StripBOM    bool     `json:"strip_bom"`
	FixMojibake bool     `json:"fix_mojibake"`
}

var DefaultPresets = map[string]Preset{
	"java": {
		Name:        "Java Project",
		Extensions:  []string{".java", ".properties", ".xml"},
		ExcludeDirs: []string{"target", "build", ".git", ".idea"},
		Backup:      true,
		StripBOM:    true,
		FixMojibake: true,
	},
	"web": {
		Name:        "Web Project",
		Extensions:  []string{".html", ".css", ".js", ".ts", ".json"},
		ExcludeDirs: []string{"node_modules", "dist", "build", ".git"},
		Backup:      true,
		StripBOM:    true,
		FixMojibake: true,
	},
	"docs": {
		Name:        "Documentation",
		Extensions:  []string{".md", ".txt", ".rst"},
		ExcludeDirs: []string{".git", "_build", "node_modules"},
		Backup:      true,
		StripBOM:    true,
		FixMojibake: true,
	},
	"go": {
		Name:        "Go Project",
		Extensions:  []string{".go", ".mod", ".sum"},
		ExcludeDirs: []string{"vendor", ".git", "bin"},
		Backup:      true,
		StripBOM:    true,
		FixMojibake: true,
	},
	"legacy": {
		Name:        "Legacy System (UTF-8 → ISO-8859-1)",
		Extensions:  []string{".txt", ".csv", ".dat"},
		ExcludeDirs: []string{".git", "backup"},
		Backup:      true,
		StripBOM:    true,
		FixMojibake: false,
	},
}

func LoadPreset(name string) (Preset, bool) {
	preset, exists := DefaultPresets[name]
	return preset, exists
}

func ListPresets() []string {
	var names []string
	for name := range DefaultPresets {
		names = append(names, name)
	}
	return names
}