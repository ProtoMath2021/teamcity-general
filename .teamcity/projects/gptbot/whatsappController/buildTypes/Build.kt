package projects.gptbot.whatsappController.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.whatsappController.vcsRoots.WhatsappControllerGit

object Build : BuildType({
    id("GptbotProject_WhatsappController_Build")
    name = "Build"

    vcs {
        root(WhatsappControllerGit)
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})
