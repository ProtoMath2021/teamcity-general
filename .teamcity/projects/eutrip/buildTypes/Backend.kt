package projects.eutrip.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.eutrip.vcsRoots.CoreApiGit

object Backend : BuildType({
    id("Eutrip_Backend")
    name = "Backend"

    vcs {
        root(CoreApiGit)
    }

    steps {
        gradle {
            name = "build"
            tasks = "bootBuildImage"
            jdkHome = "%env.JDK_21%"
        }
        dockerCommand {
            name = "tag"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/mine/java-app-run:latest protonmath/eutrip-core-api:latest"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/eutrip-core-api:latest"
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:root=${CoreApiGit.id}:**"

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
})
