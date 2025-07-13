package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptbotHelmGit : GitVcsRoot({
    id("Gptbot_GptbotHelmGit")
    name = "https://github.com/dev4team-ai/gptbot-helm.git"
    url = "https://github.com/dev4team-ai/gptbot-helm.git"
    branch = "refs/heads/main"
    authMethod = password {
        userName = "teamcity"
        password = "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"
    }
})
