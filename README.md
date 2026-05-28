# NetScanner
       
        NetScanner Enterprise is a professional-grade network diagnostic and management suite built with JavaFX. It
         provides high-performance network discovery, deep service fingerprinting, and visual analytics to help you
         monitor and manage your local network infrastructure.
       
##  Key Features
       
        - **High-Performance Parallel Scanning:** Multi-threaded engine capable of scanning hundreds of hosts
         simultaneously.
        - **Service Fingerprinting:** Deep banner grabbing to identify software versions (SSH, HTTP, FTP, etc.) on
         open ports.
        - **Hardware Intelligence:** Automatic MAC address resolution and manufacturer (Vendor) identification.
       - **Visual Analytics:** Real-time dashboard with charts showing OS distribution and host status.
       - **Persistent History:** Save and restore scan sessions to track network changes over time.
       - **Enterprise Toolset:**
           - **Wake-on-LAN (WoL):** Remotely power on devices directly from the results table.
           - **Live Search:** Global filtering for IP, Hostname, Vendor, and OS.
           - **Flexible Port Discovery:** Support for specific port lists and numeric ranges.
      
##  Technical Stack
      
       - **Java 17+**
       - **JavaFX 20:** For a modern, hardware-accelerated user interface.
       - **Jackson Databind:** High-performance JSON serialization for history management.
       - **Maven:** For dependency management and build automation.
      
##  Installation & Setup
      
### Prerequisites
       - Java Development Kit (JDK) 17 or higher.
       - Apache Maven installed.
      
### Build & Run
       1. Clone the repository to your local machine.
       2. Navigate to the project root directory.
       3. Install dependencies and compile:
          ```bash
          mvn clean install
         ```
       4. Launch the application:
          ```bash
          mvn javafx:run
          ```
      
##  Usage
      
       1. **Configure Scan:** Enter your target IP range and preferred port settings in the sidebar.
       2. **Execute:** Click **"RUN ENTERPRISE SCAN"** to begin discovery.
       3. **Analyze:** Use the **Analytics** tab to view network distribution or the **Live Search** to find
         specific devices.
       4. **Manage:** Right-click any host to copy its IP or send a Wake-on-LAN packet.
       5. **Persist:** Click **"SAVE TO HISTORY"** to archive your results for future comparison.
      
       
##  Privacy & Security
      
       - **Local Storage:** All scan history is stored locally in `scan_history.json`.
       - **Git Protection:** The `.gitignore` is pre-configured to ensure your private network data is never
         uploaded to source control.
      
       ---
       *Developed for robust network diagnostics and management.*
