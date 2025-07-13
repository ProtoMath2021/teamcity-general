package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptbotHelmGit : GitVcsRoot({
    id("Gptbot_GptbotHelmGit")
    name = "https://github.com/dev4team-ai/gptbot-helm.git"
    url = "https://github.com/dev4team-ai/gptbot-helm.git"
    branch = "refs/heads/main"
    authMethod = password {
        userName = "teamcity"
        password = "%vault:gptbot/github!pat%"
    }
})
