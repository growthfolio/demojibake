package reports

import (
	"fmt"
	"html/template"
	"os"
	"strings"
	"time"
)

type FileResult struct {
	Path       string
	Status     string
	From       string
	To         string
	Confidence int
	Applied    string
	Error      string
}

type Report struct {
	Title       string
	GeneratedAt time.Time
	Summary     Summary
	Files       []FileResult
}

type Summary struct {
	Total     int
	Converted int
	Skipped   int
	Errors    int
	NonUTF8   int
}

const htmlTemplate = `<!DOCTYPE html>
<html>
<head>
    <title>{{.Title}}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
        .summary { display: flex; gap: 20px; margin: 20px 0; }
        .stat { background: #e3f2fd; padding: 15px; border-radius: 5px; text-align: center; }
        .stat-value { font-size: 24px; font-weight: bold; color: #1976d2; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #f5f5f5; }
        .status-ok { color: #4caf50; }
        .status-fix { color: #2196f3; }
        .status-warn { color: #ff9800; }
        .status-error { color: #f44336; }
        .status-skip { color: #9e9e9e; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{.Title}}</h1>
        <p>Generated: {{.GeneratedAt.Format "2006-01-02 15:04:05"}}</p>
    </div>
    
    <div class="summary">
        <div class="stat">
            <div class="stat-value">{{.Summary.Total}}</div>
            <div>Total Files</div>
        </div>
        <div class="stat">
            <div class="stat-value">{{.Summary.Converted}}</div>
            <div>Converted</div>
        </div>
        <div class="stat">
            <div class="stat-value">{{.Summary.NonUTF8}}</div>
            <div>Non-UTF8</div>
        </div>
        <div class="stat">
            <div class="stat-value">{{.Summary.Errors}}</div>
            <div>Errors</div>
        </div>
    </div>
    
    <table>
        <thead>
            <tr>
                <th>Status</th>
                <th>File</th>
                <th>From</th>
                <th>Confidence</th>
                <th>Applied</th>
            </tr>
        </thead>
        <tbody>
            {{range .Files}}
            <tr>
                <td class="status-{{.Status | lower}}">{{.Status}}</td>
                <td>{{.Path}}</td>
                <td>{{.From}}</td>
                <td>{{.Confidence}}%</td>
                <td>{{.Applied}}</td>
            </tr>
            {{end}}
        </tbody>
    </table>
</body>
</html>`

func GenerateHTMLReport(report Report, filename string) error {
	tmpl, err := template.New("report").Funcs(template.FuncMap{
		"lower": strings.ToLower,
	}).Parse(htmlTemplate)
	if err != nil {
		return err
	}
	
	file, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer file.Close()
	
	return tmpl.Execute(file, report)
}