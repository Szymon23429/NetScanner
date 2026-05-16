package service;

import model.Host;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PortScanner {

    private List<Integer> portsToCheck = List.of(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 5432, 8080);
    private int timeout = 200;

    public void setPortsToCheck(List<Integer> ports) { this.portsToCheck = ports; }
    
    public void setPortsFromString(String portInput) {
        if (portInput == null || portInput.trim().isEmpty()) return;
        
        List<Integer> parsedPorts = new ArrayList<>();
        String[] parts = portInput.split(",");
        for (String part : parts) {
            try {
                part = part.trim();
                if (part.contains("-")) {
                    String[] range = part.split("-");
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        if (i >= 0 && i <= 65535) parsedPorts.add(i);
                    }
                } else {
                    int p = Integer.parseInt(part);
                    if (p >= 0 && p <= 65535) parsedPorts.add(p);
                }
            } catch (Exception ignored) {}
        }
        if (!parsedPorts.isEmpty()) {
            this.portsToCheck = parsedPorts.stream().distinct().collect(Collectors.toList());
        }
    }

    public void setTimeout(int timeout) { this.timeout = timeout; }

    public void scanPorts(Host host){
        if(!host.getStatus().equals("Alive")) return;

        int threads = Math.min(portsToCheck.size(), 50);
        if (threads <= 0) threads = 1;
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for(int port : portsToCheck){
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host.getIp(), port), timeout);
                    host.addOpenPort(port);
                    String banner = grabBanner(host.getIp(), port);
                    if (banner != null) {
                        host.addServiceInfo(port, banner);
                    }
                } catch (Exception ignored) {}
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String grabBanner(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.setSoTimeout(1000);
            
            if (port == 80 || port == 8080 || port == 443) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("HEAD / HTTP/1.1\r\nHost: " + ip + "\r\n\r\n");
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.toLowerCase().startsWith("server:")) {
                        return line.substring(7).trim();
                    }
                }
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                return in.readLine();
            }
        } catch (Exception ignored) {}
        return null;
    }
}