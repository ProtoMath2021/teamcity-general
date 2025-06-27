package projects.eutrip.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.eutrip.vcsRoots.AdminUiGit

object Frontend : BuildType({
    id("Eutrip_Frontend")
    name = "Frontend"

    buildNumberPattern = "dev-%build.counter%"

    params {
        param("env.REACT_APP_API_URL", "https://eutrip.devinfra.ru/api")
    }

    vcs {
        root(AdminUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/eutrip-admin-ui:dev-%build.counter%"
                commandArgs = "--build-arg REACT_APP_API_URL=%env.REACT_APP_API_URL%"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/eutrip-admin-ui:dev-%build.counter%"
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:root=${AdminUiGit.id}:**"

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
