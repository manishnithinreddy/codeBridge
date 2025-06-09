#!/usr/bin/env python3

import os
import sys
import time
import paramiko
import ftplib
import logging
import uuid
import threading
import subprocess

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Server connection details
SSH_HOST = "localhost"
SSH_PORT = 2222
SSH_USERNAME = "testuser"
SSH_PASSWORD = "testpassword"

FTP_HOST = "localhost"
FTP_PORT = 2121
FTP_USERNAME = "testuser"
FTP_PASSWORD = "testpassword"

def start_test_servers():
    """Start the test SSH and FTP servers in the background"""
    logger.info("Starting test servers...")
    
    # Start the servers in a separate process
    server_process = subprocess.Popen(
        ["python", "start_servers.py"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    
    # Wait for servers to start
    time.sleep(3)
    
    return server_process

def stop_test_servers(server_process):
    """Stop the test servers"""
    logger.info("Stopping test servers...")
    server_process.terminate()
    server_process.wait()

def test_ssh_connection():
    """Test SSH connection to the server"""
    logger.info("Testing SSH connection...")
    
    try:
        # Create SSH client
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        
        # Connect to the server
        client.connect(
            hostname=SSH_HOST,
            port=SSH_PORT,
            username=SSH_USERNAME,
            password=SSH_PASSWORD,
            timeout=5
        )
        
        logger.info("SSH connection successful!")
        
        # Close the connection
        client.close()
        return True
    
    except Exception as e:
        logger.error(f"SSH connection failed: {str(e)}")
        return False

def test_ssh_file_operations():
    """Test file operations over SSH"""
    logger.info("Testing SSH file operations...")
    
    try:
        # Create SSH client
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        
        # Connect to the server
        client.connect(
            hostname=SSH_HOST,
            port=SSH_PORT,
            username=SSH_USERNAME,
            password=SSH_PASSWORD,
            timeout=5
        )
        
        # Create SFTP client
        sftp = client.open_sftp()
        
        # List files
        logger.info("Listing files...")
        files = sftp.listdir("files")
        logger.info(f"Files in directory: {files}")
        
        # Create a test file
        test_file_name = f"test_file_{uuid.uuid4().hex[:8]}.txt"
        test_file_path = f"files/{test_file_name}"
        test_file_content = f"This is a test file created at {time.time()}"
        
        logger.info(f"Creating test file: {test_file_path}")
        with sftp.file(test_file_path, "w") as f:
            f.write(test_file_content)
        
        # Read the file back
        logger.info(f"Reading test file: {test_file_path}")
        with sftp.file(test_file_path, "r") as f:
            content = f.read()
        
        logger.info(f"File content: {content}")
        
        # Verify content
        assert content == test_file_content, "File content doesn't match"
        
        # Rename the file
        new_file_name = f"renamed_{test_file_name}"
        new_file_path = f"files/{new_file_name}"
        
        logger.info(f"Renaming file from {test_file_path} to {new_file_path}")
        sftp.rename(test_file_path, new_file_path)
        
        # Verify the file was renamed
        files = sftp.listdir("files")
        assert new_file_name in files, "Renamed file not found"
        
        # Delete the file
        logger.info(f"Deleting file: {new_file_path}")
        sftp.remove(new_file_path)
        
        # Verify the file was deleted
        files = sftp.listdir("files")
        assert new_file_name not in files, "File was not deleted"
        
        # Create a directory
        test_dir_name = f"test_dir_{uuid.uuid4().hex[:8]}"
        test_dir_path = f"files/{test_dir_name}"
        
        logger.info(f"Creating directory: {test_dir_path}")
        sftp.mkdir(test_dir_path)
        
        # Verify the directory was created
        files = sftp.listdir("files")
        assert test_dir_name in files, "Directory was not created"
        
        # Create a file in the directory
        inner_file_name = "inner_test_file.txt"
        inner_file_path = f"{test_dir_path}/{inner_file_name}"
        inner_file_content = "This is a file inside the test directory"
        
        logger.info(f"Creating file in directory: {inner_file_path}")
        with sftp.file(inner_file_path, "w") as f:
            f.write(inner_file_content)
        
        # Verify the file was created
        files = sftp.listdir(test_dir_path)
        assert inner_file_name in files, "Inner file was not created"
        
        # Delete the file
        logger.info(f"Deleting file: {inner_file_path}")
        sftp.remove(inner_file_path)
        
        # Delete the directory
        logger.info(f"Deleting directory: {test_dir_path}")
        sftp.rmdir(test_dir_path)
        
        # Verify the directory was deleted
        files = sftp.listdir("files")
        assert test_dir_name not in files, "Directory was not deleted"
        
        # Close the connection
        sftp.close()
        client.close()
        
        logger.info("SSH file operations test completed successfully!")
        return True
    
    except Exception as e:
        logger.error(f"SSH file operations test failed: {str(e)}")
        return False

def test_ssh_command_execution():
    """Test command execution over SSH"""
    logger.info("Testing SSH command execution...")
    
    try:
        # Create SSH client
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        
        # Connect to the server
        client.connect(
            hostname=SSH_HOST,
            port=SSH_PORT,
            username=SSH_USERNAME,
            password=SSH_PASSWORD,
            timeout=5
        )
        
        # Execute commands
        commands = [
            "ls -la files",
            "echo 'Hello, world!' > files/hello.txt",
            "cat files/hello.txt",
            "mkdir -p files/test_dir",
            "ls -la files",
            "rm files/hello.txt",
            "rmdir files/test_dir",
            "ls -la files"
        ]
        
        for command in commands:
            logger.info(f"Executing command: {command}")
            stdin, stdout, stderr = client.exec_command(command)
            
            # Get the output
            output = stdout.read().decode().strip()
            error = stderr.read().decode().strip()
            
            if error:
                logger.warning(f"Command error: {error}")
            
            logger.info(f"Command output: {output}")
        
        # Close the connection
        client.close()
        
        logger.info("SSH command execution test completed successfully!")
        return True
    
    except Exception as e:
        logger.error(f"SSH command execution test failed: {str(e)}")
        return False

def test_ftp_connection():
    """Test FTP connection to the server"""
    logger.info("Testing FTP connection...")
    
    try:
        # Create FTP client
        ftp = ftplib.FTP()
        
        # Connect to the server
        ftp.connect(FTP_HOST, FTP_PORT)
        ftp.login(FTP_USERNAME, FTP_PASSWORD)
        
        logger.info(f"FTP connection successful! Welcome message: {ftp.getwelcome()}")
        
        # Close the connection
        ftp.quit()
        return True
    
    except Exception as e:
        logger.error(f"FTP connection failed: {str(e)}")
        return False

def test_ftp_file_operations():
    """Test file operations over FTP"""
    logger.info("Testing FTP file operations...")
    
    try:
        # Create FTP client
        ftp = ftplib.FTP()
        
        # Connect to the server
        ftp.connect(FTP_HOST, FTP_PORT)
        ftp.login(FTP_USERNAME, FTP_PASSWORD)
        
        # List files
        logger.info("Listing files...")
        files = ftp.nlst()
        logger.info(f"Files in directory: {files}")
        
        # Create a test file locally
        test_file_name = f"test_file_{uuid.uuid4().hex[:8]}.txt"
        test_file_content = f"This is a test file created at {time.time()}"
        
        with open(test_file_name, "w") as f:
            f.write(test_file_content)
        
        # Upload the file
        logger.info(f"Uploading file: {test_file_name}")
        with open(test_file_name, "rb") as f:
            ftp.storbinary(f"STOR {test_file_name}", f)
        
        # Verify the file was uploaded
        files = ftp.nlst()
        assert test_file_name in files, "Uploaded file not found"
        
        # Download the file
        download_file_name = f"downloaded_{test_file_name}"
        
        logger.info(f"Downloading file to: {download_file_name}")
        with open(download_file_name, "wb") as f:
            ftp.retrbinary(f"RETR {test_file_name}", f.write)
        
        # Verify the downloaded content
        with open(download_file_name, "r") as f:
            content = f.read()
        
        logger.info(f"Downloaded file content: {content}")
        assert content == test_file_content, "Downloaded file content doesn't match"
        
        # Rename the file
        new_file_name = f"renamed_{test_file_name}"
        
        logger.info(f"Renaming file from {test_file_name} to {new_file_name}")
        ftp.rename(test_file_name, new_file_name)
        
        # Verify the file was renamed
        files = ftp.nlst()
        assert new_file_name in files, "Renamed file not found"
        assert test_file_name not in files, "Original file still exists"
        
        # Delete the file
        logger.info(f"Deleting file: {new_file_name}")
        ftp.delete(new_file_name)
        
        # Verify the file was deleted
        files = ftp.nlst()
        assert new_file_name not in files, "File was not deleted"
        
        # Create a directory
        test_dir_name = f"test_dir_{uuid.uuid4().hex[:8]}"
        
        logger.info(f"Creating directory: {test_dir_name}")
        ftp.mkd(test_dir_name)
        
        # Verify the directory was created
        files = ftp.nlst()
        assert test_dir_name in files, "Directory was not created"
        
        # Change to the directory
        logger.info(f"Changing to directory: {test_dir_name}")
        ftp.cwd(test_dir_name)
        
        # Upload a file to the directory
        inner_file_name = "inner_test_file.txt"
        inner_file_content = "This is a file inside the test directory"
        
        with open(inner_file_name, "w") as f:
            f.write(inner_file_content)
        
        logger.info(f"Uploading file to directory: {inner_file_name}")
        with open(inner_file_name, "rb") as f:
            ftp.storbinary(f"STOR {inner_file_name}", f)
        
        # Verify the file was uploaded
        files = ftp.nlst()
        assert inner_file_name in files, "Inner file was not uploaded"
        
        # Delete the file
        logger.info(f"Deleting file: {inner_file_name}")
        ftp.delete(inner_file_name)
        
        # Go back to the parent directory
        logger.info("Changing to parent directory")
        ftp.cwd("..")
        
        # Delete the directory
        logger.info(f"Deleting directory: {test_dir_name}")
        ftp.rmd(test_dir_name)
        
        # Verify the directory was deleted
        files = ftp.nlst()
        assert test_dir_name not in files, "Directory was not deleted"
        
        # Clean up local files
        os.remove(test_file_name)
        os.remove(download_file_name)
        os.remove(inner_file_name)
        
        # Close the connection
        ftp.quit()
        
        logger.info("FTP file operations test completed successfully!")
        return True
    
    except Exception as e:
        logger.error(f"FTP file operations test failed: {str(e)}")
        return False

def main():
    # Start the test servers
    server_process = start_test_servers()
    
    try:
        # Run the tests
        tests = [
            ("SSH Connection", test_ssh_connection),
            ("SSH File Operations", test_ssh_file_operations),
            ("SSH Command Execution", test_ssh_command_execution),
            ("FTP Connection", test_ftp_connection),
            ("FTP File Operations", test_ftp_file_operations)
        ]
        
        results = {}
        
        for test_name, test_func in tests:
            logger.info(f"\n{'=' * 50}\nRunning test: {test_name}\n{'=' * 50}")
            result = test_func()
            results[test_name] = result
        
        # Print summary
        logger.info("\n\n" + "=" * 50)
        logger.info("TEST RESULTS SUMMARY")
        logger.info("=" * 50)
        
        all_passed = True
        
        for test_name, result in results.items():
            status = "PASSED" if result else "FAILED"
            if not result:
                all_passed = False
            logger.info(f"{test_name}: {status}")
        
        logger.info("=" * 50)
        logger.info(f"Overall result: {'PASSED' if all_passed else 'FAILED'}")
        logger.info("=" * 50)
    
    finally:
        # Stop the test servers
        stop_test_servers(server_process)

if __name__ == "__main__":
    main()

