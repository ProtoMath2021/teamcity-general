import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.buildReportTab
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {
    description = "Contains all other projects"

    features {
        buildReportTab {
            id = "PROJECT_EXT_1"
            title = "Code Coverage"
            startPage = "coverage.zip!index.html"
        }
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "Docker Registry"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
    }

    cleanup {
        baseRule {
            preventDependencyCleanup = false
        }
    }

    subProject(Eutrip)
    subProject(ProtonMath)
    subProject(Gptbot)
}


object Eutrip : Project({
    name = "Eutrip"

    vcsRoot(Eutrip_GitGithubComProtoMath2021ansibleHostGit)
    vcsRoot(Eutrip_GitGithubComProtoMath2021eutripCoreApiGit)
    vcsRoot(Eutrip_GitGithubComProtoMath2021eutripAdminUiGit)
    vcsRoot(Eutrip_GitGithubComProtoMath2021eutripHelmChartsGit)

    buildType(Eutrip_FrontendProd)
    buildType(Eutrip_DeployBackend)
    buildType(Eutrip_Frontend)
    buildType(Eutrip_Backend)
})

object Eutrip_Backend : BuildType({
    name = "Backend"

    vcs {
        root(Eutrip_GitGithubComProtoMath2021eutripCoreApiGit)
    }

    steps {
        gradle {
            name = "build"
            tasks = "bootBuildImage"
            jdkHome = "%env.JDK_21%"
        }
        dockerCommand {
            name = "tag"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/mine/java-app-run:latest protonmath/eutrip-core-api:latest"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/eutrip-core-api:latest"
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:root=${Eutrip_GitGithubComProtoMath2021eutripCoreApiGit.id}:**"

            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})

object Eutrip_DeployBackend : BuildType({
    name = "Deploy"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        param("env.PATH", "/usr/bin:/usr/sbin:/usr/local/sbin:/usr/local/bin:/home/buildagent/.local/bin")
        checkbox("prod", "false", label = "prod", display = ParameterDisplay.PROMPT,
                  checked = "true", unchecked = "false")
        text("ui_version", "", label = "ui_version", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(Eutrip_GitGithubComProtoMath2021ansibleHostGit)
    }

    steps {
        script {
            name = "requirements"
            scriptContent = "ansible-galaxy install -r requirements.yml"
        }
        script {
            name = "deploy playbook"
            scriptContent = "ansible-playbook -i ./projects/eutrip/inventory/dev/inv ./projects/eutrip/playbooks/play-app.yaml -e ansible_user=cd_tech_agent -e ansible_host=85.30.208.151 -e ansible_port=2207 -e ui_version=dev-%ui_version% -vvv"
        }
        script {
            name = "deploy playbook prod"

            conditions {
                equals("prod", "true")
            }
            scriptContent = "ansible-playbook -i ./projects/eutrip/inventory/prod/inv ./projects/eutrip/playbooks/play-app.yaml -e ansible_user=infra -e ansible_host=103.137.248.112 -e ansible_port=22 -e ui_version=prod-%ui_version% -vvv"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Eutrip_Backend.id}"
            successfulOnly = true
        }
    }

    requirements {
        equals("teamcity.agent.name", "Agent 2-1")
    }
})

object Eutrip_Frontend : BuildType({
    name = "Frontend"

    buildNumberPattern = "dev-%build.counter%"

    params {
        param("env.REACT_APP_API_URL", "https://eutrip.devinfra.ru/api")
    }

    vcs {
        root(Eutrip_GitGithubComProtoMath2021eutripAdminUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/eutrip-admin-ui:dev-%build.counter%"
                commandArgs = "--build-arg REACT_APP_API_URL=%env.REACT_APP_API_URL%"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/eutrip-admin-ui:dev-%build.counter%"
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:root=${Eutrip_GitGithubComProtoMath2021eutripAdminUiGit.id}:**"

            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})

object Eutrip_FrontendProd : BuildType({
    name = "Frontend_prod"

    buildNumberPattern = "prod-%build.counter%"

    params {
        param("env.REACT_APP_API_URL", "https://portal.europevoyage.it/api")
    }

    vcs {
        root(Eutrip_GitGithubComProtoMath2021eutripAdminUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/eutrip-admin-ui:prod-%build.counter%"
                commandArgs = "--build-arg REACT_APP_API_URL=%env.REACT_APP_API_URL%"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/eutrip-admin-ui:prod-%build.counter%"
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:root=${Eutrip_GitGithubComProtoMath2021eutripAdminUiGit.id}:**"

            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})

object Eutrip_GitGithubComProtoMath2021ansibleHostGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/ansible-host.git"
    url = "git@github.com:ProtoMath2021/ansible-host.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip_cd"
    }
})

object Eutrip_GitGithubComProtoMath2021eutripAdminUiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/eutrip-admin-ui.git"
    url = "git@github.com:ProtoMath2021/eutrip-admin-ui.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-front-teamcity"
    }
})

