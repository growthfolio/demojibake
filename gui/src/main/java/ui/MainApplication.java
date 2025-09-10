package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import core.DemojibakelizadorNative;
import core.ProcessingResult;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;

public class MainApplication extends Application {
    
    private final DemojibakelizadorNative nativeLib = DemojibakelizadorNative.INSTANCE;
    
    // UI Components
    private Stage primaryStage;
    private TabPane mainTabPane;
    private ProcessingPanel processingPanel;
    private StatusBar statusBar;
    
    // Application state
    private final ObservableList<ProcessingResult> results = FXCollections.observableArrayList();
    private final IntegerProperty filesProcessed = new SimpleIntegerProperty(0);
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final BooleanProperty isProcessing = new SimpleBooleanProperty(false);
    
    // Thread management
    private final ExecutorService executorService = ForkJoinPool.commonPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @Override
    public void init() throws Exception {
        int status = nativeLib.Initialize();
        if (status != 1) {
            throw new RuntimeException("Failed to initialize native library. Status: " + status);
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
        scene.getStylesheets().add("/styles/enterprise-dark.css");
        
        stage.setTitle("Demojibakelizador Enterprise v2.0");
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
        
        mainTabPane = new TabPane();
        mainTabPane.getStyleClass().add("main-tabs");
        mainTabPane.getTabs().addAll(
            createProcessingTab(),
            createDictionaryTab(),
            createAnalyticsTab(),
            createSettingsTab()
        );
        
        statusBar = new StatusBar();
        root.setBottom(statusBar);
        
        VBox centerContent = new VBox();
        centerContent.getChildren().addAll(topSection, mainTabPane);
        root.setCenter(centerContent);
        
        return root;
    }
    
    private HBox createCustomTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(5, 10, 5, 10));
        
        Label logo = new Label("◈");
        logo.getStyleClass().add("app-logo");
        
        Label title = new Label("Demojibakelizador Enterprise");
        title.getStyleClass().add("app-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button minimizeBtn = createWindowButton("−", e -> primaryStage.setIconified(true));
        Button maximizeBtn = createWindowButton("□", e -> toggleMaximized());
        Button closeBtn = createWindowButton("×", e -> handleClose());
        closeBtn.getStyleClass().add("close-button");
        
        titleBar.getChildren().addAll(logo, title, spacer, minimizeBtn, maximizeBtn, closeBtn);
        
        makeDraggable(titleBar);
        
        return titleBar;
    }
    
    private Tab createProcessingTab() {
        Tab tab = new Tab("Processamento");
        tab.setClosable(false);
        
        processingPanel = new ProcessingPanel();
        tab.setContent(processingPanel);
        
        return tab;
    }
    
