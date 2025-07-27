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
        param("env.APP_NAME", "wwhatsapp-app")
        select("env.CLUSTER_NAME", "developing", 
               label = "Target Cluster", 
               description = "Select the cluster to deploy to",
               options = listOf("developing", "staging", "production"))
        param("env.VERSION_TO_DEPLOY", "")
        text("reverse.dep.*.env.DOCKER_VERSION", "", 
             label = "Docker Version from Dependency", 
             description = "Docker version from the upstream build", 
             allowEmpty = true)
        text("teamcity.build.number", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
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
                DOCKER_VERSION="%reverse.dep.*.env.DOCKER_VERSION%"
                IMAGE_VERSIONS_FILE="clusters/common/image-versions.yaml"
                
                echo "=== Deploy Configuration ==="
                echo "Application: ${'$'}APP_NAME"
                echo "Cluster: ${'$'}CLUSTER_NAME"
                
                # Determine version to deploy
                if [ -z "${'$'}VERSION_TO_DEPLOY" ] && [ -n "${'$'}DOCKER_VERSION" ]; then
                    VERSION_TO_DEPLOY="${'$'}DOCKER_VERSION"
                    echo "Using Docker version from dependency: ${'$'}VERSION_TO_DEPLOY"
                elif [ -n "${'$'}VERSION_TO_DEPLOY" ]; then
                    echo "Using provided version: ${'$'}VERSION_TO_DEPLOY"
                else
                    echo "❌ Error: No version specified. Set VERSION_TO_DEPLOY or add dependency"
                    exit 1
                fi
                
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
                git commit -m "Deploy ${'$'}APP_NAME to ${'$'}CLUSTER_NAME: ${'$'}ACTUAL_VERSION

                Build: %teamcity.build.number%
                
                git push origin main
                
                echo "✅ Changes pushed successfully"
            """.trimIndent()
        }
    }

    requirements {
        exists("git")
        contains("env.AGENT_TYPE", "general-build")
    }
})
