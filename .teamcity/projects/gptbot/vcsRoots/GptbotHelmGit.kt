package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptbotHelmGit : GitVcsRoot({
    id("Gptbot_GptbotHelmGit")
    name = "https://github.com/dev4team-ai/gptbot-helm.git"
    url = "https://github.com/dev4team-ai/gptbot-helm.git"
    branch = "refs/heads/main"
    authMethod = token {
        userName = "teamcity"
        tokenId = "tc_token_id:CID_5d39b539391852e509211819e6a59e55:-1:d88865e4-c6a5-4310-82c8-96d3e7a31446"
    }
})
