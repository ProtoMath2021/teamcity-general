package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
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
        // Docker build step - Dockerfile.optimized handles dependency installation and building
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile.optimized"  // Use the optimized multi-stage Dockerfile
                }
                namesAndTags = "protonmath/wwhatsapp-node:%build.number%"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:%build.number%"
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
