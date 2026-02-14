/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simisinc.platform.application.cms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.http.HttpPostCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.GitPublishSettings;

/**
 * Publishes static site content to a Git repository
 *
 * @author matt rajkowski
 * @created 2/14/26 2:30 PM
 */
public class GitPublishCommand {

  private static Log LOG = LogFactory.getLog(GitPublishCommand.class);

  public static boolean publish(GitPublishSettings settings, String staticSiteZipPath) throws DataException {

    if (settings == null || !settings.getEnabled()) {
      LOG.debug("Git publishing is not enabled");
      return false;
    }

    if (StringUtils.isBlank(staticSiteZipPath)) {
      throw new DataException("Static site zip path is required");
    }

    try {
      // Create a temporary working directory
      File tempDir = new File(System.getProperty("java.io.tmpdir"), "git-publish-" + System.currentTimeMillis());
      tempDir.mkdirs();

      try {
        // Clone the repository
        cloneRepository(settings, tempDir);

        // Create or checkout the branch
        checkoutBranch(settings, tempDir);

        // Extract the static site to the target directory
        extractStaticSite(staticSiteZipPath, settings.getTargetDirectory(), tempDir);

        // Commit and push the changes
        commitAndPush(settings, tempDir);

        // Create a pull request if configured
        if (settings.getAutoCreatePr()) {
          createPullRequest(settings);
        }

        LOG.info("Successfully published static site to Git repository");
        return true;

      } finally {
        // Clean up the temporary directory
        try {
          FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
          LOG.warn("Could not delete temp directory: " + tempDir, e);
        }
      }

    } catch (Exception e) {
      LOG.error("Error publishing to Git", e);
      throw new DataException("Failed to publish to Git: " + e.getMessage());
    }
  }

  private static void cloneRepository(GitPublishSettings settings, File workDir) throws Exception {
    LOG.info("Cloning repository: " + settings.getRepositoryUrl());

    // Use Git credential helper for authentication instead of embedding token in URL
    // This is more secure as it avoids exposing tokens in logs
    String repoUrl = settings.getRepositoryUrl();

    // Execute git clone command without credentials in URL
    ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", repoUrl, workDir.getAbsolutePath());

    // Set up environment to pass credentials securely
    if (settings.getAccessToken() != null) {
      Map<String, String> env = pb.environment();
      env.put("GIT_TERMINAL_PROMPT", "0"); // Disable interactive prompts
      // For HTTPS, we'll configure credential helper in the cloned repo
    }

    executeGitCommand(pb, "clone repository");

    // Configure credential helper to use the access token
    if (settings.getAccessToken() != null) {
      configureCredentialHelper(settings, workDir);
    }
  }

  private static void checkoutBranch(GitPublishSettings settings, File workDir) throws Exception {
    LOG.info("Checking out branch: " + settings.getBranchName());

    // Try to checkout existing branch or create new one
    try {
      ProcessBuilder pb = new ProcessBuilder("git", "checkout", settings.getBranchName());
      pb.directory(workDir);
      executeGitCommand(pb, "checkout branch");
    } catch (Exception e) {
      // Branch doesn't exist, create it
      LOG.info("Branch does not exist, creating new branch: " + settings.getBranchName());
      ProcessBuilder pb = new ProcessBuilder("git", "checkout", "-b", settings.getBranchName());
      pb.directory(workDir);
      executeGitCommand(pb, "create branch");
    }
  }