object Eutrip_GitGithubComProtoMath2021eutripCoreApiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/eutrip-core-api.git"
    url = "git@github.com:ProtoMath2021/eutrip-core-api.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-back-teamcity"
    }
})

object Eutrip_GitGithubComProtoMath2021eutripHelmChartsGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/eutrip-helm-charts.git"
    url = "git@github.com:ProtoMath2021/eutrip-helm-charts.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-helm-teamcity"
    }
})


object Gptbot : Project({
    name = "Gptbot"

    vcsRoot(Gptbot_GitGithubComDev4teamAiGptAgentApiGit)
    vcsRoot(Gptbot_GitGithubComDev4teamAiGptAgentUiGit)
    vcsRoot(Gptbot_BotForgeBack)

    buildType(Gptbot_Backend)
    buildType(Gptbot_Deploy)
    buildType(Gptbot_DeployFrontend)
    buildType(Gptbot_Frontend)
})

object Gptbot_Backend : BuildType({
    name = "Backend"

    vcs {
        root(Gptbot_GitGithubComDev4teamAiGptAgentApiGit)

        checkoutMode = CheckoutMode.ON_SERVER
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "build"
            tasks = "bootBuildImage"
            jdkHome = "/share/jdk-21"
        }
        dockerCommand {
            name = "tag"
            commandType = other {
                subCommand = "tag"
                commandArgs = "docker.io/library/api-gateway:latest protonmath/gpt-agent-api:%build.number%"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/gpt-agent-api:%build.number%"
            }
        }
    }

    triggers {
        vcs {
            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }

    requirements {
        exists("env.JDK_21")
    }
})

object Gptbot_Deploy : BuildType({
    name = "Deploy_Backend"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(Gptbot_GitGithubComDev4teamAiGptAgentApiGit)
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

    triggers {
        finishBuildTrigger {
            buildType = "${Gptbot_Backend.id}"
            successfulOnly = true

            buildParams {
                text("DEPLOY_TAG", "%build.number%", display = ParameterDisplay.PROMPT, allowEmpty = true)
            }
        }
    }
})

object Gptbot_DeployFrontend : BuildType({
    name = "Deploy_Frontend"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(Gptbot_GitGithubComDev4teamAiGptAgentUiGit)
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
})

