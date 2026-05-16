package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Host {
    private String ip = "";
    private String hostname = "Unknown";
    private String status = "Dead";
    private String vendor = "Unknown";
    private String os = "Unknown";
    private String mac = "Unknown";
    private long latency = -1;
    private List<Integer> openPorts;
    private Map<String, String> serviceInfo;

    public Host() {
        this.openPorts = Collections.synchronizedList(new ArrayList<>());
        this.serviceInfo = Collections.synchronizedMap(new HashMap<>());
    }

    public Host(String ip) {
        this();
        this.ip = (ip != null) ? ip : "";
    }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = (ip != null) ? ip : ""; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = (hostname != null) ? hostname : "Unknown"; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = (status != null) ? status : "Dead"; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = (vendor != null) ? vendor : "Unknown"; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = (os != null) ? os : "Unknown"; }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = (mac != null) ? mac : "Unknown"; }

    public long getLatency() { return latency; }
    public void setLatency(long latency) { this.latency = latency; }

    public List<Integer> getOpenPorts() { return new ArrayList<>(openPorts); }
    public void setOpenPorts(List<Integer> openPorts) { 
        this.openPorts = Collections.synchronizedList(new ArrayList<>(openPorts != null ? openPorts : new ArrayList<>())); 
    }

    public Map<String, String> getServiceInfo() { return new HashMap<>(serviceInfo); }
    public void setServiceInfo(Map<String, String> serviceInfo) {
        this.serviceInfo = Collections.synchronizedMap(new HashMap<>(serviceInfo != null ? serviceInfo : new HashMap<>()));
    }

    public void addOpenPort(int port) {
        if(!openPorts.contains(port)) openPorts.add(port);
    }

    public void addServiceInfo(int port, String info) {
        serviceInfo.put(String.valueOf(port), info);
    }

    public String getServiceInfo(int port) {
        return serviceInfo.getOrDefault(String.valueOf(port), "Unknown");
    }

    public void clearPorts() { 
        openPorts.clear(); 
        serviceInfo.clear();
    }

    @JsonIgnore
    public String getPortsString() {
        synchronized (openPorts) {
            if (openPorts.isEmpty()) return "-";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < openPorts.size(); i++) {
                int port = openPorts.get(i);
                sb.append(port);
                String info = serviceInfo.get(String.valueOf(port));
                if (info != null && !info.equals("Unknown")) {
                    sb.append(" (").append(info).append(")");
                }
                if (i < openPorts.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
    }

    @JsonIgnore
    public int getOpenPortCount() {
        return openPorts.size();
    }
}