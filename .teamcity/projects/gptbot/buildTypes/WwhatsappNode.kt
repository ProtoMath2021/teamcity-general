package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.vcsRoots.WwhatsappNodeGit

object WwhatsappNode : BuildType({
    id("GptbotProject_WwhatsappNode")
    name = "wwhatsapp-node"

    vcs {
        root(WwhatsappNodeGit)

        cleanCheckout = true
    }

    steps {
        // Extract semantic version from package.json
        script {
            name = "Extract Semantic Version"
            scriptContent = """
                #!/bin/bash
                set -e
                
                # Get version from package.json and add git commit hash
                if [ -f "package.json" ]; then
                    PACKAGE_VERSION=${'$'}(node -p "require('./package.json').version")
                    GIT_HASH=${'$'}(git rev-parse --short HEAD)
                    SEMANTIC_VERSION="${'$'}PACKAGE_VERSION-${'$'}GIT_HASH"
                else
                    # Fallback to build number if no package.json
                    SEMANTIC_VERSION="%build.number%"
                fi
                
                echo "Semantic version: ${'$'}SEMANTIC_VERSION"
                echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
            """.trimIndent()
        }

        // Docker build step - Dockerfile.optimized handles dependency installation and building
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile.optimized"  // Use the optimized multi-stage Dockerfile
                }
                // Use semantic version from Git tag, fallback to build number
                namesAndTags = """
                    protonmath/wwhatsapp-node:%env.SEMANTIC_VERSION%
                    protonmath/wwhatsapp-node:latest
                """.trimIndent()
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = """
                    protonmath/wwhatsapp-node:%env.SEMANTIC_VERSION%
                    protonmath/wwhatsapp-node:latest
                """.trimIndent()
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
    }

    requirements {
        equals("env.AGENT_TYPE", "docker-build")  // Changed from nodejs-build to docker-build
    }
})