object Gptbot_Frontend : BuildType({
    name = "Frontend"

    params {
        param("env.REACT_APP_API_URL", "https://aichatter.ru/api/")
    }

    vcs {
        root(Gptbot_GitGithubComDev4teamAiGptAgentUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/gpt-agent-ui:%build.counter%"
                commandArgs = "--build-arg REACT_APP_API_URL=%env.REACT_APP_API_URL%"
            }
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "protonmath/gpt-agent-ui:%build.counter%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})

object Gptbot_BotForgeBack : GitVcsRoot({
    name = "BotForge-back"
    url = "git@github.com:ProtoMath2021/BotForge.git"
    branch = "master"
    authMethod = uploadedKey {
        uploadedKey = "gptbot-back-teamcity"
    }
})

object Gptbot_GitGithubComDev4teamAiGptAgentApiGit : GitVcsRoot({
    name = "git@github.com:dev4team-ai/gpt-agent-api.git"
    url = "git@github.com:dev4team-ai/gpt-agent-api.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "gpt-agent-api"
    }
})

object Gptbot_GitGithubComDev4teamAiGptAgentUiGit : GitVcsRoot({
    name = "git@github.com:dev4team-ai/gpt-agent-ui.git"
    url = "git@github.com:dev4team-ai/gpt-agent-ui.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "gpt-agent-ui"
    }
})


object ProtonMath : Project({
    name = "ProtonMath"

    features {
        dockerRegistry {
            id = "PROJECT_EXT_11"
            name = "Docker Registry"
            userName = "protonmath"
            password = "zxx5e244335597ac1e30345c418695c01fe083c51d26ee56b10396789d1aa194f59a07c29f646e3eb09"
        }
    }

    subProject(ProtonMath_Frontend)
    subProject(ProtonMath_Backend)
})


object ProtonMath_Backend : Project({
    name = "Backend"

    vcsRoot(ProtonMath_Backend_GitGithubComProtoMath2021protomathCoreApiGit)

    buildType(ProtonMath_Backend_Build)
})

object ProtonMath_Backend_Build : BuildType({
    name = "build"

    params {
        param("env.NPM_TOKEN", "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")
        password("env.GH_TOKEN", "zxx8b917490812b4af1dee4d6c6a628840b20fec2d360dfb6bc18284b59651a4c2a47f1c6a8d404c9a4775d03cbe80d301b")
        text("CURRENT_TAG_EXPERT", "", allowEmpty = true)
    }

    vcs {
        root(ProtonMath_Backend_GitGithubComProtoMath2021protomathCoreApiGit)
    }

    steps {
        script {
            name = "hosts"
            scriptContent = """echo "`mkdir -p ~/.ssh && chmod 700 ~/.ssh && ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts`""""
        }
        nodeJS {
            name = "getVer"
            shellScript = """
                npm install
                npm install @semantic-release/git @semantic-release/changelog -D
                npm update semantic-release @semantic-release/* --save-dev
                
                echo HELP1
                
                git config --global --add safe.directory "${'$'}(pwd)"
                
                echo HELP 
                echo %build.number%
                echo %env.GH_TOKEN%
                echo "`pwd`"
                echo "`ls -la`"
                echo "`ls -la .git/`"
                echo "getVer1" 
                
                
                
                npx semantic-release --debug --no-ci
                
                echo "`ls -la`"
            """.trimIndent()
            dockerImage = "node"
            dockerPull = true
        }
        script {
            name = "setVer"
            scriptContent = """
                echo "`ls -la`"
                git fetch --tags
                latest_tag=${'$'}(git tag --sort=version:refname | grep -v '^backend' | tail -n 1)
                echo "${'$'}latest_tag"
                echo "setVer  HELP1"
                # Set a build parameter using a service message
                echo "##teamcity[setParameter name='CURRENT_TAG_EXPERT' value='${'$'}latest_tag']"
                echo "setVer HELP2"
                
                find ./out -type f -name "*.jar" -exec rm {} \;
            """.trimIndent()
        }
        gradle {
            name = "build"
            tasks = "clean assemble"
            jdkHome = "%env.JDK_17_0%"
        }
        script {
            name = "renameJar"
            scriptContent = """
                echo "`ls -la ./out/`"
                echo "HELP renameJar"
                echo "%CURRENT_TAG_EXPERT%"
                mv ./out/*.jar ./out/amogus-%CURRENT_TAG_EXPERT%.jar
                echo "`ls -la ./out/`"
                echo "is renamed?"
                find ./out -type f ! -name "*.jar" -exec rm {} \;
                
                echo "`ls -la ./out/`"
            """.trimIndent()
        }
    }

    features {
        vcsLabeling {
            vcsRootId = "${ProtonMath_Backend_GitGithubComProtoMath2021protomathCoreApiGit.id}"
            labelingPattern = "%env.TEAMCITY_PROJECT_NAME%-%build.vcs.number%"
            branchFilter = ""
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
    }
})

object ProtonMath_Backend_GitGithubComProtoMath2021protomathCoreApiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/protomath-core-api.git"
    url = "git@github.com:ProtoMath2021/protomath-core-api.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-back-teamcity"
    }
})


