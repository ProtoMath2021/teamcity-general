package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KeycloakifyStarterGit : GitVcsRoot({
    id("Gptbot_KeycloakifyStarterGit")
    name = "git@github.com:dev4team-ai/keycloakify-starter.git"
    url = "git@github.com:dev4team-ai/keycloakify-starter.git"
    branch = "refs/heads/master"
    branchSpec = "*"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "gpt-agent-ui"
    }
})
