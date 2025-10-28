package projects.gptbot.whatsappController.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object WhatsappControllerGit : GitVcsRoot({
    id("GptbotProject_WhatsappController_HttpsGithubComDev4teamAiWhatsappControllerRefsHeadsMaster")
    name = "https://github.com/dev4team-ai/whatsapp-controller#refs/heads/master"
    url = "https://github.com/dev4team-ai/whatsapp-controller"
    branch = "refs/heads/master"
    branchSpec = "refs/heads/*"
    authMethod = token {
        userName = "oauth2"
        tokenId = "tc_token_id:CID_5d39b539391852e509211819e6a59e55:-1:45aafd0d-e9de-4ba4-b1c1-d1903f18150b"
    }
})
