package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.vcsRoots.GptAgentApiGit

object Backend : BuildType({
    id("Gptbot_Backend")
    name = "Backend"

    vcs {
        root(GptAgentApiGit)

        cleanCheckout = true
    }

    steps {
        gradle {
            name = "build"
            tasks = "bootBuildImage"
            jdkHome = "/share/jdk-21"
        }
        dockerCommand {
            name = "tag"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/library/gpt-agent-api:0.0.1-SNAPSHOT protonmath/gpt-agent-api:%build.number%"
            }
        }
        dockerCommand {
            name = "tag for private registry"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/library/gpt-agent-api:0.0.1-SNAPSHOT registry.INTERNAL:5000/gpt-agent-api:%build.number%"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/gpt-agent-api:%build.number%"
            }
        }
        dockerCommand {
            name = "publish to private registry"
            commandType = push {
                namesAndTags = "registry.INTERNAL:5000/gpt-agent-api:%build.number%"
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
        exists("env.JDK_21")
    }
})
