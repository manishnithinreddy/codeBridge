#!/usr/bin/env python3

import os
import sys
import threading
import time
import logging
from ssh_server import start_server as start_ssh_server
from ftp_server import start_ftp_server

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def main():
    # Create test files directory
    os.makedirs("files", exist_ok=True)
    
    # Create test files
    with open("files/test1.txt", "w") as f:
        f.write("This is test file 1\n")
    with open("files/test2.txt", "w") as f:
        f.write("This is test file 2\n")
    
    # Start SSH server in a separate thread
    ssh_thread = threading.Thread(target=start_ssh_server, args=(2222,))
    ssh_thread.daemon = True
    ssh_thread.start()
    logger.info("SSH server started on port 2222")
    
    # Start FTP server in a separate thread
    ftp_thread = threading.Thread(target=start_ftp_server, args=("./files", 2121))
    ftp_thread.daemon = True
    ftp_thread.start()
    logger.info("FTP server started on port 2121")
    
    logger.info("Both servers are running. Press Ctrl+C to stop.")
    
    try:
        # Keep the main thread alive
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down servers...")
        sys.exit(0)

if __name__ == "__main__":
    main()

