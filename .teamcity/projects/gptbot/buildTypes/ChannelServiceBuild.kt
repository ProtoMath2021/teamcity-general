
package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.vcsRoots.ChannelServiceGit

object ChannelServiceBuild : BuildType({
    id("GptbotProject_ChannelServiceBuild")
    name = "channel-service"

    vcs {
        root(ChannelServiceGit)
    }

    steps {
        gradle {
            name = "build"
            id = "build"
            tasks = "bootBuildImage"
        }
        dockerCommand {
            name = "tag"
            id = "tag"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/library/channel-service:0.0.1-SNAPSHOT protonmath/channel-service:%build.number%"
            }
        }
        dockerCommand {
            name = "tag for private registry"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/library/channel-service:0.0.1-SNAPSHOT registry.INTERNAL:5000/channel-service:%build.number%"
            }
        }
        dockerCommand {
            name = "publish"
            id = "publish"
            commandType = push {
                namesAndTags = "protonmath/channel-service:%build.number%"
            }
        }
        dockerCommand {
            name = "publish to private registry"
            commandType = push {
                namesAndTags = "registry.INTERNAL:5000/channel-service:%build.number%"
            }
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
        dockerRegistryConnections {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})
