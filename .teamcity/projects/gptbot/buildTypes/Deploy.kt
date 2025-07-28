package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.*
import projects.gptbot.vcsRoots.GptbotHelmGit

object Deploy : BuildType({
    id("GptbotProject_Deploy")
    name = "Deploy"

    params {
        select("env.APP_NAME", "", display = ParameterDisplay.PROMPT,
                options = listOf("wwhatsapp-app", "gpt-agent-api", "gpt-agent-ui"))
        select("env.CLUSTER_NAME", "staging", 
               label = "Target Cluster", 
               description = "Select the cluster to deploy to", 
               display = ParameterDisplay.PROMPT,
               options = listOf("developing", "staging", "production"))
        text("env.VERSION_TO_DEPLOY", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
        password("env.GITHUB_TOKEN", "zxxaad55a75a688127f728578b85ed8fba2bb136a4b71d21129be199a31c20381df6dfd994ad67534b5775d03cbe80d301b", label = "GitHub Token", description = "GitHub Personal Access Token for repository access", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(GptbotHelmGit)
        cleanCheckout = true
    }

    steps {
        kotlinScript {
            name = "Update Image Version"
            content = """
                import java.io.File
                import kotlin.system.exitProcess
                
                // Configuration
                val appName = "%env.APP_NAME%"
                val clusterName = "%env.CLUSTER_NAME%"
                val versionToDeploy = "%env.VERSION_TO_DEPLOY%"
                val imageVersionsFile = File("clusters/common/image-versions.yaml")
                val tagKey = "${'$'}appName-${'$'}clusterName-tag"
                
                println("=== Updating ${'$'}tagKey to ${'$'}versionToDeploy ===")
                
                // Validate input parameters
                if (appName.isEmpty()) {
                    println("❌ APP_NAME parameter is required")
                    exitProcess(1)
                }
                if (clusterName.isEmpty()) {
                    println("❌ CLUSTER_NAME parameter is required")
                    exitProcess(1)
                }
                if (versionToDeploy.isEmpty()) {
                    println("❌ VERSION_TO_DEPLOY parameter is required")
                    exitProcess(1)
                }
                
                // Validate file exists
                if (!imageVersionsFile.exists()) {
                    println("❌ File not found: ${'$'}{imageVersionsFile.absolutePath}")
                    exitProcess(1)
                }
                
                try {
                    // Read the file content
                    val lines = imageVersionsFile.readLines().toMutableList()
                    val backupFile = File("${'$'}{imageVersionsFile.absolutePath}.bak")
                    
                    // Escape regex special characters in tagKey for safety
                    val escapedTagKey = Regex.escape(tagKey)
                    val tagPattern = Regex("^\\s*${'$'}escapedTagKey:\\s*")
                    val matchingLines = lines.mapIndexedNotNull { index, line ->
                        if (tagPattern.containsMatchIn(line)) index else null
                    }
                    
                    // Ensure we have exactly one match
                    when (matchingLines.size) {
                        0 -> {
                            println("❌ Key '${'$'}tagKey' not found in ${'$'}{imageVersionsFile.absolutePath}")
                            println("Available keys matching pattern '${'$'}appName-*-tag:'")
                            val availableKeys = lines.filter { 
                                it.contains("${'$'}appName-") && it.contains("-tag:") 
                            }
                            if (availableKeys.isEmpty()) {
                                println("  - No matching keys found")
                            } else {
                                availableKeys.forEach { println("  - ${'$'}{it.trim()}") }
                            }
                            exitProcess(1)
                        }
                        1 -> {
                            // Perfect - exactly one match found
                            val lineIndex = matchingLines[0]
                            val existingLine = lines[lineIndex]
                            val indentation = existingLine.takeWhile { it.isWhitespace() }
                            
                            println("Found existing key at line ${'$'}{lineIndex + 1}: ${'$'}{existingLine.trim()}")
                            
                            // Create backup before making changes
                            try {
                                imageVersionsFile.copyTo(backupFile, overwrite = true)
                            } catch (e: Exception) {
                                println("❌ Failed to create backup: ${'$'}{e.message}")
                                exitProcess(1)
                            }
                            
                            // Update the line
                            lines[lineIndex] = "${'$'}indentation${'$'}tagKey: \"${'$'}versionToDeploy\""
                            
                            // Write updated content
                            try {
                                imageVersionsFile.writeText(lines.joinToString("\n") + "\n")
                            } catch (e: Exception) {
                                println("❌ Failed to write file: ${'$'}{e.message}")
                                // Restore backup
                                if (backupFile.exists()) {
                                    backupFile.copyTo(imageVersionsFile, overwrite = true)
                                    backupFile.delete()
                                }
                                exitProcess(1)
                            }
                            
                            // Verify the update with exact match
                            val updatedContent = imageVersionsFile.readText()
                            val expectedLine = "${'$'}tagKey: \"${'$'}versionToDeploy\""
                            if (!updatedContent.contains(expectedLine)) {
                                println("❌ Update verification failed - could not find: ${'$'}expectedLine")
                                // Restore backup
                                if (backupFile.exists()) {
                                    backupFile.copyTo(imageVersionsFile, overwrite = true)
                                    backupFile.delete()
                                }
                                exitProcess(1)
                            }
                            
                            // Clean up backup
                            if (backupFile.exists()) {
                                backupFile.delete()
                            }
                            
                            println("✅ Successfully updated: ${'$'}tagKey = \"${'$'}versionToDeploy\"")
                            println("Updated line: ${'$'}{lines[lineIndex].trim()}")
                        }
                        else -> {
                            // Multiple matches - this should not happen with proper YAML
                            println("❌ Found ${'$'}{matchingLines.size} duplicate entries for '${'$'}tagKey':")
                            matchingLines.forEach { index ->
                                println("  Line ${'$'}{index + 1}: ${'$'}{lines[index].trim()}")
                            }
                            println("Please manually fix the duplicate entries in the YAML file.")
                            exitProcess(1)
                        }
                    }
                    
                } catch (e: Exception) {
                    println("❌ Update failed: ${'$'}{e.message}")
                    println("Stack trace: ${'$'}{e.stackTraceToString()}")
                    // Restore backup if it exists
                    val backupFile = File("${'$'}{imageVersionsFile.absolutePath}.bak")
                    if (backupFile.exists()) {
                        try {
                            println("🔄 Restoring backup...")
                            backupFile.copyTo(imageVersionsFile, overwrite = true)
                            backupFile.delete()
                            println("✅ Backup restored successfully")
                        } catch (restoreException: Exception) {
                            println("❌ Failed to restore backup: ${'$'}{restoreException.message}")
                        }
                    }
                    exitProcess(1)
                }
            """.trimIndent()
        }

        script {
            name = "Commit and Push Changes"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                # Extract parameters
                APP_NAME="%env.APP_NAME%"
                CLUSTER_NAME="%env.CLUSTER_NAME%"
                VERSION_TO_DEPLOY="%env.VERSION_TO_DEPLOY%"
                GITHUB_TOKEN="%env.GITHUB_TOKEN%"
                IMAGE_VERSIONS_FILE="clusters/common/image-versions.yaml"
                
                echo "=== Git Operations ==="
                echo "Configuring git..."
                
                # Configure git
                git config user.name "TeamCity"
                git config user.email "teamcity@dev4team.ai"
                
                # Check for changes
                echo "Checking for changes in ${'$'}IMAGE_VERSIONS_FILE..."
                if git diff --quiet "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "ℹ️  No changes to commit - file unchanged"
                    exit 0
                fi
                
                # Show what will be committed
                echo "📋 Changes to commit:"
                git diff "${'$'}IMAGE_VERSIONS_FILE"
                
                # Stage and commit changes
                echo "📝 Staging changes..."
                git add "${'$'}IMAGE_VERSIONS_FILE"
                
                COMMIT_MESSAGE="Deploy ${'$'}APP_NAME to ${'$'}CLUSTER_NAME: ${'$'}VERSION_TO_DEPLOY"
                echo "💾 Committing with message: ${'$'}COMMIT_MESSAGE"
                git commit -m "${'$'}COMMIT_MESSAGE"
                
                # Set up authenticated remote URL
                echo "🔐 Setting up GitHub authentication..."
                AUTHENTICATED_URL="https://${'$'}GITHUB_TOKEN@github.com/dev4team-ai/gptbot-helm.git"
                git remote set-url origin "${'$'}AUTHENTICATED_URL"
                
                # Push changes
                echo "🚀 Pushing changes to remote..."
                if git push origin main; then
                    echo "✅ Changes pushed successfully!"
                    
                    # Get the commit hash for reference
                    COMMIT_HASH=$(git rev-parse HEAD)
                    echo "📍 Commit hash: ${'$'}COMMIT_HASH"
                    echo "🔗 View commit: https://github.com/dev4team-ai/gptbot-helm/commit/${'$'}COMMIT_HASH"
                else
                    echo "❌ Failed to push changes"
                    exit 1
                fi
                
                echo "🎉 Deploy operation completed successfully!"
            """.trimIndent()
        }
    }

})
