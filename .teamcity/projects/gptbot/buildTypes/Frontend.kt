package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import projects.gptbot.vcsRoots.GptAgentUiGit

object Frontend : BuildType({
    id("Gptbot_Frontend")
    name = "Frontend"

    params {
        param("env.REACT_APP_API_URL", "https://aichatter.ru/api/")
    }

    vcs {
        root(GptAgentUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = """
                    protonmath/gpt-agent-ui:%build.counter%
                    registry.INTERNAL:5000/gpt-agent-ui:%build.counter%
                """.trimIndent()
                commandArgs = "--build-arg REACT_APP_API_URL=%env.REACT_APP_API_URL%"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/gpt-agent-ui:%build.counter%"
            }
        }
        dockerCommand {
            name = "push to private registry"
            commandType = push {
                namesAndTags = "registry.INTERNAL:5000/gpt-agent-ui:%build.counter%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_4"
            }
        }
    }

    requirements {
        equals("env.AGENT_TYPE", "nodejs-build")
    }
})