    private Tab createDictionaryTab() {
        Tab tab = new Tab("Dicionário");
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
    
    private Tab createAnalyticsTab() {
        Tab tab = new Tab("Analytics");
        tab.setClosable(false);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label analyticsLabel = new Label("Métricas de Performance");
        analyticsLabel.getStyleClass().add("section-title");
        
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(10);
        
        metricsGrid.add(new Label("Arquivos Processados:"), 0, 0);
        metricsGrid.add(new Label("0"), 1, 0);
        
        metricsGrid.add(new Label("Taxa de Sucesso:"), 0, 1);
        metricsGrid.add(new Label("100%"), 1, 1);
        
        metricsGrid.add(new Label("Tempo Médio:"), 0, 2);
        metricsGrid.add(new Label("0ms"), 1, 2);
        
        content.getChildren().addAll(analyticsLabel, metricsGrid);
        tab.setContent(content);
        
        return tab;
    }
    
    private Tab createSettingsTab() {
        Tab tab = new Tab("Configurações");
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
    
    private class ProcessingPanel extends VBox {
        private final TableView<ProcessingResult> resultsTable;
        private final ProgressIndicator progressIndicator;
        private final Label statusLabel;
        private final Button processButton;
        private final VBox dropZone;
        
        public ProcessingPanel() {
            super(20);
            setPadding(new Insets(20));
            getStyleClass().add("processing-panel");
            
            dropZone = createDropZone();
            
            HBox controls = new HBox(10);
            controls.setAlignment(Pos.CENTER_LEFT);
            
            processButton = new Button("Processar Arquivos");
            processButton.getStyleClass().add("primary-button");
            processButton.setOnAction(e -> selectAndProcessFiles());
            
            Button batchButton = new Button("Processamento em Lote");
            batchButton.getStyleClass().add("secondary-button");
            
            Button stopButton = new Button("Parar");
            stopButton.getStyleClass().add("danger-button");
            stopButton.disableProperty().bind(isProcessing.not());
            
            controls.getChildren().addAll(processButton, batchButton, stopButton);
            
            HBox progressBox = new HBox(15);
            progressBox.setAlignment(Pos.CENTER);
            
            progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            
            statusLabel = new Label("Pronto para processar");
            statusLabel.getStyleClass().add("status-label");
            
            Label filesLabel = new Label();
            filesLabel.textProperty().bind(
                Bindings.format("%d / %d arquivos processados", 
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
            
            Label dropLabel = new Label("Arraste arquivos aqui");
            dropLabel.getStyleClass().add("drop-label");
            
            Label dropIcon = new Label("📁");
            dropIcon.setStyle("-fx-font-size: 48px;");
            
            dropZone.getChildren().addAll(dropIcon, dropLabel);
            
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
                    processFiles(db.getFiles());
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
            
            return dropZone;
        }
        
        private TableView<ProcessingResult> createResultsTable() {
            TableView<ProcessingResult> table = new TableView<>();
            table.getStyleClass().add("results-table");
            
            TableColumn<ProcessingResult, String> fileCol = new TableColumn<>("Arquivo");
            fileCol.setCellValueFactory(data -> 
                new SimpleStringProperty(new File(data.getValue().path).getName()));
            fileCol.setPrefWidth(250);
            
            TableColumn<ProcessingResult, String> encodingCol = new TableColumn<>("Encoding");
            encodingCol.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().originalEncoding));
            encodingCol.setPrefWidth(150);
            
            TableColumn<ProcessingResult, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().status));
            statusCol.setPrefWidth(100);
            
            table.getColumns().addAll(fileCol, encodingCol, statusCol);
            table.setItems(results);
            
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
            results.clear();
            
            progressIndicator.setVisible(true);
            statusLabel.setText("Processando arquivos...");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<String> paths = files.stream()
                        .map(File::getAbsolutePath)
                        .toList();
                    
                    String jsonPaths = new Gson().toJson(paths);
                    
                    DemojibakelizadorNative.ProgressCallback callback = 
                        (current, total, filename, status) -> {
                            Platform.runLater(() -> {
                                filesProcessed.set(current);
                                statusLabel.setText("Processando: " + new File(filename).getName());
                                
                                double progress = (double) current / total;
                                progressIndicator.setProgress(progress);
                            });
                        };
                    
                    Map<String, Object> options = new HashMap<>();
                    options.put("fixMojibake", true);
                    options.put("useDictionary", true);
                    options.put("parallel", true);
                    
                    String jsonOptions = new Gson().toJson(options);
                    
                    int result = nativeLib.ProcessBatchParallel(jsonPaths, callback, jsonOptions);
                    
                    if (result != 0) {
                        throw new RuntimeException("Processing failed with code: " + result);
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
            dragDelta.x = primaryStage.getX() - mouseEvent.getScreenX();
            dragDelta.y = primaryStage.getY() - mouseEvent.getScreenY();
        });
        
        node.setOnMouseDragged(mouseEvent -> {
            primaryStage.setX(mouseEvent.getScreenX() + dragDelta.x);
            primaryStage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
    }
    
    private static class Delta {
        double x, y;
    }
    
    // Stub methods
    private void ensureSingleInstance() {}
    private void loadConfiguration() {}
    private void preloadResources() {}
    private void showWithAnimation(Stage stage) { stage.show(); }
    private void setupSystemTray() {}
    private void startPerformanceMonitoring() {}
    private MenuBar createRibbonMenu() { return new MenuBar(); }
    private ToolBar createQuickAccessToolbar() { return new ToolBar(); }
    private void toggleMaximized() {}
    private void handleClose() { Platform.exit(); }
    private VBox createGeneralSettings() { return new VBox(); }
    private VBox createPerformanceSettings() { return new VBox(); }
    private void selectAndProcessFiles() {}
    private void updateDictionaryStats(TextArea area) {}
    private void showAlert(String title, String message) {}
    private void showNotification(String title, String message) {}
    private void showError(String title, String message) {}
    
    // Status bar class
    private static class StatusBar extends HBox {
        public StatusBar() {
            getStyleClass().add("status-bar");
            setPadding(new Insets(5, 10, 5, 10));
            getChildren().add(new Label("Pronto"));
        }
    }
    
    @Override
    public void stop() {
        nativeLib.Shutdown();
        executorService.shutdown();
        scheduler.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}