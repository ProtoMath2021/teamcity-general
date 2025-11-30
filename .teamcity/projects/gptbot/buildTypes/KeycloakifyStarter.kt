package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import projects.gptbot.vcsRoots.KeycloakifyStarterGit

object KeycloakifyStarter : BuildType({
    id("Gptbot_KeycloakifyStarter")
    name = "Keycloakify Starter Theme"

    vcs {
        root(KeycloakifyStarterGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = """
                    protonmath/keycloakify-starter:%build.counter%
                    protonmath/keycloakify-starter:latest
                """.trimIndent()
                commandArgs = "--platform linux/amd64"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/keycloakify-starter:%build.counter%"
            }
        }
        dockerCommand {
            name = "push latest"
            commandType = push {
                namesAndTags = "protonmath/keycloakify-starter:latest"
            }
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
        equals("env.AGENT_TYPE", "nodejs-build")
    }
})
