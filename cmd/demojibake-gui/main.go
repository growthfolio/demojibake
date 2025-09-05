package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"

	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"
	fyne "fyne.io/fyne/v2"
)

type GUI struct {
	app    *app.App
	window fyne.Window
	
	pathEntry      *widget.Entry
	modeSelect     *widget.RadioGroup
	fromSelect     *widget.Select
	extEntry       *widget.Entry
	recursiveCheck *widget.Check
	inPlaceCheck   *widget.Check
	dryRunCheck    *widget.Check
	mojibakeCheck  *widget.Check
	stripBOMCheck  *widget.Check
	addBOMCheck    *widget.Check
	failCheck      *widget.Check
	backupEntry    *widget.Entry
	workersEntry   *widget.Entry
	
	logArea    *widget.Entry
	progress   *widget.ProgressBarInfinite
	runButton  *widget.Button
	cancelFunc context.CancelFunc
}

func main() {
	myApp := app.New()
	myApp.SetIcon(nil) // TODO: Load icon from assets/icons/app.png
	
	myWindow := myApp.NewWindow("Demojibakelizador")
	myWindow.Resize(fyne.NewSize(760, 560))
	
	gui := &GUI{app: &myApp}
	gui.setupUI(myWindow)
	
	myWindow.ShowAndRun()
}

func (g *GUI) setupUI(w fyne.Window) {
	// Path selection
	g.pathEntry = widget.NewEntry()
	g.pathEntry.SetPlaceHolder("Selecione arquivo ou pasta...")
	
	fileButton := widget.NewButton("Arquivo", func() {
		dialog.ShowFileOpen(func(reader fyne.URIReadCloser) {
			if reader != nil {
				g.pathEntry.SetText(reader.URI().Path())
				reader.Close()
			}
		}, w)
	})
	
	folderButton := widget.NewButton("Pasta", func() {
		dialog.ShowFolderOpen(func(uri fyne.ListableURI) {
			if uri != nil {
				g.pathEntry.SetText(uri.Path())
			}
		}, w)
	})
	
	pathContainer := container.NewBorder(nil, nil, nil, 
		container.NewHBox(fileButton, folderButton), g.pathEntry)
	
	// Mode selection
	g.modeSelect = widget.NewRadioGroup([]string{
		"Detectar (não altera)",
		"Converter p/ UTF-8",
	}, nil)
	g.modeSelect.SetSelected("Converter p/ UTF-8")
	
	// Encoding selection
	g.fromSelect = widget.NewSelect([]string{
		"Auto",
		"iso-8859-1",
		"windows-1252", 
		"iso-8859-15",
		"macintosh",
		"cp850",
	}, nil)
	g.fromSelect.SetSelected("Auto")
	
	// Extensions
	g.extEntry = widget.NewEntry()
	g.extEntry.SetText(".txt,.md,.java,.xml,.properties,.csv,.html,.js,.ts,.go")
	
	// Checkboxes
	g.recursiveCheck = widget.NewCheck("Recursivo", nil)
	g.recursiveCheck.SetChecked(true)
	
	g.inPlaceCheck = widget.NewCheck("In-place", nil)
	g.inPlaceCheck.SetChecked(true)
	
	g.dryRunCheck = widget.NewCheck("Dry-run", nil)
	
	g.mojibakeCheck = widget.NewCheck("Fix Mojibake", nil)
	g.mojibakeCheck.SetChecked(true)
	
	g.stripBOMCheck = widget.NewCheck("Remover BOM", nil)
	g.stripBOMCheck.SetChecked(true)
	
	g.addBOMCheck = widget.NewCheck("Adicionar BOM", nil)
	g.addBOMCheck.OnChanged = func(checked bool) {
		if checked {
			g.stripBOMCheck.SetChecked(false)
		}
	}
	g.stripBOMCheck.OnChanged = func(checked bool) {
		if checked {
			g.addBOMCheck.SetChecked(false)
		}
	}
	
	g.failCheck = widget.NewCheck("Falhar se não-UTF-8 (CI)", nil)
	
	// Text entries
	g.backupEntry = widget.NewEntry()
	g.backupEntry.SetText(".bak")
	g.backupEntry.SetPlaceHolder("Sufixo de backup")
	
	g.workersEntry = widget.NewEntry()
	g.workersEntry.SetText(strconv.Itoa(runtime.NumCPU() / 2))
	g.workersEntry.SetPlaceHolder("Workers")
	
	// Progress and buttons
	g.progress = widget.NewProgressBarInfinite()
	g.progress.Hide()
	
	g.runButton = widget.NewButton("Executar", g.runProcess)
	cancelButton := widget.NewButton("Cancelar", g.cancelProcess)
	
	// Log area
	g.logArea = widget.NewMultiLineEntry()
	g.logArea.SetPlaceHolder("Logs aparecerão aqui...")
	
	// Layout
	form := container.NewVBox(
		widget.NewLabel("Caminho:"),
		pathContainer,
		
		widget.NewSeparator(),
		
		widget.NewLabel("Modo:"),
		g.modeSelect,
		
		widget.NewLabel("Encoding origem:"),
		g.fromSelect,
		
		widget.NewLabel("Extensões:"),
		g.extEntry,
		
		widget.NewSeparator(),
		
		container.NewGridWithColumns(2,
			g.recursiveCheck, g.inPlaceCheck,
			g.dryRunCheck, g.mojibakeCheck,
			g.stripBOMCheck, g.addBOMCheck,
		),
		g.failCheck,
		
		widget.NewSeparator(),
		
		container.NewGridWithColumns(2,
			widget.NewLabel("Sufixo backup:"), g.backupEntry,
			widget.NewLabel("Workers:"), g.workersEntry,
		),
		
		widget.NewSeparator(),
		
		g.progress,
		container.NewGridWithColumns(2, g.runButton, cancelButton),
	)
	
	content := container.NewBorder(
		form, nil, nil, nil,
		container.NewScroll(g.logArea),
	)
	
	w.SetContent(content)
}