object ProtonMath_Frontend : Project({
    name = "Frontend"

    subProject(ProtonMath_Frontend_StudentUi)
    subProject(ProtonMath_Frontend_TeacherUiV2)
    subProject(ProtonMath_Frontend_TeacherUi)
})


object ProtonMath_Frontend_StudentUi : Project({
    name = "student-ui"

    vcsRoot(ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit)

    buildType(ProtonMath_Frontend_StudentUi_Build)
})

object ProtonMath_Frontend_StudentUi_Build : BuildType({
    name = "build"

    params {
        param("env.KEYCLOAK_ISSUER", "https://auth.devinfra.ru/realms/protonmath")
        param("env.KEYCLOAK_REFRESH_TOKEN_URL", "https://auth.devinfra.ru/realms/protonmath/protocol/openid-connect/token")
        password("env.KEYCLOAK_CLIENT_SECRET", "zxxb1ef4e8195fe8dfbe37e9d5f92fdc63da6b2025e57cb2acec22934181b107177775d03cbe80d301b")
        param("env.NEXTAUTH_URL", "https://student.testinfra.online")
        password("env.NEXTAUTH_SECRET", "zxx90fd640ddc84feb3bae26bf487de1654")
        password("env.CRYPT_KEY", "zxx90fd640ddc84feb3bae26bf487de1654")
        param("env.END_SESSION_URL", "https://auth.devinfra.ru/realms/protonmath/protocol/openid-connect/logout")
        param("env.KEYCLOAK_CLIENT_ID", "student.protonmath.ru")
        param("BASE_URL", "TODO")
        param("AUTH_BASE_URL", "TODO")
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/proton-student-ui:%DEPLOY_TAG%"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/proton-student-ui:%DEPLOY_TAG%"
            }
        }
        script {
            name = "deploy"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace protonmath \
                                    --set app.version=%DEPLOY_TAG% \
                                    --set app.baseUrl=%BASE_URL% \
                                    --set app.authBaseUrl=%AUTH_BASE_URL% \
                                    --set app.keycloakClientId=%env.KEYCLOAK_CLIENT_ID% \
                                    --set app.keycloakClientSecret=%env.KEYCLOAK_CLIENT_SECRET% \
                                    --set app.keycloakIssuer=%env.KEYCLOAK_ISSUER% \
                                    --set app.keycloakRefreshTokenUrl=%env.KEYCLOAK_REFRESH_TOKEN_URL% \
                                    --set app.nextAuthUrl=%env.NEXTAUTH_URL% \
                                    --set app.endSessionUrl=%env.END_SESSION_URL% \
                                    --set app.nextAuthSecret=%env.NEXTAUTH_SECRET% \
                                    --set app.cryptKey=%env.CRYPT_KEY% \
                                	student-frontend .
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            triggerRules = """
                +:root=${ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit.id}:**
                +:refs/head/master
            """.trimIndent()

            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
    }
})

object ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    url = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-student-ui"
    }
    param("secure:password", "")
})


object ProtonMath_Frontend_TeacherUi : Project({
    name = "teacher-ui"

    vcsRoot(ProtonMath_Frontend_TeacherUi_GitGithubComProtoMath2021protomathUiNewGit)

    buildType(ProtonMath_Frontend_TeacherUi_BuildDocker)
    buildType(ProtonMath_Frontend_TeacherUi_Deploy)
})

object ProtonMath_Frontend_TeacherUi_BuildDocker : BuildType({
    name = "build_docker"

    params {
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(ProtonMath_Frontend_TeacherUi_GitGithubComProtoMath2021protomathUiNewGit)
    }

    steps {
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/proton-ui:%DEPLOY_TAG%"
            }
        }
        dockerCommand {
            name = "push image"
            commandType = push {
                namesAndTags = "protonmath/proton-ui:%DEPLOY_TAG%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
        vcsLabeling {
            vcsRootId = "${ProtonMath_Frontend_TeacherUi_GitGithubComProtoMath2021protomathUiNewGit.id}"
            labelingPattern = "%DEPLOY_TAG%"
            successfulOnly = true
            branchFilter = ""
        }
    }
})

