package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.vcsRoots.WwhatsappNodeVcs

object WwhatsappNode : BuildType({
    id("GptbotProject_WwhatsappNode")
    name = "wwhatsapp-node"

    // Set default parameter for version
    params {
        param("env.DOCKER_VERSION", "dev-0")    // Docker-compatible version
        param("teamcity.build.branch", "refs/heads/main")  // Configurable build branch, defaults to main
    }

    vcs {
        root(WwhatsappNodeVcs)

        cleanCheckout = true
    }

    steps {
        // Generate simple version: branch-build.number
        script {
            name = "Generate Version"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                echo "=== Generating simple version ==="
                
                # Extract branch name from refs
                BRANCH_REF="%teamcity.build.branch%"
                BRANCH_NAME=${'$'}{BRANCH_REF#refs/heads/}
                BRANCH_NAME=${'$'}{BRANCH_NAME#refs/tags/}
                echo "Branch name: ${'$'}BRANCH_NAME"
                
                # Clean branch name for version identifier
                CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]' | sed 's/^-*//; s/-*${'$'}//')
                
                # Generate simple version: branch-build.number
                DOCKER_VERSION="${'$'}CLEAN_BRANCH_NAME-%build.number%"
                
                echo "� Docker version: ${'$'}DOCKER_VERSION"
                
                # Set version parameters
                echo "##teamcity[setParameter name='env.DOCKER_VERSION' value='${'$'}DOCKER_VERSION']"
                
                echo "✅ Version generation complete"
            """.trimIndent()
        }

        // Docker build step - uses simple version
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile.optimized"
                }
                // Use simple version format: branch-build.number
                namesAndTags = """
                    protonmath/wwhatsapp-node:%env.DOCKER_VERSION%
                    protonmath/wwhatsapp-node:latest
                    registry.INTERNAL:5000/wwhatsapp-node:%env.DOCKER_VERSION%
                    registry.INTERNAL:5000/wwhatsapp-node:latest
                """.trimIndent()
            }
        }
        
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:%env.DOCKER_VERSION%"
            }
        }
        
        // Publish latest tag only for main branch
        dockerCommand {
            name = "publish latest tag"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:latest"
            }
            conditions {
                matches("teamcity.build.branch", "refs/heads/(main|master)")
            }
        }

        // Push to private registry
        dockerCommand {
            name = "publish to private registry"
            commandType = push {
                namesAndTags = "registry.INTERNAL:5000/wwhatsapp-node:%env.DOCKER_VERSION%"
            }
        }

        // Publish latest tag to private registry only for main branch
        dockerCommand {
            name = "publish latest to private registry"
            commandType = push {
                namesAndTags = "registry.INTERNAL:5000/wwhatsapp-node:latest"
            }
            conditions {
                matches("teamcity.build.branch", "refs/heads/(main|master)")
            }
        }
    }

    triggers {
        vcs {
            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
        
        vcsLabeling {
            vcsRootId = "${WwhatsappNodeVcs.id}"
            labelingPattern = "%env.DOCKER_VERSION%"
            successfulOnly = true
            branchFilter = """
                +:refs/heads/main
                +:refs/heads/master
            """.trimIndent()
        }
    }

    requirements {
        // Require Docker capability for building and pushing images
        exists("docker.server.version")
        // Prefer nodejs-build agents but allow any agent with Docker
        equals("env.AGENT_TYPE", "nodejs-build")
    }
})
