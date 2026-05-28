NetScanner 

NetScanner is a professional-grade network diagnostic and management suite built with JavaFX.
It provides high-performance network discovery, deep service fingerprinting, and visual analytics to help monitor and manage local network infrastructure efficiently.

---

Key Features

High-Performance Parallel Scanning

A multi-threaded scanning engine capable of discovering and analyzing hundreds of hosts simultaneously.

Service Fingerprinting

Advanced banner grabbing and service detection for identifying software versions running on open ports, including:

- SSH
- HTTP/HTTPS
- FTP
- SMTP
- and more

Hardware Intelligence

Automatic:

- MAC address resolution
- Vendor/manufacturer identification

Visual Analytics

Real-time analytics dashboard featuring:

- Operating system distribution charts
- Host status monitoring
- Live network statistics

Persistent Scan History

Save and restore scan sessions to:

- Track infrastructure changes
- Compare historical results
- Maintain audit visibility

Enterprise Toolset

Wake-on-LAN (WoL)

Remotely power on compatible devices directly from the results table.

Live Search & Filtering

Instant filtering by:

- IP Address
- Hostname
- Vendor
- Operating System

Flexible Port Discovery

Supports:

- Custom port lists
- Numeric port ranges
- Multi-port scanning configurations

---

Technical Stack

- Java 17+
- JavaFX 20 — modern hardware-accelerated UI
- Jackson Databind — high-performance JSON serialization
- Maven — dependency management and build automation

---

Installation & Setup

Prerequisites

Before running the application, ensure the following are installed:

- Java Development Kit (JDK) 17 or newer
- Apache Maven

---

Build & Run

1. Clone the repository

git clone <repository-url>
cd NetScanner

2. Build the project

mvn clean install

3. Launch the application

mvn javafx:run

---

Usage

1. Configure Scan

Enter:

- Target IP range
- Preferred port settings
- Scan configuration options

2. Start Scanning

Click:

RUN ENTERPRISE SCAN

to begin host discovery and analysis.

3. Analyze Results

Use:

- Analytics tab for network insights
- Live Search for instant filtering

4. Manage Hosts

Right-click any discovered host to:

- Copy IP address
- Send Wake-on-LAN packet

5. Save Results

Click:

SAVE TO HISTORY

to archive scan sessions for future comparison.

---

Privacy & Security

Local Storage

All scan history is stored locally in:

scan_history.json

Git Protection

The included ".gitignore" configuration helps prevent accidental uploads of sensitive network data to version control.

---

Notes

- Designed for local network diagnostics and administration
- Optimized for performance and scalability
- Built with a modern JavaFX architecture

---

Developed for robust network diagnostics and enterprise network management.