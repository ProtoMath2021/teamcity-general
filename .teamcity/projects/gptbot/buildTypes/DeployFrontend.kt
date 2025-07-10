package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import projects.gptbot.vcsRoots.GptAgentUiGit

object DeployFrontend : BuildType({
    id("Gptbot_DeployFrontend")
    name = "Deploy_Frontend"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(GptAgentUiGit)
    }

    steps {
        script {
            name = "deploy helm"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace gptbot \
                                    --set app.version=%DEPLOY_TAG% \
                                	frontend .
            """.trimIndent()
        }
    }

    requirements {
        equals("env.AGENT_TYPE", "helm-deploy")
    }
})
