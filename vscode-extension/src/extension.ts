import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

interface EncodingResult {
    path: string;
    status: string;
    from: string;
    confidence: number;
    applied?: string;
    error?: string;
}

class DemojibakelizadorProvider implements vscode.TreeDataProvider<EncodingIssue> {
    private _onDidChangeTreeData: vscode.EventEmitter<EncodingIssue | undefined | null | void> = new vscode.EventEmitter<EncodingIssue | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<EncodingIssue | undefined | null | void> = this._onDidChangeTreeData.event;

    private issues: EncodingIssue[] = [];

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: EncodingIssue): vscode.TreeItem {
        return element;
    }

    getChildren(element?: EncodingIssue): Thenable<EncodingIssue[]> {
        if (!element) {
            return Promise.resolve(this.issues);
        }
        return Promise.resolve([]);
    }

    updateIssues(issues: EncodingIssue[]) {
        this.issues = issues;
        this.refresh();
        vscode.commands.executeCommand('setContext', 'demojibakelizador.hasIssues', issues.length > 0);
    }
}

class EncodingIssue extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly filePath: string,
        public readonly encoding: string,
        public readonly confidence: number,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState
    ) {
        super(label, collapsibleState);
        this.tooltip = `${this.filePath}\nEncoding: ${this.encoding} (${this.confidence}% confidence)`;
        this.description = `${this.encoding} (${this.confidence}%)`;
        this.contextValue = 'encodingIssue';
        this.command = {
            command: 'vscode.open',
            title: 'Open File',
            arguments: [vscode.Uri.file(this.filePath)]
        };
        
        // Set icon based on severity
        if (this.confidence > 80) {
            this.iconPath = new vscode.ThemeIcon('warning', new vscode.ThemeColor('problemsWarningIcon.foreground'));
        } else {
            this.iconPath = new vscode.ThemeIcon('error', new vscode.ThemeColor('problemsErrorIcon.foreground'));
        }
    }
}

let statusBarItem: vscode.StatusBarItem;
let treeDataProvider: DemojibakelizadorProvider;

export function activate(context: vscode.ExtensionContext) {
    console.log('Demojibakelizador extension is now active!');

    // Initialize tree data provider
    treeDataProvider = new DemojibakelizadorProvider();
    vscode.window.registerTreeDataProvider('demojibakelizadorView', treeDataProvider);

    // Create status bar item
    statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    statusBarItem.command = 'demojibakelizador.detectCurrentFile';
    context.subscriptions.push(statusBarItem);

    // Register commands
    const commands = [
        vscode.commands.registerCommand('demojibakelizador.fixCurrentFile', fixCurrentFile),
        vscode.commands.registerCommand('demojibakelizador.detectCurrentFile', detectCurrentFile),
        vscode.commands.registerCommand('demojibakelizador.scanWorkspace', scanWorkspace),
        vscode.commands.registerCommand('demojibakelizador.convertToISO', convertToISO),
        vscode.commands.registerCommand('demojibakelizador.showReport', showReport),
        vscode.commands.registerCommand('demojibakelizador.openSettings', openSettings)
    ];

    context.subscriptions.push(...commands);

    // Auto-detect on file open
    vscode.workspace.onDidOpenTextDocument(onDocumentOpen);
    vscode.window.onDidChangeActiveTextEditor(onActiveEditorChange);

    // Initial detection for current file
    if (vscode.window.activeTextEditor) {
        detectFileEncoding(vscode.window.activeTextEditor.document);
    }
}

async function onDocumentOpen(document: vscode.TextDocument) {
    const config = vscode.workspace.getConfiguration('demojibakelizador');
    if (config.get('autoDetectOnOpen')) {
        await detectFileEncoding(document);
    }
}

async function onActiveEditorChange(editor: vscode.TextEditor | undefined) {
    if (editor) {
        await detectFileEncoding(editor.document);
    } else {
        updateStatusBar('', '');
    }
}

async function detectFileEncoding(document: vscode.TextDocument) {
    if (!shouldProcessFile(document.fileName)) {
        updateStatusBar('', '');
        return;
    }

    try {
        const result = await runDemojibake(['-path', document.fileName, '-detect']);
        const lines = result.stdout.split('\n').filter(line => line.trim());
        
        if (lines.length > 0) {
            const lastLine = lines[lines.length - 1];
            if (lastLine.includes('|')) {
                const parts = lastLine.split('|');
                if (parts.length >= 3) {
                    const status = parts[0].trim();
                    const fromPart = parts[2].trim();
                    const encoding = fromPart.split('=')[1]?.split(' ')[0] || 'unknown';
                    
                    updateStatusBar(status, encoding);
                    return;
                }
            }
        }
        
        updateStatusBar('OK', 'utf-8');
    } catch (error) {
        updateStatusBar('ERROR', 'unknown');
    }
}

