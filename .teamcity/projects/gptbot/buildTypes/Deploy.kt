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
                
                # Validate file exists
                if [ ! -f "${'$'}IMAGE_VERSIONS_FILE" ]; then
                    echo "❌ Error: ${'$'}IMAGE_VERSIONS_FILE not found"
                    exit 1
                fi
                
                # Generate tag key
                TAG_KEY="${'$'}APP_NAME-${'$'}CLUSTER_NAME-tag"
                echo "Updating key: ${'$'}TAG_KEY"
                
                # Update version
                if grep -q "^${'$'}TAG_KEY:" "${'$'}IMAGE_VERSIONS_FILE"; then
                    echo "Updating existing key..."
                    sed -i.bak "s/^${'$'}TAG_KEY:.*/${'$'}TAG_KEY: ${'$'}VERSION_TO_DEPLOY/" "${'$'}IMAGE_VERSIONS_FILE"
                    rm -f "${'$'}IMAGE_VERSIONS_FILE.bak"
                else
                    echo "Adding new key..."
                    echo "${'$'}TAG_KEY: ${'$'}VERSION_TO_DEPLOY" >> "${'$'}IMAGE_VERSIONS_FILE"
                fi
                
                echo "✅ Version updated successfully"
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
                
                # Push using token authentication (similar to personal access token approach)
                git push https://oauth2:%secure:CID_5d39b539391852e509211819e6a59e55%@github.com/dev4team-ai/gptbot-helm.git
                
                echo "✅ Changes pushed successfully"
            """.trimIndent()
        }
    }

})
