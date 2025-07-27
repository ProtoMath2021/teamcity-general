package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptbotHelmGit : GitVcsRoot({
    id("GptbotProject_GptbotHelmGit")
    name = "gptbot-helm-vcs"
    url = "https://github.com/dev4team-ai/gptbot-helm"
    branch = "refs/heads/main"
    authMethod = token {
        userName = "oauth2"
        tokenId = "tc_token_id:CID_5d39b539391852e509211819e6a59e55:-1:490356fb-dfff-496a-8b86-735e36fdec21"
    }
})
