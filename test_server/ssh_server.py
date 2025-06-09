#!/usr/bin/env python3

import socket
import sys
import threading
import os
import paramiko
import logging
from binascii import hexlify

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Generate a test key
host_key = paramiko.RSAKey.generate(2048)
logger.info(f"Host key fingerprint: {hexlify(host_key.get_fingerprint())}")

class SSHServer(paramiko.ServerInterface):
    def __init__(self):
        self.event = threading.Event()
        self.username = "testuser"
        self.password = "testpassword"

    def check_channel_request(self, kind, chanid):
        if kind == "session":
            return paramiko.OPEN_SUCCEEDED
        return paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED

    def check_auth_password(self, username, password):
        if username == self.username and password == self.password:
            return paramiko.AUTH_SUCCESSFUL
        return paramiko.AUTH_FAILED

    def check_auth_publickey(self, username, key):
        # For testing, we'll accept any key
        if username == self.username:
            return paramiko.AUTH_SUCCESSFUL
        return paramiko.AUTH_FAILED

    def get_allowed_auths(self, username):
        return "password,publickey"

    def check_channel_shell_request(self, channel):
        self.event.set()
        return True

    def check_channel_pty_request(
        self, channel, term, width, height, pixelwidth, pixelheight, modes
    ):
        return True

    def check_channel_exec_request(self, channel, command):
        # Execute the command
        logger.info(f"Executing command: {command}")
        
        # Simple command handling
        if command.startswith(b"ls"):
            path = "."
            if len(command) > 3:
                path = command[3:].decode().strip()
            
            try:
                files = os.listdir(path)
                output = "\n".join(files)
                channel.send(output + "\n")
            except Exception as e:
                channel.send(f"Error: {str(e)}\n")
        
        elif command.startswith(b"mkdir"):
            if len(command) > 6:
                dir_name = command[6:].decode().strip()
                try:
                    os.makedirs(dir_name, exist_ok=True)
                    channel.send(f"Directory {dir_name} created\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: mkdir <directory>\n")
        
        elif command.startswith(b"rm"):
            if len(command) > 3:
                file_name = command[3:].decode().strip()
                try:
                    if os.path.isdir(file_name):
                        os.rmdir(file_name)
                    else:
                        os.remove(file_name)
                    channel.send(f"Removed {file_name}\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: rm <file/directory>\n")
        
        elif command.startswith(b"touch"):
            if len(command) > 6:
                file_name = command[6:].decode().strip()
                try:
                    with open(file_name, 'a'):
                        os.utime(file_name, None)
                    channel.send(f"Created/updated {file_name}\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: touch <file>\n")
        
        elif command.startswith(b"echo"):
            if len(command) > 5:
                content = command[5:].decode().strip()
                if ">" in content:
                    parts = content.split(">", 1)
                    text = parts[0].strip()
                    file_name = parts[1].strip()
                    try:
                        with open(file_name, 'w') as f:
                            f.write(text)
                        channel.send(f"Wrote to {file_name}\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\n")
                else:
                    channel.send(content + "\n")
            else:
                channel.send("\n")
        
        elif command.startswith(b"cat"):
            if len(command) > 4:
                file_name = command[4:].decode().strip()
                try:
                    with open(file_name, 'r') as f:
                        content = f.read()
                    channel.send(content + "\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: cat <file>\n")
        
        else:
            channel.send(f"Command not supported: {command.decode()}\n")
        
        channel.send_exit_status(0)
        channel.close()
        return True

def start_server(port=2222):
    # Create a socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    # Bind to the port
    try:
        sock.bind(("0.0.0.0", port))
    except socket.error as e:
        logger.error(f"Bind failed: {str(e)}")
        sys.exit(1)
    
    # Start listening
    sock.listen(100)
    logger.info(f"Listening for connections on port {port}...")
    
    # Accept connections
    while True:
        try:
            client, addr = sock.accept()
            logger.info(f"Connection from {addr[0]}:{addr[1]}")
            
            # Create a new thread for the connection
            t = threading.Thread(target=handle_connection, args=(client, addr))
            t.daemon = True
            t.start()
        except KeyboardInterrupt:
            logger.info("Server shutting down...")
            break
        except Exception as e:
            logger.error(f"Error: {str(e)}")
    
    sock.close()

def handle_connection(client, addr):
    try:
        # Set up the transport
        transport = paramiko.Transport(client)
        transport.add_server_key(host_key)
        
        # Start the server
        server = SSHServer()
        transport.start_server(server=server)
        
        # Wait for authentication
        channel = transport.accept(20)
        if channel is None:
            logger.warning("No channel established")
            return
        
        server.event.wait(10)
        if not server.event.is_set():
            logger.warning("No shell requested")
            return
        
        # Set up a simple shell
        channel.send("Welcome to the test SSH server!\r\n")
        channel.send("Type 'exit' to disconnect\r\n")
        
        # Simple shell loop
        f = channel.makefile("rU")
        while True:
            channel.send("$ ")
            command = f.readline().strip("\r\n")
            if command == "exit":
                break
            
            # Execute the command
            if command.startswith("ls"):
                path = "."
                if len(command) > 3:
                    path = command[3:].strip()
                
                try:
                    files = os.listdir(path)
                    output = "\n".join(files)
                    channel.send(output + "\r\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\r\n")
            
            elif command.startswith("mkdir"):
                if len(command) > 6:
                    dir_name = command[6:].strip()
                    try:
                        os.makedirs(dir_name, exist_ok=True)
                        channel.send(f"Directory {dir_name} created\r\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: mkdir <directory>\r\n")
            
            elif command.startswith("rm"):
                if len(command) > 3:
                    file_name = command[3:].strip()
                    try:
                        if os.path.isdir(file_name):
                            os.rmdir(file_name)
                        else:
                            os.remove(file_name)
                        channel.send(f"Removed {file_name}\r\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: rm <file/directory>\r\n")
            
            elif command.startswith("touch"):
                if len(command) > 6:
                    file_name = command[6:].strip()
                    try:
                        with open(file_name, 'a'):
                            os.utime(file_name, None)
                        channel.send(f"Created/updated {file_name}\r\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: touch <file>\r\n")
            
            elif command.startswith("echo"):
                if len(command) > 5:
                    content = command[5:].strip()
                    if ">" in content:
                        parts = content.split(">", 1)
                        text = parts[0].strip()
                        file_name = parts[1].strip()
                        try:
                            with open(file_name, 'w') as f:
                                f.write(text)
                            channel.send(f"Wrote to {file_name}\r\n")
                        except Exception as e:
                            channel.send(f"Error: {str(e)}\r\n")
                    else:
                        channel.send(content + "\r\n")
                else:
                    channel.send("\r\n")
            
            elif command.startswith("cat"):
                if len(command) > 4:
                    file_name = command[4:].strip()
                    try:
                        with open(file_name, 'r') as f:
                            content = f.read()
                        channel.send(content + "\r\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: cat <file>\r\n")
            
            else:
                channel.send(f"Command not supported: {command}\r\n")
        
        channel.close()
    
    except Exception as e:
        logger.error(f"Error: {str(e)}")
    finally:
        try:
            transport.close()
        except:
            pass

if __name__ == "__main__":
    # Create test files
    os.makedirs("files", exist_ok=True)
    with open("files/test1.txt", "w") as f:
        f.write("This is test file 1\n")
    with open("files/test2.txt", "w") as f:
        f.write("This is test file 2\n")
    
    # Start the server
    start_server()

