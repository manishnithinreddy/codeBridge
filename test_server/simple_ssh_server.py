#!/usr/bin/env python3

import os
import socket
import sys
import threading
import paramiko
import logging

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Generate a test key
host_key = paramiko.RSAKey.generate(2048)

class SimpleSSHServer(paramiko.ServerInterface):
    def __init__(self):
        self.event = threading.Event()
        
    def check_channel_request(self, kind, chanid):
        if kind == 'session':
            return paramiko.OPEN_SUCCEEDED
        return paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED
    
    def check_auth_password(self, username, password):
        if username == 'testuser' and password == 'testpassword':
            return paramiko.AUTH_SUCCESSFUL
        return paramiko.AUTH_FAILED
    
    def check_auth_publickey(self, username, key):
        return paramiko.AUTH_SUCCESSFUL
    
    def get_allowed_auths(self, username):
        return 'password,publickey'
    
    def check_channel_shell_request(self, channel):
        self.event.set()
        return True
    
    def check_channel_pty_request(self, channel, term, width, height, pixelwidth, pixelheight, modes):
        return True
    
    def check_channel_exec_request(self, channel, command):
        command_str = command.decode('utf-8')
        logger.info(f"Executing command: {command_str}")
        
        if command_str.startswith('ls'):
            path = '.'
            if len(command_str) > 3:
                path = command_str[3:].strip()
            
            try:
                files = os.listdir(path)
                output = '\n'.join(files)
                channel.send(output + '\n')
            except Exception as e:
                channel.send(f"Error: {str(e)}\n")
        
        elif command_str.startswith('mkdir'):
            if len(command_str) > 6:
                dir_name = command_str[6:].strip()
                try:
                    os.makedirs(dir_name, exist_ok=True)
                    channel.send(f"Directory {dir_name} created\n")
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: mkdir <directory>\n")
        
        elif command_str.startswith('echo'):
            if len(command_str) > 5:
                content = command_str[5:].strip()
                if '>' in content:
                    parts = content.split('>', 1)
                    text = parts[0].strip()
                    file_name = parts[1].strip()
                    try:
                        with open(file_name, 'w') as f:
                            f.write(text)
                        channel.send(f"Wrote to {file_name}\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\n")
                else:
                    channel.send(content + '\n')
            else:
                channel.send('\n')
        
        elif command_str.startswith('cat'):
            if len(command_str) > 4:
                file_name = command_str[4:].strip()
                try:
                    with open(file_name, 'r') as f:
                        content = f.read()
                    channel.send(content + '\n')
                except Exception as e:
                    channel.send(f"Error: {str(e)}\n")
            else:
                channel.send("Usage: cat <file>\n")
        
        else:
            channel.send(f"Command executed: {command_str}\n")
        
        channel.send_exit_status(0)
        channel.close()
        return True

def start_server(port=2222):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(('0.0.0.0', port))
        sock.listen(100)
        logger.info(f"Listening for connections on port {port}...")
        
        # Create test files directory
        os.makedirs('files', exist_ok=True)
        with open('files/test1.txt', 'w') as f:
            f.write('This is test file 1\n')
        with open('files/test2.txt', 'w') as f:
            f.write('This is test file 2\n')
        
        while True:
            client, addr = sock.accept()
            logger.info(f'Connection from {addr[0]}:{addr[1]}')
            
            t = threading.Thread(target=handle_connection, args=(client,))
            t.daemon = True
            t.start()
    
    except KeyboardInterrupt:
        logger.info('Server shutting down...')
        sys.exit(0)
    except Exception as e:
        logger.error(f'Error: {str(e)}')
        sys.exit(1)

def handle_connection(client):
    try:
        transport = paramiko.Transport(client)
        transport.add_server_key(host_key)
        
        server = SimpleSSHServer()
        transport.start_server(server=server)
        
        channel = transport.accept(20)
        if channel is None:
            logger.warning('No channel established')
            return
        
        server.event.wait(10)
        if not server.event.is_set():
            logger.warning('No shell requested')
            return
        
        channel.send('Welcome to the simple SSH server!\r\n')
        channel.send('Type "exit" to disconnect\r\n')
        
        f = channel.makefile('rU')
        while True:
            channel.send('$ ')
            command = f.readline().strip('\r\n')
            if command == 'exit':
                break
            
            if command.startswith('ls'):
                path = '.'
                if len(command) > 3:
                    path = command[3:].strip()
                
                try:
                    files = os.listdir(path)
                    output = '\n'.join(files)
                    channel.send(output + '\r\n')
                except Exception as e:
                    channel.send(f"Error: {str(e)}\r\n")
            
            elif command.startswith('mkdir'):
                if len(command) > 6:
                    dir_name = command[6:].strip()
                    try:
                        os.makedirs(dir_name, exist_ok=True)
                        channel.send(f"Directory {dir_name} created\r\n")
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: mkdir <directory>\r\n")
            
            elif command.startswith('echo'):
                if len(command) > 5:
                    content = command[5:].strip()
                    if '>' in content:
                        parts = content.split('>', 1)
                        text = parts[0].strip()
                        file_name = parts[1].strip()
                        try:
                            with open(file_name, 'w') as f:
                                f.write(text)
                            channel.send(f"Wrote to {file_name}\r\n")
                        except Exception as e:
                            channel.send(f"Error: {str(e)}\r\n")
                    else:
                        channel.send(content + '\r\n')
                else:
                    channel.send('\r\n')
            
            elif command.startswith('cat'):
                if len(command) > 4:
                    file_name = command[4:].strip()
                    try:
                        with open(file_name, 'r') as f:
                            content = f.read()
                        channel.send(content + '\r\n')
                    except Exception as e:
                        channel.send(f"Error: {str(e)}\r\n")
                else:
                    channel.send("Usage: cat <file>\r\n")
            
            else:
                channel.send(f"Command executed: {command}\r\n")
        
        channel.close()
    
    except Exception as e:
        logger.error(f'Error: {str(e)}')
    finally:
        try:
            transport.close()
        except:
            pass

if __name__ == '__main__':
    start_server()

