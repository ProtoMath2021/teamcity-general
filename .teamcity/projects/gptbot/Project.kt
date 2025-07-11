package projects.gptbot

import jetbrains.buildServer.configs.kotlin.*
import projects.gptbot.vcsRoots.*
import projects.gptbot.buildTypes.*

object GptbotProject : Project({
    name = "Gptbot"

    vcsRoot(WwhatsappNodeGit)
    vcsRoot(GptAgentApiGit)
    vcsRoot(GptAgentUiGit)

    buildType(Backend)
    buildType(DeployBackend)
    buildType(DeployFrontend)
    buildType(Frontend)
    buildType(WwhatsappNode)
})
