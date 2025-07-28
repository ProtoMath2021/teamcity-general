package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
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
        script {
            name = "Update Image Version"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                # Extract parameters
                APP_NAME="%env.APP_NAME%"
                CLUSTER_NAME="%env.CLUSTER_NAME%"
                VERSION_TO_DEPLOY="%env.VERSION_TO_DEPLOY%"
                IMAGE_VERSIONS_FILE="clusters/common/image-versions.yaml"
                
                echo "=== Deploy Configuration ==="
                echo "Application: ${'$'}APP_NAME"
                echo "Cluster: ${'$'}CLUSTER_NAME"
                echo "Version: ${'$'}VERSION_TO_DEPLOY"
                
                # Validate file exists and is a ConfigMap
                if [ ! -f "${'$'}IMAGE_VERSIONS_FILE" ]; then
                    echo "❌ Error: ${'$'}IMAGE_VERSIONS_FILE not found"
                    exit 1
                fi
                
                # Verify it's a ConfigMap structure
                if ! grep -q "kind: ConfigMap" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "⚠️  Warning: File doesn't appear to be a ConfigMap"
                fi
                
                if ! grep -q "^data:" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "❌ Error: No 'data:' section found in ConfigMap"
                    exit 1
                fi
                
                # Generate tag key
                TAG_KEY="${'$'}APP_NAME-${'$'}CLUSTER_NAME-tag"
                echo "Updating key: ${'$'}TAG_KEY"
                
                # Check current content
                echo "Current file content around the key:"
                grep -n "${'$'}TAG_KEY" "${'$'}IMAGE_VERSIONS_FILE" || echo "Key not found"
                
                # Create backup for rollback if needed
                cp "${'$'}IMAGE_VERSIONS_FILE" "${'$'}IMAGE_VERSIONS_FILE.original"
                
                # Update version in ConfigMap data section with more robust approach
                if grep -q "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "Updating existing key in ConfigMap..."
                    
                    # Use perl for more robust regex replacement that handles quotes properly
                    perl -i -pe "s/^(\\s*)\\Q${'$'}TAG_KEY\\E:\\s*.*$/"'$$1'"${'$'}TAG_KEY: \\\"${'$'}VERSION_TO_DEPLOY\\\"/g" "${'$'}IMAGE_VERSIONS_FILE"
                    
                    # Verify the update worked
                    if grep -q "^[[:space:]]*${'$'}TAG_KEY:[[:space:]]*\"${'$'}VERSION_TO_DEPLOY\"" "${'$'}IMAGE_VERSIONS_FILE"; then
                        echo "✅ Key updated successfully"
                    else
                        echo "❌ Update verification failed, rolling back..."
                        mv "${'$'}IMAGE_VERSIONS_FILE.original" "${'$'}IMAGE_VERSIONS_FILE"
                        exit 1
                    fi
                else
                    echo "Adding new key to ConfigMap data section..."
                    
                    # Find insertion point in data section (before first comment block)
                    LINE_NUM=$(grep -n "^[[:space:]]*# ===" "${'$'}IMAGE_VERSIONS_FILE" | head -1 | cut -d: -f1)
                    if [ -z "${'$'}LINE_NUM" ]; then
                        # If no comment sections, add before metadata or at end of data
                        LINE_NUM=$(grep -n "^[[:space:]]*# ==" "${'$'}IMAGE_VERSIONS_FILE" | head -1 | cut -d: -f1)
                        if [ -z "${'$'}LINE_NUM" ]; then
                            LINE_NUM=$(wc -l < "${'$'}IMAGE_VERSIONS_FILE")
                        fi
                    fi
                    
                    # Insert new key with proper indentation before the found line
                    sed -i.bak "${'$'}{${'$'}LINE_NUM}i\\
  ${'$'}TAG_KEY: \"${'$'}VERSION_TO_DEPLOY\"\\
" "${'$'}IMAGE_VERSIONS_FILE"
                    rm -f "${'$'}IMAGE_VERSIONS_FILE.bak"
                    echo "✅ Key added successfully"
                fi
                
                # Remove any duplicate entries (keep first occurrence)
                DUPLICATE_COUNT=$(grep -c "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE")
                if [ "${'$'}DUPLICATE_COUNT" -gt 1 ]; then
                    echo "⚠️  Found ${'$'}DUPLICATE_COUNT duplicate entries for ${'$'}TAG_KEY, cleaning up..."
                    
                    # Keep only the first occurrence
                    awk -v tag_key="${'$'}TAG_KEY" '
                    BEGIN { found = 0 }
                    /^[[:space:]]*'"${'$'}TAG_KEY"':/ {
                        if (found == 0) {
                            print
                            found = 1
                        }
                        next
                    }
                    { print }
                    ' "${'$'}IMAGE_VERSIONS_FILE" > "${'$'}IMAGE_VERSIONS_FILE.tmp"
                    mv "${'$'}IMAGE_VERSIONS_FILE.tmp" "${'$'}IMAGE_VERSIONS_FILE"
                    echo "✅ Duplicates removed"
                fi
                
                # Final verification and cleanup
                echo "=== Final Verification ==="
                FINAL_LINE=$(grep -n "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE" | head -1)
                echo "Result: ${'$'}FINAL_LINE"
                
                # Verify the change is correct
                if grep -q "^[[:space:]]*${'$'}TAG_KEY:[[:space:]]*\"${'$'}VERSION_TO_DEPLOY\"" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "✅ Version updated successfully: ${'$'}TAG_KEY = \"${'$'}VERSION_TO_DEPLOY\""
                    rm -f "${'$'}IMAGE_VERSIONS_FILE.original"
                else
                    echo "❌ Final verification failed, rolling back..."
                    mv "${'$'}IMAGE_VERSIONS_FILE.original" "${'$'}IMAGE_VERSIONS_FILE"
                    exit 1
                fi
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
