package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.Host;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private static final String HISTORY_FILE = "scan_history.json";
    private ObjectMapper mapper;

    public HistoryManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.findAndRegisterModules();
    }

    public static class ScanSession {
        private String timestamp;
        private List<Host> hosts;

        public ScanSession() {}
        public ScanSession(String timestamp, List<Host> hosts) {
            this.timestamp = timestamp;
            this.hosts = hosts;
        }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public List<Host> getHosts() { return hosts; }
        public void setHosts(List<Host> hosts) { this.hosts = hosts; }

        @Override
        public String toString() {
            return timestamp + " (" + (hosts != null ? hosts.size() : 0) + " hosts)";
        }
    }

    public boolean saveScan(List<Host> hosts) {
        try {
            List<ScanSession> history = loadHistory();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            history.add(0, new ScanSession(timestamp, new ArrayList<>(hosts)));
            mapper.writeValue(new File(HISTORY_FILE), history);
            return true;
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
            return false;
        }
    }

    public List<ScanSession> loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        
        try {
            return mapper.readValue(file, new TypeReference<List<ScanSession>>() {});
        } catch (IOException e) {
            System.err.println("Load failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}