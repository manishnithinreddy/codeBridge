#!/usr/bin/env python3

import os
import logging
from pyftpdlib.authorizers import DummyAuthorizer
from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import FTPServer

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def start_ftp_server(directory="./files", port=2121):
    # Create directory if it doesn't exist
    os.makedirs(directory, exist_ok=True)
    
    # Create test files
    with open(os.path.join(directory, "test1.txt"), "w") as f:
        f.write("This is test file 1\n")
    with open(os.path.join(directory, "test2.txt"), "w") as f:
        f.write("This is test file 2\n")
    
    # Create an authorizer
    authorizer = DummyAuthorizer()
    
    # Add a user with full permissions
    authorizer.add_user("testuser", "testpassword", directory, perm="elradfmwMT")
    
    # Add an anonymous user with read-only permissions
    authorizer.add_anonymous(directory, perm="elr")
    
    # Create a handler
    handler = FTPHandler
    handler.authorizer = authorizer
    
    # Set up the server
    server = FTPServer(("0.0.0.0", port), handler)
    
    # Set the maximum number of connections
    server.max_cons = 256
    server.max_cons_per_ip = 5
    
    # Start the server
    logger.info(f"Starting FTP server on port {port}, serving directory {directory}")
    server.serve_forever()

if __name__ == "__main__":
    start_ftp_server()