function updateStatusBar(status: string, encoding: string) {
    const config = vscode.workspace.getConfiguration('demojibakelizador');
    if (!config.get('showStatusBar')) {
        statusBarItem.hide();
        return;
    }

    if (!encoding) {
        statusBarItem.hide();
        return;
    }

    const icon = getStatusIcon(status);
    statusBarItem.text = `${icon} ${encoding.toUpperCase()}`;
    statusBarItem.tooltip = `Encoding: ${encoding} (${status})`;
    
    // Set color based on status
    if (status === 'WARN') {
        statusBarItem.color = new vscode.ThemeColor('problemsWarningIcon.foreground');
    } else if (status === 'ERROR') {
        statusBarItem.color = new vscode.ThemeColor('problemsErrorIcon.foreground');
    } else {
        statusBarItem.color = undefined;
    }
    
    statusBarItem.show();
}

function getStatusIcon(status: string): string {
    switch (status) {
        case 'OK': return '✅';
        case 'WARN': return '⚠️';
        case 'ERROR': return '❌';
        case 'FIX': return '🔧';
        default: return 'ℹ️';
    }
}

async function fixCurrentFile() {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showErrorMessage('No active file to fix');
        return;
    }

    const filePath = editor.document.fileName;
    if (!shouldProcessFile(filePath)) {
        vscode.window.showWarningMessage('File type not supported for encoding fix');
        return;
    }

    try {
        await vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Fixing encoding...",
            cancellable: false
        }, async () => {
            const config = vscode.workspace.getConfiguration('demojibakelizador');
            const args = [
                '-path', filePath,
                '-in-place'
            ];

            if (config.get('autoBackup')) {
                args.push('-backup-suffix', config.get('backupSuffix') as string);
            }

            if (config.get('fixMojibake')) {
                args.push('-fix-mojibake');
            }

            if (config.get('stripBOM')) {
                args.push('-strip-bom');
            }

            const result = await runDemojibake(args);
            
            // Reload the file in VS Code
            await vscode.commands.executeCommand('workbench.action.files.revert');
            
            vscode.window.showInformationMessage('✅ Encoding fixed successfully!');
            
            // Re-detect encoding
            await detectFileEncoding(editor.document);
        });
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to fix encoding: ${error}`);
    }
}

async function detectCurrentFile() {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showErrorMessage('No active file to detect');
        return;
    }

    const filePath = editor.document.fileName;
    if (!shouldProcessFile(filePath)) {
        vscode.window.showWarningMessage('File type not supported for encoding detection');
        return;
    }

    try {
        const result = await runDemojibake(['-path', filePath, '-detect', '-v']);
        
        // Show result in output channel
        const outputChannel = vscode.window.createOutputChannel('Demojibakelizador');
        outputChannel.clear();
        outputChannel.appendLine('=== Encoding Detection Result ===');
        outputChannel.appendLine(result.stdout);
        if (result.stderr) {
            outputChannel.appendLine('=== Errors ===');
            outputChannel.appendLine(result.stderr);
        }
        outputChannel.show();
        
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to detect encoding: ${error}`);
    }
}

