package projects.gptbot

import jetbrains.buildServer.configs.kotlin.*
import projects.gptbot.vcsRoots.*
import projects.gptbot.buildTypes.*

object GptbotProject : Project({
    name = "Gptbot"

    vcsRoot(GptAgentApiGit)
    vcsRoot(GptAgentUiGit)
    vcsRoot(BotForgeBackGit)

    buildType(Backend)
    buildType(Deploy)
    buildType(DeployFrontend)
    buildType(Frontend)
})
