package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Host;
import service.HistoryManager;
import service.NetworkScanner;
import service.PortScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MainView extends Application {

    private NetworkScanner networkScanner = new NetworkScanner();
    private PortScanner portScanner = new PortScanner();
    private HistoryManager historyManager = new HistoryManager();
    
    private ObservableList<Host> hostList = FXCollections.observableArrayList();
    private FilteredList<Host> filteredHostList = new FilteredList<>(hostList, p -> true);
    
    private TabPane tabPane;
    private PieChart osChart;
    private PieChart statusChart;
    private ListView<HistoryManager.ScanSession> historyListView;

    @Override
    public void start(Stage stage) {
        tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tabs");

        Tab scanTab = new Tab("Live Scanner", createScanView(stage));
        Tab dashboardTab = new Tab("Analytics", createDashboardView());
        Tab historyTab = new Tab("Scan History", createHistoryView());

        scanTab.setClosable(false);
        dashboardTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(scanTab, dashboardTab, historyTab);

        historyTab.setOnSelectionChanged(e -> {
            if (historyTab.isSelected()) refreshHistory();
        });

        Scene scene = new Scene(tabPane, 1400, 800);
        try {
            String cssPath = getClass().getResource("/ui/App.css").toExternalForm();
            if (cssPath != null) scene.getStylesheets().add(cssPath);
        } catch (Exception ignored) {}

        stage.setTitle("NetScanner");
        stage.setScene(scene);
        stage.show();
    }

    private Node createScanView(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(15));
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("sidebar");

        Label settingsLabel = new Label("SCAN CONFIGURATION");
        settingsLabel.getStyleClass().add("header-label");

        TextField startIpField = new TextField("192.168.1.1");
        TextField endIpField = new TextField("192.168.1.254");
        TextField portsField = new TextField("21,22,80,443,3306,8080");
        
        Spinner<Integer> timeoutSpinner = new Spinner<>(100, 5000, 500, 100);
        Spinner<Integer> threadsSpinner = new Spinner<>(1, 200, 50, 10);

        Button scanBtn = new Button("RUN ENTERPRISE SCAN");
        scanBtn.setMaxWidth(Double.MAX_VALUE);
        scanBtn.getStyleClass().add("btn-primary");
        
        Button stopBtn = new Button("ABORT SCAN");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setDisable(true);
        stopBtn.getStyleClass().add("btn-danger");

        Button saveBtn = new Button("SAVE TO HISTORY");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setDisable(true);

        sidebar.getChildren().addAll(
            settingsLabel,
            new Label("Start IP:"), startIpField,
            new Label("End IP:"), endIpField,
            new Label("Custom Ports:"), portsField,
            new Label("Timeout (ms):"), timeoutSpinner,
            new Label("Parallel Threads:"), threadsSpinner,
            new Separator(),
            scanBtn, stopBtn, saveBtn
        );

        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(0, 0, 0, 15));
        
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Live Search (IP, Hostname, Vendor, OS)...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        ComboBox<String> filterBox = new ComboBox<>();
        filterBox.getItems().addAll("All Hosts", "Alive Only", "Dead Only");
        filterBox.setValue("All Hosts");
        
        header.getChildren().addAll(searchField, new Label("Display:"), filterBox);

        HBox statusArea = new HBox(15);
        statusArea.setAlignment(Pos.CENTER_LEFT);
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        Label statusLabel = new Label("Ready to scan");
        statusArea.getChildren().addAll(progressBar, statusLabel);

        TableView<Host> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        
        TableColumn<Host, String> ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        
        TableColumn<Host, String> hostCol = new TableColumn<>("Hostname");
        hostCol.setCellValueFactory(new PropertyValueFactory<>("hostname"));

        TableColumn<Host, String> vendorCol = new TableColumn<>("Manufacturer");
        vendorCol.setCellValueFactory(new PropertyValueFactory<>("vendor"));

        TableColumn<Host, String> osCol = new TableColumn<>("OS Hint");
        osCol.setCellValueFactory(new PropertyValueFactory<>("os"));

        TableColumn<Host, Long> latencyCol = new TableColumn<>("Latency");
        latencyCol.setCellValueFactory(new PropertyValueFactory<>("latency"));
        latencyCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item < 0) setText("-");
                else setText(item + " ms");
            }
        });

        TableColumn<Host, String> portsCol = new TableColumn<>("Services & Banners");
        portsCol.setCellValueFactory(new PropertyValueFactory<>("portsString"));

        table.getColumns().addAll(ipCol, hostCol, vendorCol, osCol, latencyCol, portsCol);
        table.setItems(filteredHostList);

        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("Copy IP");
        copy.setOnAction(e -> {
            Host h = table.getSelectionModel().getSelectedItem();
            if (h != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(h.getIp());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        MenuItem wol = new MenuItem("Wake-on-LAN (WoL)");
        wol.setOnAction(e -> {
            Host h = table.getSelectionModel().getSelectedItem();
            if (h != null && !h.getMac().equals("Unknown")) {
                try {
                    service.WakeOnLan.sendMagicPacket(h.getMac());
                    statusLabel.setText("WoL Magic Packet sent to " + h.getMac());
                } catch (Exception ex) {
                    showAlert("WoL Error", "Failed to send packet: " + ex.getMessage());
                }
            } else {
                showAlert("WoL Error", "Valid MAC address required to send Magic Packet.");
            }
        });
        cm.getItems().addAll(copy, wol);
        table.setContextMenu(cm);

        mainContent.getChildren().addAll(header, statusArea, table);
        root.setLeft(sidebar);
        root.setCenter(mainContent);

        searchField.textProperty().addListener((obs, old, val) -> {
            updateFilters(val, filterBox.getValue());
        });
        filterBox.setOnAction(e -> updateFilters(searchField.getText(), filterBox.getValue()));

        scanBtn.setOnAction(e -> {
            hostList.clear();
            progressBar.setProgress(0);
            scanBtn.setDisable(true);
            stopBtn.setDisable(false);
            saveBtn.setDisable(true);
            
            networkScanner.setTimeout(timeoutSpinner.getValue());
            networkScanner.setThreadCount(threadsSpinner.getValue());
            portScanner.setPortsFromString(portsField.getText());

            new Thread(() -> {
                AtomicInteger completed = new AtomicInteger(0);
                String sIp = startIpField.getText();
                String eIp = endIpField.getText();
                long startLong = ipToLong(sIp);
                long endLong = ipToLong(eIp);
                int total = (int) (endLong - startLong + 1);

                networkScanner.scanRange(sIp, eIp, host -> {
                    if (host.getStatus().equals("Alive")) {
                        portScanner.scanPorts(host);
                    }
                    
                    int current = completed.incrementAndGet();
                    Platform.runLater(() -> {
                        hostList.add(host);
                        progressBar.setProgress((double) current / total);
                        statusLabel.setText(String.format("Scanning %d/%d...", current, total));
                        updateCharts();
                    });
                });

                Platform.runLater(() -> {
                    scanBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    saveBtn.setDisable(hostList.isEmpty());
                    statusLabel.setText("Scan Complete. Results ready to save.");
                });
            }).start();
        });

        stopBtn.setOnAction(e -> networkScanner.stop());
        saveBtn.setOnAction(e -> {
            boolean success = historyManager.saveScan(hostList);
            if (success) {
                refreshHistory();
                saveBtn.setDisable(true);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Scan successfully saved to history.", ButtonType.OK);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.show();
            } else {
                showAlert("Save Error", "Failed to save scan session to disk.");
            }
        });

        return root;
    }

    private Node createDashboardView() {
        HBox charts = new HBox(20);
        charts.setPadding(new Insets(20));
        charts.setAlignment(Pos.CENTER);

        statusChart = new PieChart();
        statusChart.setTitle("Host Status Distribution");
        
        osChart = new PieChart();
        osChart.setTitle("OS Distribution");

        charts.getChildren().addAll(statusChart, osChart);
        return charts;
    }

    private Node createHistoryView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("PAST SCAN SESSIONS");
        title.getStyleClass().add("header-label");

        historyListView = new ListView<>();
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        
        Button loadBtn = new Button("RESTORE SESSION");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-primary");

        Button clearBtn = new Button("CLEAR ALL HISTORY");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.getStyleClass().add("btn-danger");

        historyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) loadSelectedHistory();
        });

        loadBtn.setOnAction(e -> loadSelectedHistory());
        clearBtn.setOnAction(e -> {
            File file = new File("scan_history.json");
            if (file.exists()) file.delete();
            refreshHistory();
        });

        root.getChildren().addAll(title, historyListView, loadBtn, clearBtn);
        refreshHistory();
        return root;
    }

    private void updateFilters(String search, String statusFilter) {
        filteredHostList.setPredicate(host -> {
            boolean matchesSearch = search == null || search.isEmpty() ||
                host.getIp().contains(search) ||
                host.getHostname().toLowerCase().contains(search.toLowerCase()) ||
                host.getVendor().toLowerCase().contains(search.toLowerCase()) ||
                host.getOs().toLowerCase().contains(search.toLowerCase());
            
            boolean matchesStatus = true;
            if (statusFilter.equals("Alive Only")) matchesStatus = host.getStatus().equals("Alive");
            else if (statusFilter.equals("Dead Only")) matchesStatus = host.getStatus().equals("Dead");
            
            return matchesSearch && matchesStatus;
        });
    }

    private void updateCharts() {
        Map<String, Long> statusData = hostList.stream()
            .collect(Collectors.groupingBy(Host::getStatus, Collectors.counting()));
        
        statusChart.getData().clear();
        statusData.forEach((k, v) -> statusChart.getData().add(new PieChart.Data(k, v)));

        Map<String, Long> osData = hostList.stream()
            .filter(h -> h.getStatus().equals("Alive"))
            .collect(Collectors.groupingBy(Host::getOs, Collectors.counting()));
        
        osChart.getData().clear();
        osData.forEach((k, v) -> osChart.getData().add(new PieChart.Data(k, v)));
    }

    private void refreshHistory() {
        List<HistoryManager.ScanSession> history = historyManager.loadHistory();
        historyListView.setItems(FXCollections.observableArrayList(history));
    }

    private void loadSelectedHistory() {
        HistoryManager.ScanSession session = historyListView.getSelectionModel().getSelectedItem();
        if (session != null) {
            hostList.setAll(session.getHosts());
            tabPane.getSelectionModel().select(0);
            updateCharts();
        }
    }

    private long ipToLong(String ip) {
        try {
            String[] parts = ip.split("\\.");
            long res = 0;
            for (int i = 0; i < 4; i++) res += (long)Integer.parseInt(parts[i]) * (long)Math.pow(256, 3 - i);
            return res;
        } catch (Exception e) { return 0; }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(); }
}