async function scanWorkspace() {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
        vscode.window.showErrorMessage('No workspace folder open');
        return;
    }

    const folder = workspaceFolders[0];
    
    try {
        await vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Scanning workspace for encoding issues...",
            cancellable: false
        }, async () => {
            const config = vscode.workspace.getConfiguration('demojibakelizador');
            const extensions = config.get('fileExtensions') as string[];
            const excludeDirs = config.get('excludeDirectories') as string[];
            
            const args = [
                '-path', folder.uri.fsPath,
                '-detect',
                '-ext', extensions.join(','),
                '-exclude-dirs', excludeDirs.join(','),
                '-v'
            ];

            const result = await runDemojibake(args);
            
            // Parse results and update tree view
            const issues = parseDetectionResults(result.stdout);
            treeDataProvider.updateIssues(issues);
            
            if (issues.length > 0) {
                vscode.window.showWarningMessage(`Found ${issues.length} files with encoding issues. Check the Encoding Issues panel.`);
            } else {
                vscode.window.showInformationMessage('✅ No encoding issues found in workspace!');
            }
        });
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to scan workspace: ${error}`);
    }
}

async function convertToISO() {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showErrorMessage('No active file to convert');
        return;
    }

    const choice = await vscode.window.showQuickPick([
        { label: '🔍 Validate Compatibility', value: 'validate' },
        { label: '🔄 Convert to ISO-8859-1', value: 'convert' },
        { label: '🔧 Convert with Auto-fix', value: 'autofix' }
    ], { placeHolder: 'Choose conversion mode' });

    if (!choice) return;

    const filePath = editor.document.fileName;
    
    try {
        await vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Converting to ISO-8859-1...",
            cancellable: false
        }, async () => {
            const args = ['-path', filePath, '-to', 'iso-8859-1'];
            
            if (choice.value === 'validate') {
                args.push('-validate-only');
            } else if (choice.value === 'autofix') {
                args.push('-auto-fix');
            }
            
            if (choice.value !== 'validate') {
                args.push('-in-place', '-backup-suffix', '.utf8');
            }

            const result = await runDemojibake(args);
            
            if (choice.value === 'validate') {
                const outputChannel = vscode.window.createOutputChannel('Demojibakelizador');
                outputChannel.clear();
                outputChannel.appendLine('=== ISO-8859-1 Compatibility Check ===');
                outputChannel.appendLine(result.stdout);
                outputChannel.show();
            } else {
                await vscode.commands.executeCommand('workbench.action.files.revert');
                vscode.window.showInformationMessage('✅ File converted to ISO-8859-1!');
            }
        });
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to convert: ${error}`);
    }
}

async function showReport() {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
        vscode.window.showErrorMessage('No workspace folder open');
        return;
    }

    const folder = workspaceFolders[0];
    const reportPath = path.join(folder.uri.fsPath, 'encoding-report.html');
    
    try {
        const config = vscode.workspace.getConfiguration('demojibakelizador');
        const extensions = config.get('fileExtensions') as string[];
        
        const args = [
            '-path', folder.uri.fsPath,
            '-detect',
            '-ext', extensions.join(','),
            '-report-format', 'html',
            '-report-output', reportPath
        ];

        await runDemojibake(args);
        
        // Open report in VS Code
        const reportUri = vscode.Uri.file(reportPath);
        await vscode.commands.executeCommand('vscode.open', reportUri);
        
        vscode.window.showInformationMessage('📊 Report generated and opened!');
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to generate report: ${error}`);
    }
}

function openSettings() {
    vscode.commands.executeCommand('workbench.action.openSettings', 'demojibakelizador');
}

function parseDetectionResults(output: string): EncodingIssue[] {
    const issues: EncodingIssue[] = [];
    const lines = output.split('\n');
    
    for (const line of lines) {
        if (line.includes('WARN') && line.includes('|')) {
            const parts = line.split('|');
            if (parts.length >= 3) {
                const filePath = parts[1].trim();
                const fromPart = parts[2].trim();
                const encoding = fromPart.split('=')[1]?.split(' ')[0] || 'unknown';
                const confMatch = fromPart.match(/conf=(\d+)/);
                const confidence = confMatch ? parseInt(confMatch[1]) : 0;
                
                const fileName = path.basename(filePath);
                const issue = new EncodingIssue(
                    fileName,
                    filePath,
                    encoding,
                    confidence,
                    vscode.TreeItemCollapsibleState.None
                );
                issues.push(issue);
            }
        }
    }
    
    return issues;
}

async function runDemojibake(args: string[]): Promise<{ stdout: string; stderr: string }> {
    const config = vscode.workspace.getConfiguration('demojibakelizador');
    let binaryPath = config.get('binaryPath') as string;
    
    if (!binaryPath) {
        // Try to find binary in common locations
        const possiblePaths = [
            'demojibake',
            'demojibake.exe',
            path.join(process.cwd(), 'dist', 'demojibake.exe'),
            path.join(process.cwd(), 'dist', 'demojibake')
        ];
        
        for (const testPath of possiblePaths) {
            try {
                await execAsync(`"${testPath}" -h`);
                binaryPath = testPath;
                break;
            } catch {
                continue;
            }
        }
        
        if (!binaryPath) {
            throw new Error('Demojibake binary not found. Please set the path in settings.');
        }
    }
    
    const command = `"${binaryPath}" ${args.map(arg => `"${arg}"`).join(' ')}`;
    return await execAsync(command);
}

function shouldProcessFile(filePath: string): boolean {
    const config = vscode.workspace.getConfiguration('demojibakelizador');
    const extensions = config.get('fileExtensions') as string[];
    const ext = path.extname(filePath).toLowerCase();
    
    return extensions.includes(ext);
}

export function deactivate() {
    if (statusBarItem) {
        statusBarItem.dispose();
    }
}