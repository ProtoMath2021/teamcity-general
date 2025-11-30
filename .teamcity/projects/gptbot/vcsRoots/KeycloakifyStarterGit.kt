package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KeycloakifyStarterGit : GitVcsRoot({
    id("Gptbot_KeycloakifyStarterGit")
    name = "https://github.com/dev4team-ai/keycloakify-starter#refs/heads/master"
    url = "https://github.com/dev4team-ai/keycloakify-starter"
    branch = "refs/heads/master"
    branchSpec = "refs/heads/*"
    authMethod = token {
        userName = "oauth2"
        tokenId = "tc_token_id:CID_5d39b539391852e509211819e6a59e55:-1:5a458da9-ef72-4eba-bc5f-44a60d22969b"
    }
})
