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
                
                # Update version in ConfigMap data section
                if grep -q "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "Updating existing key in ConfigMap..."
                    # Update the key in the data section, preserving indentation (typically 2 spaces)
                    sed -i.bak "s/^\\([[:space:]]*\\)${'$'}TAG_KEY:[[:space:]]*.*$/\\1${'$'}TAG_KEY: \"${'$'}VERSION_TO_DEPLOY\"/" "${'$'}IMAGE_VERSIONS_FILE"
                    rm -f "${'$'}IMAGE_VERSIONS_FILE.bak"
                    echo "Key updated successfully"
                else
                    echo "Adding new key to ConfigMap data section..."
                    # Find the data section and add the new key with proper indentation
                    # Insert before the closing sections (before # ======= lines or metadata)
                    awk -v tag_key="${'$'}TAG_KEY" -v version="${'$'}VERSION_TO_DEPLOY" '
                    /^[[:space:]]*# ===========================================/ && !inserted && in_data {
                        print "  " tag_key ": \"" version "\""
                        print ""
                        inserted = 1
                    }
                    /^data:/ { in_data = 1 }
                    /^metadata:/ { in_data = 0 }
                    { print }
                    END {
                        if (!inserted && in_data) {
                            print "  " tag_key ": \"" version "\""
                        }
                    }' "${'$'}IMAGE_VERSIONS_FILE" > "${'$'}IMAGE_VERSIONS_FILE.tmp"
                    mv "${'$'}IMAGE_VERSIONS_FILE.tmp" "${'$'}IMAGE_VERSIONS_FILE"
                    echo "Key added successfully"
                fi
                
                # Verify the change
                echo "After update:"
                grep -n "${'$'}TAG_KEY" "${'$'}IMAGE_VERSIONS_FILE"
                
                # Check for and remove any duplicate entries in ConfigMap
                DUPLICATE_COUNT=$(grep -c "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE" || echo "0")
                if [ "${'$'}DUPLICATE_COUNT" -gt 1 ]; then
                    echo "⚠️  Found ${'$'}DUPLICATE_COUNT duplicate entries for ${'$'}TAG_KEY, cleaning up..."
                    # Keep only the first occurrence and remove duplicates
                    awk -v tag_key="${'$'}TAG_KEY" '
                    /^[[:space:]]*'"${'$'}TAG_KEY"':/ {
                        if (!seen) {
                            print
                            seen = 1
                        }
                        next
                    }
                    { print }' "${'$'}IMAGE_VERSIONS_FILE" > "${'$'}IMAGE_VERSIONS_FILE.tmp"
                    mv "${'$'}IMAGE_VERSIONS_FILE.tmp" "${'$'}IMAGE_VERSIONS_FILE"
                    echo "Duplicates removed. Final result:"
                    grep -n "${'$'}TAG_KEY" "${'$'}IMAGE_VERSIONS_FILE"
                fi
                
                echo "✅ Version updated successfully"
                
                # Final verification - show the actual value that was set
                echo "=== Final Verification ==="
                FINAL_VALUE=$(grep "^[[:space:]]*${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE" | head -1)
                echo "Set: ${'$'}FINAL_VALUE"
                echo "Expected format: ${'$'}TAG_KEY: \"${'$'}VERSION_TO_DEPLOY\""
            """.trimIndent()
        }

        script {
            name = "Commit and Push Changes"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                APP_NAME="%env.APP_NAME%"
                CLUSTER_NAME="%env.CLUSTER_NAME%"
                IMAGE_VERSIONS_FILE="clusters/common/image-versions.yaml"
                
                # Configure git
                git config user.name "TeamCity"
                git config user.email "teamcity@dev4team.ai"
                
                # Check for changes
                if git diff --quiet "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "No changes to commit"
                    exit 0
                fi
                
                # Show what will be committed
                echo "Changes to commit:"
                git diff "${'$'}IMAGE_VERSIONS_FILE"
                
                # Commit and push
                git add "${'$'}IMAGE_VERSIONS_FILE"
                git commit -m "Deploy ${'$'}APP_NAME to ${'$'}CLUSTER_NAME: %env.VERSION_TO_DEPLOY%"
                
                # Push using GitHub token authentication
                GITHUB_TOKEN="%env.GITHUB_TOKEN%"
                git remote set-url origin "https://${'$'}GITHUB_TOKEN@github.com/dev4team-ai/gptbot-helm.git"
                git push origin main
                
                echo "✅ Changes pushed successfully"
            """.trimIndent()
        }
    }

})