func (g *GUI) runProcess() {
	if g.pathEntry.Text == "" {
		dialog.ShowError(fmt.Errorf("selecione um arquivo ou pasta"), g.window)
		return
	}
	
	// Build command arguments
	args := g.buildArgs()
	
	// Find demojibake binary
	binaryPath := g.findBinary()
	if binaryPath == "" {
		dialog.ShowError(fmt.Errorf("não foi possível encontrar o binário demojibake"), g.window)
		return
	}
	
	// Setup context for cancellation
	ctx, cancel := context.WithCancel(context.Background())
	g.cancelFunc = cancel
	
	// Update UI
	g.runButton.SetText("Executando...")
	g.runButton.Disable()
	g.progress.Show()
	g.logArea.SetText("")
	
	// Run command
	go func() {
		defer func() {
			g.runButton.SetText("Executar")
			g.runButton.Enable()
			g.progress.Hide()
			g.cancelFunc = nil
		}()
		
		cmd := exec.CommandContext(ctx, binaryPath, args...)
		
		stdout, err := cmd.StdoutPipe()
		if err != nil {
			g.appendLog(fmt.Sprintf("Erro ao criar pipe: %v\n", err))
			return
		}
		
		stderr, err := cmd.StderrPipe()
		if err != nil {
			g.appendLog(fmt.Sprintf("Erro ao criar pipe stderr: %v\n", err))
			return
		}
		
		if err := cmd.Start(); err != nil {
			g.appendLog(fmt.Sprintf("Erro ao iniciar comando: %v\n", err))
			return
		}
		
		// Read output
		go g.readOutput(stdout, "STDOUT")
		go g.readOutput(stderr, "STDERR")
		
		if err := cmd.Wait(); err != nil {
			if ctx.Err() == context.Canceled {
				g.appendLog("Processo cancelado pelo usuário\n")
			} else {
				g.appendLog(fmt.Sprintf("Processo terminou com erro: %v\n", err))
			}
		} else {
			g.appendLog("Processo concluído com sucesso\n")
		}
	}()
}

func (g *GUI) cancelProcess() {
	if g.cancelFunc != nil {
		g.cancelFunc()
	}
}

func (g *GUI) buildArgs() []string {
	var args []string
	
	// Path
	args = append(args, "-path", g.pathEntry.Text)
	
	// Mode
	if g.modeSelect.Selected == "Detectar (não altera)" {
		args = append(args, "-detect")
	}
	
	// From encoding
	if g.fromSelect.Selected != "Auto" {
		args = append(args, "-from", g.fromSelect.Selected)
	}
	
	// Extensions
	if g.extEntry.Text != "" {
		args = append(args, "-ext", g.extEntry.Text)
	}
	
	// Boolean flags
	if !g.recursiveCheck.Checked {
		args = append(args, "-recursive=false")
	}
	
	if g.inPlaceCheck.Checked {
		args = append(args, "-in-place")
	}
	
	if g.dryRunCheck.Checked {
		args = append(args, "-dry-run")
	}
	
	if !g.mojibakeCheck.Checked {
		args = append(args, "-fix-mojibake=false")
	}
	
	if !g.stripBOMCheck.Checked {
		args = append(args, "-strip-bom=false")
	}
	
	if g.addBOMCheck.Checked {
		args = append(args, "-add-bom")
	}
	
	if g.failCheck.Checked {
		args = append(args, "-fail-if-not-utf8")
	}
	
	// Text values
	if g.backupEntry.Text != "" {
		args = append(args, "-backup-suffix", g.backupEntry.Text)
	}
	
	if g.workersEntry.Text != "" {
		args = append(args, "-workers", g.workersEntry.Text)
	}
	
	return args
}

func (g *GUI) findBinary() string {
	// Try same directory as GUI executable
	if exePath, err := os.Executable(); err == nil {
		exeDir := filepath.Dir(exePath)
		binaryName := "demojibake"
		if runtime.GOOS == "windows" {
			binaryName += ".exe"
		}
		
		binaryPath := filepath.Join(exeDir, binaryName)
		if _, err := os.Stat(binaryPath); err == nil {
			return binaryPath
		}
	}
	
	// Try PATH
	binaryName := "demojibake"
	if runtime.GOOS == "windows" {
		binaryName += ".exe"
	}
	
	if path, err := exec.LookPath(binaryName); err == nil {
		return path
	}
	
	return ""
}

func (g *GUI) readOutput(pipe io.ReadCloser, prefix string) {
	defer pipe.Close()
	
	scanner := bufio.NewScanner(pipe)
	for scanner.Scan() {
		line := scanner.Text()
		g.appendLog(line + "\n")
	}
}

func (g *GUI) appendLog(text string) {
	current := g.logArea.Text
	g.logArea.SetText(current + text)
	
	// Auto-scroll to bottom (approximate)
	lines := strings.Count(g.logArea.Text, "\n")
	if lines > 100 {
		// Keep only last 100 lines
		allLines := strings.Split(g.logArea.Text, "\n")
		if len(allLines) > 100 {
			g.logArea.SetText(strings.Join(allLines[len(allLines)-100:], "\n"))
		}
	}
}