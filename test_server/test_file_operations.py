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
import io

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

def test_ssh_command_execution():
    """Test command execution over SSH using direct exec_command"""
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
        
        # Test directory creation
        test_dir = f"test_dir_{uuid.uuid4().hex[:8]}"
        logger.info(f"Creating directory: {test_dir}")
        
        # Execute command to create directory
        stdin, stdout, stderr = client.exec_command(f"mkdir -p {test_dir}")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            error = stderr.read().decode()
            logger.error(f"Failed to create directory: {error}")
        else:
            logger.info(f"Directory {test_dir} created successfully")
        
        # Execute command to list files
        logger.info("Listing files...")
        stdin, stdout, stderr = client.exec_command("ls -la")
        output = stdout.read().decode()
        logger.info(f"Directory listing: {output}")
        
        # Create a test file
        test_file = f"{test_dir}/test_file.txt"
        test_content = f"This is a test file created at {time.time()}"
        logger.info(f"Creating file: {test_file}")
        
        # Execute command to create file
        stdin, stdout, stderr = client.exec_command(f"echo '{test_content}' > {test_file}")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            error = stderr.read().decode()
            logger.error(f"Failed to create file: {error}")
        else:
            logger.info(f"File {test_file} created successfully")
        
        # Read the file content
        logger.info(f"Reading file: {test_file}")
        stdin, stdout, stderr = client.exec_command(f"cat {test_file}")
        output = stdout.read().decode()
        logger.info(f"File content: {output}")
        
        # Verify content
        if output.strip() == test_content:
            logger.info("File content verification successful")
        else:
            logger.error(f"File content verification failed. Expected: {test_content}, Got: {output}")
        
        # Clean up
        logger.info(f"Removing test directory: {test_dir}")
        stdin, stdout, stderr = client.exec_command(f"rm -rf {test_dir}")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            error = stderr.read().decode()
            logger.error(f"Failed to remove directory: {error}")
        else:
            logger.info(f"Directory {test_dir} removed successfully")
        
        # Close the connection
        client.close()
        
        logger.info("SSH command execution test completed successfully!")
        return True
    
    except Exception as e:
        logger.error(f"SSH command execution test failed: {str(e)}")
        return False

def test_ssh_file_operations():
    """Test file operations over SSH using direct file transfer"""
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
        
        # Create a test directory using exec_command
        test_dir = f"test_dir_{uuid.uuid4().hex[:8]}"
        logger.info(f"Creating directory: {test_dir}")
        stdin, stdout, stderr = client.exec_command(f"mkdir -p {test_dir}")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            error = stderr.read().decode()
            logger.error(f"Failed to create directory: {error}")
            return False
        
        # Create a test file locally
        test_file_name = f"test_file_{uuid.uuid4().hex[:8]}.txt"
        test_file_content = f"This is a test file created at {time.time()}"
        
        with open(test_file_name, "w") as f:
            f.write(test_file_content)
        
        # Upload the file using SCP
        logger.info(f"Uploading file: {test_file_name} to {test_dir}")
        
        # Create SFTP client
        sftp = client.open_sftp()
        
        # Upload the file
        remote_path = f"{test_dir}/{test_file_name}"
        sftp.put(test_file_name, remote_path)
        logger.info(f"File uploaded to: {remote_path}")
        
        # Verify the file was uploaded
        stdin, stdout, stderr = client.exec_command(f"ls -la {test_dir}")
        output = stdout.read().decode()
        logger.info(f"Directory listing: {output}")
        
        # Read the file content
        stdin, stdout, stderr = client.exec_command(f"cat {remote_path}")
        output = stdout.read().decode()
        logger.info(f"File content: {output}")
        
        # Verify content
        if output.strip() == test_file_content:
            logger.info("File content verification successful")
        else:
            logger.error(f"File content verification failed. Expected: {test_file_content}, Got: {output}")
        
        # Download the file
        download_file_name = f"downloaded_{test_file_name}"
        logger.info(f"Downloading file to: {download_file_name}")
        sftp.get(remote_path, download_file_name)
        
        # Verify the downloaded content
        with open(download_file_name, "r") as f:
            content = f.read()
        
        logger.info(f"Downloaded file content: {content}")
        if content.strip() == test_file_content:
            logger.info("Downloaded file content verification successful")
        else:
            logger.error(f"Downloaded file content verification failed. Expected: {test_file_content}, Got: {content}")
        
        # Rename the file
        new_file_name = f"renamed_{test_file_name}"
        new_remote_path = f"{test_dir}/{new_file_name}"
        logger.info(f"Renaming file from {remote_path} to {new_remote_path}")
        sftp.rename(remote_path, new_remote_path)
        
        # Verify the file was renamed
        stdin, stdout, stderr = client.exec_command(f"ls -la {test_dir}")
        output = stdout.read().decode()
        logger.info(f"Directory listing after rename: {output}")
        
        # Delete the file
        logger.info(f"Deleting file: {new_remote_path}")
        sftp.remove(new_remote_path)
        
        # Verify the file was deleted
        stdin, stdout, stderr = client.exec_command(f"ls -la {test_dir}")
        output = stdout.read().decode()
        logger.info(f"Directory listing after delete: {output}")
        
        # Clean up
        logger.info(f"Removing test directory: {test_dir}")
        stdin, stdout, stderr = client.exec_command(f"rm -rf {test_dir}")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            error = stderr.read().decode()
            logger.error(f"Failed to remove directory: {error}")
        
        # Clean up local files
        os.remove(test_file_name)
        os.remove(download_file_name)
        
        # Close connections
        sftp.close()
        client.close()
        
        logger.info("SSH file operations test completed successfully!")
        return True
    
    except Exception as e:
        logger.error(f"SSH file operations test failed: {str(e)}")
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
