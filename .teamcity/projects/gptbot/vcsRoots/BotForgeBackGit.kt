package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object BotForgeBackGit : GitVcsRoot({
    id("Gptbot_BotForgeBackGit")
    name = "BotForge-back"
    url = "git@github.com:ProtoMath2021/BotForge.git"
    branch = "master"
    authMethod = uploadedKey {
        uploadedKey = "gptbot-back-teamcity"
    }
})