  private static void extractStaticSite(String zipPath, String targetDirectory, File workDir) throws Exception {
    LOG.info("Extracting static site to: " + targetDirectory);

    // Get the zip file
    File zipFile = new File(FileSystemCommand.getFileServerRootPathValue() + zipPath);
    if (!zipFile.exists()) {
      throw new DataException("Static site zip file not found: " + zipFile);
    }

    // Determine the target directory within the git repo
    File targetDir = new File(workDir, targetDirectory);
    if (!targetDirectory.equals("/")) {
      targetDir.mkdirs();
    }

    // Clear existing content in target directory (except .git)
    if (targetDir.exists()) {
      File[] files = targetDir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (!file.getName().equals(".git")) {
            if (file.isDirectory()) {
              FileUtils.deleteDirectory(file);
            } else {
              file.delete();
            }
          }
        }
      }
    }

    // Unzip the static site content
    ProcessBuilder pb = new ProcessBuilder("unzip", "-q", zipFile.getAbsolutePath(), "-d", targetDir.getAbsolutePath());
    executeGitCommand(pb, "extract static site");

    // Move content from site/ subdirectory if it exists
    File siteDir = new File(targetDir, "site");
    if (siteDir.exists() && siteDir.isDirectory()) {
      File[] siteFiles = siteDir.listFiles();
      if (siteFiles != null) {
        for (File file : siteFiles) {
          File dest = new File(targetDir, file.getName());
          if (file.isDirectory()) {
            FileUtils.moveDirectory(file, dest);
          } else {
            FileUtils.moveFile(file, dest);
          }
        }
      }
      siteDir.delete();
    }
  }

  private static void commitAndPush(GitPublishSettings settings, File workDir) throws Exception {
    LOG.info("Committing and pushing changes");

    // Configure git user
    ProcessBuilder pb1 = new ProcessBuilder("git", "config", "user.name", settings.getUsername());
    pb1.directory(workDir);
    executeGitCommand(pb1, "configure git user.name");

    ProcessBuilder pb2 = new ProcessBuilder("git", "config", "user.email", settings.getEmail());
    pb2.directory(workDir);
    executeGitCommand(pb2, "configure git user.email");

    // Add all changes
    ProcessBuilder pb3 = new ProcessBuilder("git", "add", ".");
    pb3.directory(workDir);
    executeGitCommand(pb3, "add changes");

    // Generate commit message from template
    String commitMessage = generateMessage(settings.getCommitMessageTemplate());

    // Commit changes
    ProcessBuilder pb4 = new ProcessBuilder("git", "commit", "-m", commitMessage);
    pb4.directory(workDir);
    try {
      executeGitCommand(pb4, "commit changes");
    } catch (Exception e) {
      // Check if there are no changes to commit
      if (e.getMessage() != null && e.getMessage().contains("nothing to commit")) {
        LOG.info("No changes to commit");
        return;
      }
      throw e;
    }

    // Push changes using the configured remote (credentials already set up via credential helper)
    ProcessBuilder pb5 = new ProcessBuilder("git", "push", "origin", settings.getBranchName());
    pb5.directory(workDir);
    executeGitCommand(pb5, "push changes");
  }

  private static void configureCredentialHelper(GitPublishSettings settings, File workDir) throws Exception {
    // Use Git credential store to securely provide the access token
    // This avoids embedding the token in URLs or command-line arguments
    if (settings.getAccessToken() == null) {
      return;
    }

    // Configure git to use credential helper
    ProcessBuilder pb1 = new ProcessBuilder("git", "config", "credential.helper", "store");
    pb1.directory(workDir);
    executeGitCommand(pb1, "configure credential helper");

    // Extract the host from the repository URL
    String repoUrl = settings.getRepositoryUrl();
    String host = extractHostFromUrl(repoUrl);

    if (host != null) {
      // Write credentials to the Git credential store
      // Format: https://username:token@hostname
      File credentialFile = new File(workDir, ".git-credentials");
      String credentialLine = "https://" + settings.getUsername() + ":" + settings.getAccessToken() + "@" + host;
      try (java.io.FileWriter fw = new java.io.FileWriter(credentialFile)) {
        fw.write(credentialLine + "\n");
      }

      // Point Git to this credential file
      ProcessBuilder pb2 = new ProcessBuilder("git", "config", "credential.helper", "store --file=" + credentialFile.getAbsolutePath());
      pb2.directory(workDir);
      executeGitCommand(pb2, "configure credential store path");
    }
  }

  private static String extractHostFromUrl(String url) {
    try {
      // Extract hostname from URL
      // Examples: https://github.com/user/repo.git -> github.com
      //           https://gitlab.com/user/repo.git -> gitlab.com
      if (url.startsWith("https://")) {
        String withoutProtocol = url.substring(8); // Remove "https://"
        int slashIndex = withoutProtocol.indexOf('/');
        if (slashIndex > 0) {
          return withoutProtocol.substring(0, slashIndex);
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to extract host from URL: " + url, e);
    }
    return null;
  }

  private static void createPullRequest(GitPublishSettings settings) throws Exception {
    LOG.info("Creating pull request");

    String provider = settings.getGitProvider().toLowerCase();
    if ("github".equals(provider)) {
      createGitHubPullRequest(settings);
    } else if ("gitlab".equals(provider)) {
      createGitLabMergeRequest(settings);
    } else {
      LOG.warn("Pull request creation not supported for provider: " + provider);
    }
  }

  private static void createGitHubPullRequest(GitPublishSettings settings) throws Exception {
    // Extract owner and repo from URL
    // Format: https://github.com/owner/repo.git
    String repoUrl = settings.getRepositoryUrl();
    String[] parts = repoUrl.replace("https://github.com/", "").replace(".git", "").split("/");
    if (parts.length < 2) {
      throw new DataException("Invalid GitHub repository URL");
    }
    String owner = parts[0];
    String repo = parts[1];

    // Prepare API request
    String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/pulls";

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode body = mapper.createObjectNode();
    body.put("title", generateMessage(settings.getPrTitleTemplate()));
    body.put("body", generateMessage(settings.getPrDescriptionTemplate()));
    body.put("head", settings.getBranchName());
    body.put("base", settings.getBaseBranch());

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + settings.getAccessToken());
    headers.put("Accept", "application/vnd.github+json");
    headers.put("Content-Type", "application/json");

    String response = HttpPostCommand.execute(apiUrl, headers, body.toString());
    if (StringUtils.isBlank(response)) {
      throw new DataException("Failed to create pull request: empty response");
    }

    JsonNode responseJson = JsonCommand.fromString(response);
    if (responseJson.has("html_url")) {
      LOG.info("Pull request created: " + responseJson.get("html_url").asText());
    } else if (responseJson.has("message")) {
      LOG.warn("GitHub API response: " + responseJson.get("message").asText());
    }
  }

  private static void createGitLabMergeRequest(GitPublishSettings settings) throws Exception {
    // Extract project path from URL
    // Format: https://gitlab.com/owner/repo.git
    String repoUrl = settings.getRepositoryUrl();
    String projectPath = repoUrl.replace("https://gitlab.com/", "").replace(".git", "");
    String encodedPath = projectPath.replace("/", "%2F");

    // Prepare API request
    String apiUrl = "https://gitlab.com/api/v4/projects/" + encodedPath + "/merge_requests";

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode body = mapper.createObjectNode();
    body.put("title", generateMessage(settings.getPrTitleTemplate()));
    body.put("description", generateMessage(settings.getPrDescriptionTemplate()));
    body.put("source_branch", settings.getBranchName());
    body.put("target_branch", settings.getBaseBranch());

    Map<String, String> headers = new HashMap<>();
    headers.put("PRIVATE-TOKEN", settings.getAccessToken());
    headers.put("Content-Type", "application/json");

    String response = HttpPostCommand.execute(apiUrl, headers, body.toString());
    if (StringUtils.isBlank(response)) {
      throw new DataException("Failed to create merge request: empty response");
    }

    JsonNode responseJson = JsonCommand.fromString(response);
    if (responseJson.has("web_url")) {
      LOG.info("Merge request created: " + responseJson.get("web_url").asText());
    } else if (responseJson.has("message")) {
      LOG.warn("GitLab API response: " + responseJson.get("message").asText());
    }
  }

  private static String generateMessage(String template) {
    if (StringUtils.isBlank(template)) {
      return "Static site update";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String timestamp = sdf.format(new Date());
    return template.replace("${timestamp}", timestamp);
  }

  private static void executeGitCommand(ProcessBuilder pb, String description) throws Exception {
    pb.redirectErrorStream(true);
    Process process = pb.start();

    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
        LOG.debug(line);
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new Exception("Failed to " + description + " (exit code: " + exitCode + "): " + output);
    }
  }
}
