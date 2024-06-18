import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.buildReportTab
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
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
    }

    cleanup {
        baseRule {
            preventDependencyCleanup = false
        }
    }

    subProject(ProtonMath)
}


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
        param("env.NPM_TOKEN", "tmp")
        password("env.GH_TOKEN", "zxxffd737ff8404eda40073b4dc3d937ed96436a6d98d80c23e951ec1af0f775d6002dd95f81af44e60775d03cbe80d301b")
        text("CURRENT_TAG_EXPERT", "", allowEmpty = true)
    }

    vcs {
        root(ProtonMath_Backend_GitGithubComProtoMath2021protomathCoreApiGit)
    }

    steps {
        nodeJS {
            name = "getVer"
            shellScript = """
                npm init -y
                npm install
                npm install @semantic-release/git @semantic-release/changelog -D
                npm update semantic-release @semantic-release/* --save-dev
                
                git config --global --add safe.directory "${'$'}(pwd)"
                echo HELP
                echo %build.number%
                echo %env.GH_TOKEN%
                echo %env.NPM_TOKEN%
                echo "`pwd`"
                echo "`ls -la`"
                echo "`ls -la .git/`"
                echo "getVer1"
                npx semantic-release --debug --no-ci
                echo "`ls -la`"
            """.trimIndent()
            dockerImage = "node:21.7.3-slim"
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
