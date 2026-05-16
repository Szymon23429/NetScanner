package service;

import model.Host;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NetworkScanner {

    private int timeout = 500;
    private int threadCount = 50;
    private volatile boolean stopScan = false;
    
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private static final Map<String, String> VENDOR_MAP = new HashMap<>();
    static {
        VENDOR_MAP.put("00:05:02", "Apple");
        VENDOR_MAP.put("00:50:56", "VMware");
        VENDOR_MAP.put("00:0C:29", "VMware");
        VENDOR_MAP.put("00:15:5D", "Microsoft (Hyper-V)");
        VENDOR_MAP.put("00:1A:11", "Google");
        VENDOR_MAP.put("00:00:0C", "Cisco");
        VENDOR_MAP.put("B8:27:EB", "Raspberry Pi");
        VENDOR_MAP.put("DC:A6:32", "Raspberry Pi");
        VENDOR_MAP.put("00:11:32", "Synology");
        VENDOR_MAP.put("F4:F2:6D", "TP-Link");
    }

    public NetworkScanner() {}
    public NetworkScanner(int timeout) { this.timeout = timeout; }

    public void stop() { this.stopScan = true; }

    public static boolean isValidIp(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    public void scanRange(String startIp, String endIp, Consumer<Host> onHostDiscovered) {
        if (!isValidIp(startIp) || !isValidIp(endIp)) {
            return;
        }

        this.stopScan = false;
        long start = ipToLong(startIp);
        long end = ipToLong(endIp);

        if (start > end) return;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (long i = start; i <= end; i++) {
            if (stopScan) break;
            final String ip = longToIp(i);
            executor.submit(() -> {
                if (stopScan) return;
                try {
                    Host host = scanHost(ip);
                    onHostDiscovered.accept(host);
                } catch (Exception e) {
                    System.err.println("Error scanning " + ip + ": " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Host scanHost(String ip) {
        Host host = new Host(ip);
        try {
            long startTime = System.currentTimeMillis();
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(timeout)) {
                long endTime = System.currentTimeMillis();
                host.setStatus("Alive");
                host.setLatency(endTime - startTime);
                host.setHostname(address.getCanonicalHostName());
                String mac = resolveMac(ip);
                host.setMac(mac);
                host.setVendor(lookupVendor(mac));
                host.setOs(detectOsHint(host));
            } else {
                host.setStatus("Dead");
            }
        } catch (IOException e) {
            host.setStatus("Error");
        }
        return host;
    }

    private String resolveMac(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "arp -a " + ip : "arp -n " + ip;
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(ip)) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (part.matches("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")) {
                                return part.toUpperCase().replace("-", ":");
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String lookupVendor(String mac) {
        if (mac == null || mac.equals("Unknown") || mac.length() < 8) return "Unknown";
        String prefix = mac.substring(0, 8).toUpperCase();
        return VENDOR_MAP.getOrDefault(prefix, "Generic/Unknown");
    }

    private String detectOsHint(Host host) {
        String hn = host.getHostname().toLowerCase();
        if (hn.contains("windows")) return "Windows";
        if (hn.contains("android")) return "Android";
        if (hn.contains("iphone") || hn.contains("ipad") || hn.contains("apple")) return "iOS/macOS";
        if (host.getVendor().contains("Raspberry")) return "Linux (Raspberry Pi)";
        if (host.getVendor().contains("VMware")) return "VMware Guest";
        return "Generic Linux/Other";
    }

    private long ipToLong(String ipAddress) {
        try {
            String[] parts = ipAddress.split("\\.");
            long result = 0;
            for (int i = 0; i < 4; i++) {
                result += Integer.parseInt(parts[i]) * Math.pow(256, 3 - i);
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    private String longToIp(long i) {
        return ((i >> 24) & 0xFF) + "." +
               ((i >> 16) & 0xFF) + "." +
               ((i >> 8) & 0xFF) + "." +
               (i & 0xFF);
    }

    public void setTimeout(int timeout) { this.timeout = timeout; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
}