object ProtonMath_Frontend_TeacherUi_Deploy : BuildType({
    name = "deploy"

    params {
        param("BASE_URL", "https://testinfra.online/api")
        param("AUTH_BASE_URL", "https://auth.devinfra.ru")
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(ProtonMath_Frontend_TeacherUi_GitGithubComProtoMath2021protomathUiNewGit)
    }

    steps {
        script {
            name = "deply helm"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace protonmath \
                                    --set app.version=%DEPLOY_TAG% \
                                    --set app.baseUrl=%BASE_URL% \
                                    --set app.authBaseUrl=%AUTH_BASE_URL% \
                                	frontend .
            """.trimIndent()
        }
    }
})

object ProtonMath_Frontend_TeacherUi_GitGithubComProtoMath2021protomathUiNewGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    url = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-teamcity"
    }
})


object ProtonMath_Frontend_TeacherUiV2 : Project({
    name = "teacher-ui-v2"

    vcsRoot(ProtonMath_Frontend_TeacherUiV2_GitGithubComProtoMath2021protomathStudentUiGit)

    buildType(ProtonMath_Frontend_TeacherUiV2_Build)
})

object ProtonMath_Frontend_TeacherUiV2_Build : BuildType({
    name = "build"

    params {
        param("env.KEYCLOAK_ISSUER", "https://auth.devinfra.ru/realms/protonmath")
        param("env.KEYCLOAK_REFRESH_TOKEN_URL", "https://auth.devinfra.ru/realms/protonmath/protocol/openid-connect/token")
        password("env.KEYCLOAK_CLIENT_SECRET", "zxxb1ef4e8195fe8dfbe37e9d5f92fdc63da6b2025e57cb2acec22934181b107177775d03cbe80d301b")
        param("env.NEXTAUTH_URL", "https://teacher.testinfra.online")
        password("env.NEXTAUTH_SECRET", "zxx90fd640ddc84feb3bae26bf487de1654")
        password("env.CRYPT_KEY", "zxx90fd640ddc84feb3bae26bf487de1654")
        param("env.END_SESSION_URL", "https://auth.devinfra.ru/realms/protonmath/protocol/openid-connect/logout")
        param("env.KEYCLOAK_CLIENT_ID", "teacher.protonmath.ru")
        param("BASE_URL", "TODO")
        param("AUTH_BASE_URL", "TODO")
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(ProtonMath_Frontend_TeacherUiV2_GitGithubComProtoMath2021protomathStudentUiGit)
    }

    steps {
        dockerCommand {
            name = "build"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/proton-teacher-ui:%DEPLOY_TAG%"
            }
        }
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/proton-teacher-ui:%DEPLOY_TAG%"
            }
        }
        script {
            name = "deploy"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace protonmath \
                                    --set app.version=%DEPLOY_TAG% \
                                    --set app.baseUrl=%BASE_URL% \
                                    --set app.authBaseUrl=%AUTH_BASE_URL% \
                                    --set app.keycloakClientId=%env.KEYCLOAK_CLIENT_ID% \
                                    --set app.keycloakClientSecret=%env.KEYCLOAK_CLIENT_SECRET% \
                                    --set app.keycloakIssuer=%env.KEYCLOAK_ISSUER% \
                                    --set app.keycloakRefreshTokenUrl=%env.KEYCLOAK_REFRESH_TOKEN_URL% \
                                    --set app.nextAuthUrl=%env.NEXTAUTH_URL% \
                                    --set app.endSessionUrl=%env.END_SESSION_URL% \
                                    --set app.nextAuthSecret=%env.NEXTAUTH_SECRET% \
                                    --set app.cryptKey=%env.CRYPT_KEY% \
                                	teacher-ui .
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            triggerRules = """
                +:root=${ProtonMath_Frontend_TeacherUiV2_GitGithubComProtoMath2021protomathStudentUiGit.id}:**
                +:refs/head/master
            """.trimIndent()

            branchFilter = ""
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
    }
})

object ProtonMath_Frontend_TeacherUiV2_GitGithubComProtoMath2021protomathStudentUiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/protomath-teacher-ui.git"
    url = "git@github.com:ProtoMath2021/protomath-teacher-ui.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-teacher-ui"
    }
    param("secure:password", "")
})
