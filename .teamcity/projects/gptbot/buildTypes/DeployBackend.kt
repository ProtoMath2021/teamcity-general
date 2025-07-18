package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import projects.gptbot.vcsRoots.GptAgentApiGit

object DeployBackend : BuildType({
    id("Gptbot_Deploy")
    name = "Deploy_Backend"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(GptAgentApiGit)
    }

    steps {
        script {
            name = "deploy helm"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace gptbot \
                                    --set app.version=%DEPLOY_TAG% \
                                	backend .
            """.trimIndent()
        }
    }

    requirements {
        equals("env.AGENT_TYPE", "helm-deploy")
    }
})
