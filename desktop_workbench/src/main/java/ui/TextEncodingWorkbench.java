package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import core.TextEncodingAnalyzer;
import core.EncodingAnalysisResult;
import core.MojibakeProcessor;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.google.gson.Gson;

public class TextEncodingWorkbench extends Application {
    
    private static final Logger logger = Logger.getLogger(TextEncodingWorkbench.class.getName());
    private static final Gson gson = new Gson();
    private final TextEncodingAnalyzer characterEncodingEngine = TextEncodingAnalyzer.INSTANCE;
    
    // UI Components
    private Stage primaryStage;
    private TabPane workbenchTabs;
    private FileProcessingWorkspace processingWorkspace;
    private ApplicationStatusBar statusBar;
    
    // Application state
    private final ObservableList<EncodingAnalysisResult> analysisResults = FXCollections.observableArrayList();
    private final IntegerProperty filesProcessed = new SimpleIntegerProperty(0);
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final BooleanProperty isProcessing = new SimpleBooleanProperty(false);
    
    // Thread management
    private final ExecutorService executorService = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "DemojibakelizadorScheduler");
        t.setDaemon(true);
        return t;
    });
    
    @Override
    public void init() throws Exception {
        try {
            int status = characterEncodingEngine.InitializeEncodingEngine();
            if (status != 1) {
                throw new RuntimeException("Failed to initialize native library. Status: " + status);
            }
            logger.info("⚡ Text Encoding Workbench inicializado com sucesso");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro na inicialização", e);
            throw e;
        }
        
        ensureSingleInstance();
        loadConfiguration();
        preloadResources();
    }
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        stage.initStyle(StageStyle.UNDECORATED);
        
        BorderPane root = createMainLayout();
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add("/styles/professional_dark_theme.css");
        
        stage.setTitle("Text Encoding Workbench v2.0");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        
        stage.centerOnScreen();
        showWithAnimation(stage);
        
        setupSystemTray();
        startPerformanceMonitoring();
    }
    
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-container");
        
        HBox titleBar = createCustomTitleBar();
        root.setTop(titleBar);
        
        VBox topSection = new VBox();
        topSection.getChildren().addAll(
            createRibbonMenu(),
            createQuickAccessToolbar()
        );
        
        workbenchTabs = new TabPane();
        workbenchTabs.getStyleClass().add("main-tabs");
        workbenchTabs.getTabs().addAll(
            createFileProcessingTab(),
            createLanguageDictionaryTab(),
            createProcessingAnalyticsTab(),
            createWorkbenchSettingsTab()
        );
        
        statusBar = new ApplicationStatusBar();
        root.setBottom(statusBar);
        
        VBox centerContent = new VBox();
        centerContent.getChildren().addAll(topSection, workbenchTabs);
        root.setCenter(centerContent);
        
        return root;
    }
    
    private HBox createCustomTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(5, 10, 5, 10));
        
        Label logo = new Label("⬢");
        logo.getStyleClass().add("app-logo");
        
        // Solução bizarra #8: Smoother logo animation with both X and Y scale
        Timeline logoAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(logo.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(logo.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(1.5), 
                new KeyValue(logo.scaleXProperty(), 1.1, Interpolator.EASE_BOTH),
                new KeyValue(logo.scaleYProperty(), 1.1, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.seconds(3), 
                new KeyValue(logo.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(logo.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            )
        );
        logoAnimation.setCycleCount(Timeline.INDEFINITE);
        logoAnimation.setAutoReverse(false);
        
        // Solução bizarra #9: Delay start to avoid conflict with window animation
        Timeline delayedStart = new Timeline(new KeyFrame(Duration.seconds(1), e -> logoAnimation.play()));
        delayedStart.play();
        
        Label title = new Label("Text Encoding Workbench");
        title.getStyleClass().add("app-title");
        
        // Indicador de animações (aparece quando há problema)
        Label animStatus = new Label("🟢");
        animStatus.setTooltip(new Tooltip("Animações OK - Duplo clique em ⬜ para reset se necessário"));
        animStatus.setVisible(false); // Só aparece se houver problema
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button minimizeBtn = createWindowButton("⎯", e -> minimizeWithAnimation());
        Button maximizeBtn = createWindowButton("⬜", e -> toggleMaximizedWithAnimation());
        
        // Botão de emergência anti-pulso (duplo clique)
        maximizeBtn.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                // BOTÃO DE PÂNICO: Para TODAS as animações
                resetAllAnimations();
            } else {
                toggleMaximizedWithAnimation();
            }
        });
        
        Button closeBtn = createWindowButton("✕", e -> closeWithAnimation());
        closeBtn.getStyleClass().add("close-button");
        
        titleBar.getChildren().addAll(logo, title, animStatus, spacer, minimizeBtn, maximizeBtn, closeBtn);
        
        makeDraggable(titleBar);
        
        return titleBar;
    }
    
    private Tab createFileProcessingTab() {
        Tab tab = new Tab("⚡ PROCESSAMENTO");
        tab.setClosable(false);
        
        processingWorkspace = new FileProcessingWorkspace();
        tab.setContent(processingWorkspace);
        
        return tab;
    }
    
    private Tab createLanguageDictionaryTab() {
        Tab tab = new Tab("📚 DICIONÁRIO");
        tab.setClosable(false);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label statsLabel = new Label("Estatísticas do Dicionário");
        statsLabel.getStyleClass().add("section-title");
        
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefRowCount(10);
        
        Button refreshBtn = new Button("Atualizar Estatísticas");
        refreshBtn.setOnAction(e -> updateDictionaryStats(statsArea));
        
        content.getChildren().addAll(statsLabel, statsArea, refreshBtn);
        tab.setContent(content);
        
        return tab;
    }
    
    private Tab createProcessingAnalyticsTab() {
        Tab tab = new Tab("📊 ANALYTICS");
        tab.setClosable(false);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label analyticsLabel = new Label("Métricas de Performance");
        analyticsLabel.getStyleClass().add("section-title");
        
        // Métricas dinâmicas baseadas em dados reais
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(10);
        
        Label filesProcessedLabel = new Label();
        filesProcessedLabel.textProperty().bind(filesProcessed.asString());
        
        Label successRateLabel = new Label();
        successRateLabel.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (analysisResults.isEmpty()) return "0%";
                long successful = analysisResults.stream()
                    .mapToLong(r -> "success".equals(r.getStatus()) || "Processado".equals(r.getStatus()) ? 1 : 0)
                    .sum();
                double rate = (double) successful / analysisResults.size() * 100;
                return String.format("%.1f%%", rate);
            }, analysisResults)
        );
        
        Label avgTimeLabel = new Label();
        avgTimeLabel.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (analysisResults.isEmpty()) return "0ms";
                double avgTime = analysisResults.stream()
                    .mapToLong(EncodingAnalysisResult::getProcessingTime)
                    .average()
                    .orElse(0.0);
                return String.format("%.0fms", avgTime);
            }, analysisResults)
        );
        
        metricsGrid.add(new Label("Arquivos Processados:"), 0, 0);
        metricsGrid.add(filesProcessedLabel, 1, 0);
        
        metricsGrid.add(new Label("Taxa de Sucesso:"), 0, 1);
        metricsGrid.add(successRateLabel, 1, 1);
        
        metricsGrid.add(new Label("Tempo Médio:"), 0, 2);
        metricsGrid.add(avgTimeLabel, 1, 2);
        
        // Área de estatísticas detalhadas
        TextArea detailedStats = new TextArea();
        detailedStats.setEditable(false);
        detailedStats.setPrefRowCount(8);
        
        Button refreshStatsBtn = new Button("Atualizar Estatísticas Detalhadas");
        refreshStatsBtn.setOnAction(e -> {
            try {
                updateDetailedStats(detailedStats);
            } catch (Exception ex) {
                logger.severe("Erro ao atualizar estatísticas: " + ex.getMessage());
                showError("Erro", "Falha ao atualizar estatísticas: " + ex.getMessage());
            }
        });
        
        content.getChildren().addAll(analyticsLabel, metricsGrid, 
            new Label("Estatísticas Detalhadas:"), detailedStats, refreshStatsBtn);
        tab.setContent(content);
        
        return tab;
    }
    
    private void updateDetailedStats(TextArea statsArea) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estatísticas de Processamento ===\n");
        stats.append("Total de Arquivos: ").append(analysisResults.size()).append("\n");
        
        if (!analysisResults.isEmpty()) {
            long successful = analysisResults.stream()
                .mapToLong(r -> "success".equals(r.getStatus()) || "Processado".equals(r.getStatus()) ? 1 : 0)
                .sum();
            long failed = analysisResults.size() - successful;
            
            stats.append("Sucessos: ").append(successful).append("\n");
            stats.append("Falhas: ").append(failed).append("\n");
            
            double avgTime = analysisResults.stream()
                .mapToLong(EncodingAnalysisResult::getProcessingTime)
                .average()
                .orElse(0.0);
            stats.append("Tempo Médio: ").append(String.format("%.2fms", avgTime)).append("\n");
            
            double avgConfidence = analysisResults.stream()
                .mapToDouble(EncodingAnalysisResult::getConfidence)
                .average()
                .orElse(0.0);
            stats.append("Confiança Média: ").append(String.format("%.1f%%", avgConfidence * 100)).append("\n");
            
            stats.append("\n=== Últimos Resultados ===\n");
            analysisResults.stream()
                .limit(10)
                .forEach(result -> {
                    stats.append("Arquivo: ").append(new File(result.getPath()).getName())
                         .append(" - Status: ").append(result.getStatus())
                         .append(" - Encoding: ").append(result.getOriginalEncoding())
                         .append(" - Tempo: ").append(result.getProcessingTime()).append("ms")
                         .append(" - Confiança: ").append(String.format("%.1f%%", result.getConfidence() * 100)).append("\n");
                });
        }
        
        // Adiciona estatísticas do dicionário
        try {
            String dictionaryMetrics = characterEncodingEngine.RetrieveLanguageDictionaryMetrics();
            stats.append("\n=== Estatísticas do Dicionário ===\n");
            stats.append(dictionaryMetrics);
        } catch (Exception e) {
            stats.append("\nErro ao obter estatísticas do dicionário: ").append(e.getMessage());
        }
        
        Platform.runLater(() -> statsArea.setText(stats.toString()));
    }
    
    private Tab createWorkbenchSettingsTab() {
        Tab tab = new Tab("⚙️ CONFIGURAÇÕES");
        tab.setClosable(false);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        TitledPane generalSettings = new TitledPane("Geral", createGeneralSettings());
        TitledPane performanceSettings = new TitledPane("Performance", createPerformanceSettings());
        
        content.getChildren().addAll(generalSettings, performanceSettings);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    private class FileProcessingWorkspace extends VBox {
        private final TableView<EncodingAnalysisResult> resultsTable;
        private final ProgressIndicator progressIndicator;
        private final Label statusLabel;
        private final Button processButton;
        private final VBox dropZone;
        
        public FileProcessingWorkspace() {
            super(20);
            setPadding(new Insets(20));
            getStyleClass().add("processing-panel");
            
            dropZone = createDropZone();
            
            HBox controls = new HBox(10);
            controls.setAlignment(Pos.CENTER_LEFT);
            
            processButton = new Button("🚀 PROCESSAR ARQUIVOS");
            processButton.getStyleClass().add("primary-button");
            processButton.setOnAction(e -> selectAndProcessFiles());
            
            Button batchButton = new Button("⚡ LOTE AVANÇADO");
            batchButton.getStyleClass().add("secondary-button");
            batchButton.setOnAction(e -> openAdvancedBatchDialog());
            
            Button stopButton = new Button("⏹ PARAR");
            stopButton.getStyleClass().add("danger-button");
            stopButton.disableProperty().bind(isProcessing.not());
            stopButton.setOnAction(e -> stopProcessing());
            
            controls.getChildren().addAll(processButton, batchButton, stopButton);
            
            HBox progressBox = new HBox(15);
            progressBox.setAlignment(Pos.CENTER);
            
            progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            
            statusLabel = new Label("⚡ SISTEMA PRONTO - AGUARDANDO ARQUIVOS");
            statusLabel.getStyleClass().add("status-label");
            statusLabel.setStyle("-fx-text-fill: #2ea043; -fx-font-weight: bold;");
            
            Label filesLabel = new Label();
            filesLabel.textProperty().bind(
                javafx.beans.binding.Bindings.format("%d / %d arquivos processados", 
                filesProcessed, totalFiles)
            );
            
            progressBox.getChildren().addAll(progressIndicator, statusLabel, filesLabel);
            
            resultsTable = createResultsTable();
            
            getChildren().addAll(dropZone, controls, progressBox, resultsTable);
        }
        
        private VBox createDropZone() {
            VBox dropZone = new VBox(10);
            dropZone.setAlignment(Pos.CENTER);
            dropZone.setPrefHeight(150);
            dropZone.getStyleClass().add("drop-zone");
            
            Label dropLabel = new Label("⚡ ARRASTE ARQUIVOS AQUI ⚡");
            dropLabel.getStyleClass().add("drop-label");
            
            Label subLabel = new Label("Suporte: .txt, .log, .csv, .json");
            subLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
            
            Label dropIcon = new Label("⬢");
            dropIcon.setStyle("-fx-font-size: 64px; -fx-text-fill: linear-gradient(45deg, #58a6ff, #79c0ff);");
            dropIcon.getStyleClass().add("glow-animation");
            
            dropZone.getChildren().addAll(dropIcon, dropLabel, subLabel);
            
            dropZone.setOnDragOver(event -> {
                if (event.getGestureSource() != dropZone && 
                    event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    dropZone.getStyleClass().add("drop-zone-active");
                }
                event.consume();
            });
            
            dropZone.setOnDragExited(event -> {
                dropZone.getStyleClass().remove("drop-zone-active");
                event.consume();
            });
            
            dropZone.setOnDragDropped(event -> {
                var db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    try {
                        processFiles(db.getFiles());
                        success = true;
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Erro no processamento de arquivos", e);
                        showError("Erro", "Erro ao processar arquivos: " + e.getMessage());
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
            
            return dropZone;
        }
        
        private TableView<EncodingAnalysisResult> createResultsTable() {
            TableView<EncodingAnalysisResult> table = new TableView<>();
            table.getStyleClass().add("results-table");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            
            // Coluna Arquivo - 40% do espaço
            TableColumn<EncodingAnalysisResult, String> fileCol = new TableColumn<>("📄 Arquivo");
            fileCol.setCellValueFactory(data -> {
                String fileName = "Unknown";
                try {
                    fileName = new File(data.getValue().getPath()).getName();
                } catch (Exception e) {
                    logger.warning("Erro ao extrair nome do arquivo: " + e.getMessage());
                }
                return new SimpleStringProperty(fileName);
            });
            fileCol.prefWidthProperty().bind(table.widthProperty().multiply(0.40));
            fileCol.setMinWidth(150);
            
            // Coluna Encoding - 15% do espaço
            TableColumn<EncodingAnalysisResult, String> encodingCol = new TableColumn<>("🔤 Encoding");
            encodingCol.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getOriginalEncoding()));
            encodingCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
            encodingCol.setMinWidth(80);
            
            // Coluna Status - 20% do espaço
            TableColumn<EncodingAnalysisResult, String> statusCol = new TableColumn<>("📊 Status");
            statusCol.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getStatus()));
            statusCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));
            statusCol.setMinWidth(100);
            
            // Coluna Confiança - 15% do espaço
            TableColumn<EncodingAnalysisResult, String> confidenceCol = new TableColumn<>("🎯 Confiança");
            confidenceCol.setCellValueFactory(data -> 
                new SimpleStringProperty(String.format("%.1f%%", data.getValue().getConfidence() * 100)));
            confidenceCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
            confidenceCol.setMinWidth(70);
            
            // Coluna Tempo - 10% do espaço
            TableColumn<EncodingAnalysisResult, String> timeCol = new TableColumn<>("⏱️ Tempo");
            timeCol.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getProcessingTime() + "ms"));
            timeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.10));
            timeCol.setMinWidth(60);
            
            // Adiciona todas as colunas
            table.getColumns().addAll(fileCol, encodingCol, statusCol, confidenceCol, timeCol);
            table.setItems(analysisResults);
            
            // Configurações adicionais da tabela
            table.setRowFactory(tv -> {
                TableRow<EncodingAnalysisResult> row = new TableRow<>();
                row.itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        // Adiciona classes CSS baseadas no status
                        row.getStyleClass().removeAll("success-row", "warning-row", "error-row");
                        if ("success".equalsIgnoreCase(newItem.getStatus())) {
                            row.getStyleClass().add("success-row");
                        } else if ("warning".equalsIgnoreCase(newItem.getStatus())) {
                            row.getStyleClass().add("warning-row");
                        } else if ("error".equalsIgnoreCase(newItem.getStatus())) {
                            row.getStyleClass().add("error-row");
                        }
                    }
                });
                return row;
            });
            
            return table;
        }
        
        private void processFiles(List<File> files) {
            if (isProcessing.get()) {
                showAlert("Processamento em andamento", 
                    "Aguarde o processamento atual terminar.");
                return;
            }
            
            isProcessing.set(true);
            totalFiles.set(files.size());
            filesProcessed.set(0);
            analysisResults.clear();
            
            progressIndicator.setVisible(true);
            statusLabel.setText("Processando arquivos...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<String> paths = files.stream()
                        .map(File::getAbsolutePath)
                        .toList();
                    
                    String jsonPaths = gson.toJson(paths);
                    
                    MojibakeProcessor.ProcessingProgressCallback callback = 
                        (current, total, filename, status) -> {
                            Platform.runLater(() -> {
                                filesProcessed.set(current);
                                String fileName = "Unknown file";
                                if (filename != null) {
                                    try {
                                        fileName = new File(filename).getName();
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "Erro ao processar nome do arquivo", e);
                                    }
                                }
                                statusLabel.setText("Processando: " + fileName);
                                
                                double progress = total > 0 ? (double) current / total : 0.0;
                                progressIndicator.setProgress(progress);
                            });
                        };
                    
                    Map<String, Object> options = new HashMap<>();
                    options.put("fixMojibake", true);
                    options.put("useDictionary", true);
                    options.put("parallel", true);
                    
                    String jsonOptions = gson.toJson(options);
                    
                    // Processamento real usando biblioteca nativa
                    for (int i = 0; i < paths.size(); i++) {
                        String path = paths.get(i);
                        int current = i + 1;
                        
                        try {
                            // Chama função nativa real
                            String resultJson = characterEncodingEngine.AnalyzeDocumentEncoding(path, jsonOptions);
                            EncodingAnalysisResult result = gson.fromJson(resultJson, EncodingAnalysisResult.class);
                            
                            Platform.runLater(() -> {
                                analysisResults.add(result);
                                filesProcessed.set(current);
                            });
                            
                            if (callback != null) {
                                callback.invoke(current, paths.size(), path, result.getStatus());
                            }
                            
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Erro ao processar arquivo: " + path, ex);
                            Platform.runLater(() -> {
                                EncodingAnalysisResult errorResult = new EncodingAnalysisResult(path, "unknown", "error: " + ex.getMessage());
                                analysisResults.add(errorResult);
                                filesProcessed.set(current);
                            });
                        }
                    }
                    
                    return null;
                }
            };
            
            task.setOnSucceeded(e -> {
                isProcessing.set(false);
                progressIndicator.setVisible(false);
                statusLabel.setText("Processamento concluído!");
                showNotification("Sucesso", "Todos os arquivos foram processados.");
            });
            
            task.setOnFailed(e -> {
                isProcessing.set(false);
                progressIndicator.setVisible(false);
                statusLabel.setText("Erro no processamento");
                showError("Erro", task.getException().getMessage());
            });
            
            executorService.submit(task);
        }
    }
    
    private void stopProcessing() {
        if (isProcessing.get()) {
            // Cancela tarefas em execução
            executorService.shutdownNow();
            
            // Recria o executor para futuras operações
            // executorService = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
            
            isProcessing.set(false);
            processingWorkspace.progressIndicator.setVisible(false);
            processingWorkspace.statusLabel.setText("⏹ PROCESSAMENTO INTERROMPIDO");
            processingWorkspace.statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
            
            showAlert("Processamento Interrompido", "O processamento foi cancelado pelo usuário.");
        }
    }
    
    // Utility methods
    private Button createWindowButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add("window-button");
        btn.setOnAction(handler);
        return btn;
    }
    
    private void makeDraggable(javafx.scene.Node node) {
        final Delta dragDelta = new Delta();
        
        node.setOnMousePressed(mouseEvent -> {
            if (primaryStage != null) {
                dragDelta.x = primaryStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = primaryStage.getY() - mouseEvent.getScreenY();
            }
        });
        
        node.setOnMouseDragged(mouseEvent -> {
            if (primaryStage != null) {
                primaryStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                primaryStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }
    
    private static class Delta {
        double x, y;
    }
    
    // Stub methods
    private void ensureSingleInstance() {}
    private void loadConfiguration() {}
    private void preloadResources() {}
    private void showWithAnimation(Stage stage) {
        // Solução bizarra #1: Force hardware acceleration
        stage.getScene().getRoot().setCache(true);
        stage.getScene().getRoot().setCacheHint(javafx.scene.CacheHint.SPEED);
        
        // Solução bizarra #2: Set initial state BEFORE showing
        stage.getScene().getRoot().setOpacity(0);
        stage.getScene().getRoot().setScaleX(0.8);
        stage.getScene().getRoot().setScaleY(0.8);
        stage.setOpacity(1); // Stage opacity stays at 1
        stage.show();
        
        // Solução bizarra #3: Use Platform.runLater to avoid timing conflicts
        Platform.runLater(() -> {
            // Single interpolator for smooth animation
            Interpolator smoothInterpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), stage.getScene().getRoot());
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(smoothInterpolator);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(600), stage.getScene().getRoot());
            scaleIn.setFromX(0.8);
            scaleIn.setFromY(0.8);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);
            scaleIn.setInterpolator(smoothInterpolator);
            
            ParallelTransition entrance = new ParallelTransition(fadeIn, scaleIn);
            
            // Solução bizarra #4: Clear cache after animation
            entrance.setOnFinished(e -> {
                stage.getScene().getRoot().setCache(false);
            });
            
            entrance.play();
        });
    }
    private void setupSystemTray() {}
    private void startPerformanceMonitoring() {}
    private MenuBar createRibbonMenu() { return new MenuBar(); }
    private ToolBar createQuickAccessToolbar() { return new ToolBar(); }
    private void minimizeWithAnimation() {
        // Solução anti-pulso: Parar TODAS as animações primeiro
        primaryStage.getScene().getRoot().getChildrenUnmodifiable().forEach(node -> {
            if (node instanceof Label && node.getStyleClass().contains("app-logo")) {
                // Para a animação do logo temporariamente
                Timeline logoKiller = new Timeline();
                logoKiller.getKeyFrames().clear();
                logoKiller.stop();
            }
        });
        
        // Animação de minimizar mais sutil (não tão extrema)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), primaryStage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3); // Não some completamente
        
        ScaleTransition minimize = new ScaleTransition(Duration.millis(150), primaryStage.getScene().getRoot());
        minimize.setFromX(1.0);
        minimize.setFromY(1.0);
        minimize.setToX(0.8); // 80% ao invés de 10% (menos extremo)
        minimize.setToY(0.8);
        minimize.setInterpolator(Interpolator.EASE_IN);
        
        ParallelTransition minimizeEffect = new ParallelTransition(fadeOut, minimize);
        minimizeEffect.setOnFinished(e -> {
            primaryStage.setIconified(true);
            // Reset imediato quando minimizar (evita travamento do state)
            Platform.runLater(() -> {
                primaryStage.getScene().getRoot().setOpacity(1.0);
                primaryStage.getScene().getRoot().setScaleX(1.0);
                primaryStage.getScene().getRoot().setScaleY(1.0);
            });
        });
        
        minimizeEffect.play();
    }
    
    private void toggleMaximizedWithAnimation() {
        // Solução anti-pulso: Sem animação recursiva maluca!
        if (primaryStage.isMaximized()) {
            // Primeiro anima saída do maximizado
            ScaleTransition shrink = new ScaleTransition(Duration.millis(200), primaryStage.getScene().getRoot());
            shrink.setFromX(1.0);
            shrink.setFromY(1.0);
            shrink.setToX(0.95);
            shrink.setToY(0.95);
            shrink.setInterpolator(Interpolator.EASE_OUT);
            
            shrink.setOnFinished(e -> {
                primaryStage.setMaximized(false);
                // Reset suave sem nova animação
                Platform.runLater(() -> {
                    primaryStage.getScene().getRoot().setScaleX(1.0);
                    primaryStage.getScene().getRoot().setScaleY(1.0);
                });
            });
            shrink.play();
            
        } else {
            // Anima entrada para maximizado
            ScaleTransition grow = new ScaleTransition(Duration.millis(200), primaryStage.getScene().getRoot());
            grow.setFromX(1.0);
            grow.setFromY(1.0);
            grow.setToX(1.05);
            grow.setToY(1.05);
            grow.setInterpolator(Interpolator.EASE_OUT);
            
            grow.setOnFinished(e -> {
                primaryStage.setMaximized(true);
                // Reset suave sem nova animação
                Platform.runLater(() -> {
                    primaryStage.getScene().getRoot().setScaleX(1.0);
                    primaryStage.getScene().getRoot().setScaleY(1.0);
                });
            });
            grow.play();
        }
    }
    
    private void closeWithAnimation() {
        // Solução bizarra #5: Prevent user interaction during close
        primaryStage.getScene().getRoot().setDisable(true);
        
        // Solução bizarra #6: Combined fade + scale out for smoother exit
        Interpolator exitInterpolator = Interpolator.EASE_IN;
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), primaryStage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(exitInterpolator);
        
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), primaryStage.getScene().getRoot());
        scaleOut.setFromX(1.0);
        scaleOut.setFromY(1.0);
        scaleOut.setToX(0.9);
        scaleOut.setToY(0.9);
        scaleOut.setInterpolator(exitInterpolator);
        
        ParallelTransition exit = new ParallelTransition(fadeOut, scaleOut);
        
        // Solução bizarra #7: Force immediate exit after animation
        exit.setOnFinished(e -> {
            Platform.runLater(() -> {
                primaryStage.hide();
                Platform.exit();
                System.exit(0); // Nuclear option for clean exit
            });
        });
        
        exit.play();
    }
    
    private void openAdvancedBatchDialog() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Selecionar Diretório para Processamento em Lote");
        
        File directory = dirChooser.showDialog(primaryStage);
        if (directory != null) {
            List<File> files = Arrays.stream(directory.listFiles())
                .filter(f -> f.isFile() && 
                    (f.getName().endsWith(".txt") || f.getName().endsWith(".log") ||
                     f.getName().endsWith(".csv") || f.getName().endsWith(".json")))
                .collect(Collectors.toList());
            
            if (!files.isEmpty()) {
                processingWorkspace.processFiles(files);
            } else {
                showAlert("Nenhum Arquivo", "Nenhum arquivo compatível encontrado no diretório.");
            }
        }
    }
    private void handleClose() { Platform.exit(); }
    
    // Solução bizarra #12: Emergency animation reset method
    private void resetAllAnimations() {
        Platform.runLater(() -> {
            try {
                // Stop all running animations
                primaryStage.getScene().getRoot().getChildrenUnmodifiable().forEach(node -> {
                    node.setOpacity(1.0);
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                    node.setTranslateX(0);
                    node.setTranslateY(0);
                    node.setRotate(0);
                    node.setCache(false);
                });
                
                // Force scene refresh
                primaryStage.getScene().getRoot().setOpacity(0.999);
                Platform.runLater(() -> primaryStage.getScene().getRoot().setOpacity(1.0));
            } catch (Exception e) {
                // Ignore any errors during reset
            }
        });
    }
    private VBox createGeneralSettings() {
        VBox settings = new VBox(15);
        settings.setPadding(new Insets(10));
        
        CheckBox autoBackupCheck = new CheckBox("Criar backup automático dos arquivos");
        autoBackupCheck.setSelected(true);
        
        CheckBox aggressiveModeCheck = new CheckBox("Modo agressivo de correção");
        aggressiveModeCheck.setSelected(false);
        
        HBox confidenceBox = new HBox(10);
        confidenceBox.setAlignment(Pos.CENTER_LEFT);
        Label confidenceLabel = new Label("Limite de confiança:");
        Slider confidenceSlider = new Slider(0.5, 1.0, 0.8);
        confidenceSlider.setShowTickLabels(true);
        confidenceSlider.setShowTickMarks(true);
        Label confidenceValue = new Label("80%");
        confidenceSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            confidenceValue.setText(String.format("%.0f%%", newVal.doubleValue() * 100)));
        confidenceBox.getChildren().addAll(confidenceLabel, confidenceSlider, confidenceValue);
        
        settings.getChildren().addAll(autoBackupCheck, aggressiveModeCheck, confidenceBox);
        return settings;
    }
    
    private VBox createPerformanceSettings() {
        VBox settings = new VBox(15);
        settings.setPadding(new Insets(10));
        
        HBox threadsBox = new HBox(10);
        threadsBox.setAlignment(Pos.CENTER_LEFT);
        Label threadsLabel = new Label("Threads de processamento:");
        Spinner<Integer> threadsSpinner = new Spinner<>(1, Runtime.getRuntime().availableProcessors(), 
            Runtime.getRuntime().availableProcessors());
        threadsBox.getChildren().addAll(threadsLabel, threadsSpinner);
        
        CheckBox parallelProcessingCheck = new CheckBox("Processamento paralelo");
        parallelProcessingCheck.setSelected(true);
        
        CheckBox memoryOptimizationCheck = new CheckBox("Otimização de memória");
        memoryOptimizationCheck.setSelected(true);
        
        settings.getChildren().addAll(threadsBox, parallelProcessingCheck, memoryOptimizationCheck);
        return settings;
    }
    private void selectAndProcessFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Arquivos");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Arquivos de Texto", "*.txt"),
            new FileChooser.ExtensionFilter("Logs", "*.log"),
            new FileChooser.ExtensionFilter("CSV", "*.csv"),
            new FileChooser.ExtensionFilter("JSON", "*.json")
        );
        
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        if (files != null && !files.isEmpty()) {
            processingWorkspace.processFiles(files);
        }
    }
    private void updateDictionaryStats(TextArea area) {
        try {
            String analyticsData = characterEncodingEngine.RetrieveLanguageDictionaryMetrics();
            area.setText("Métricas do Analisador Linguístico:\n\n" + analyticsData);
        } catch (Exception e) {
            area.setText("Erro ao carregar estatísticas: " + e.getMessage());
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Status bar class
    private static class ApplicationStatusBar extends HBox {
        public ApplicationStatusBar() {
            getStyleClass().add("status-bar");
            setPadding(new Insets(5, 10, 5, 10));
            getChildren().add(new Label("Pronto"));
        }
    }
    
    @Override
    public void stop() {
        try {
            characterEncodingEngine.GracefulEngineShutdown();
            logger.info("🔌 Sistema finalizado com sucesso");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro no shutdown", e);
        }
        executorService.shutdown();
        scheduler.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}