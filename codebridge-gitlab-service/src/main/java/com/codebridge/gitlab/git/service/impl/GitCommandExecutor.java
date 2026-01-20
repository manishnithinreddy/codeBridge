package com.codebridge.gitlab.git.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for executing Git commands.
 */
@Component
@Slf4j
public class GitCommandExecutor {

    /**
     * Execute a Git command and return the output.
     *
     * @param workingDir The directory to execute the command in
     * @param command The command to execute
     * @return The output of the command
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public String executeGitCommand(File workingDir, String... command) throws IOException, InterruptedException {
        String[] fullCommand = new String[command.length + 1];
        fullCommand[0] = "git";
        System.arraycopy(command, 0, fullCommand, 1, command.length);
        
        log.debug("Executing Git command: {}", Arrays.toString(fullCommand));
        
        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand)
                .directory(workingDir)
                .redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Git command timed out: " + Arrays.toString(fullCommand));
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git command failed with exit code " + exitCode + ": " + Arrays.toString(fullCommand) + "\nOutput: " + output);
        }
        
        return output.toString().trim();
    }
    
    /**
     * Get the hash of a stash.
     *
     * @param workingDir The Git repository directory
     * @param stashRef The stash reference (e.g., "stash@{0}")
     * @return The hash of the stash
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public String getStashHash(File workingDir, String stashRef) throws IOException, InterruptedException {
        return executeGitCommand(workingDir, "rev-parse", stashRef);
    }
    
    /**
     * Create a stash and return its hash.
     *
     * @param workingDir The Git repository directory
     * @param message The stash message
     * @return The hash of the created stash
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public String createStash(File workingDir, String message) throws IOException, InterruptedException {
        executeGitCommand(workingDir, "stash", "push", "-m", message);
        return getStashHash(workingDir, "stash@{0}");
    }
    
    /**
     * Store a commit as a stash.
     *
     * @param workingDir The Git repository directory
     * @param commitHash The hash of the commit to store as a stash
     * @param message The stash message
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public void storeStash(File workingDir, String commitHash, String message) throws IOException, InterruptedException {
        executeGitCommand(workingDir, "stash", "store", "-m", message, commitHash);
    }
    
    /**
     * Apply a stash.
     *
     * @param workingDir The Git repository directory
     * @param stashRef The stash reference (e.g., "stash@{0}")
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public void applyStash(File workingDir, String stashRef) throws IOException, InterruptedException {
        executeGitCommand(workingDir, "stash", "apply", stashRef);
    }
    
    /**
     * Fetch a commit from a remote repository.
     *
     * @param workingDir The Git repository directory
     * @param remote The name of the remote
     * @param commitHash The hash of the commit to fetch
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public void fetchCommit(File workingDir, String remote, String commitHash) throws IOException, InterruptedException {
        executeGitCommand(workingDir, "fetch", remote, commitHash);
    }
    
    /**
     * Check if a commit exists in the repository.
     *
     * @param workingDir The Git repository directory
     * @param commitHash The hash of the commit to check
     * @return True if the commit exists, false otherwise
     */
    public boolean commitExists(File workingDir, String commitHash) {
        try {
            executeGitCommand(workingDir, "cat-file", "-e", commitHash);
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Get the current branch name.
     *
     * @param workingDir The Git repository directory
     * @return The name of the current branch
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    public String getCurrentBranch(File workingDir) throws IOException, InterruptedException {
        return executeGitCommand(workingDir, "rev-parse", "--abbrev-ref", "HEAD");
    }
}

