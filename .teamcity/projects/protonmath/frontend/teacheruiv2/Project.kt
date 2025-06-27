package projects.protonmath.frontend.teacheruiv2

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object TeacherUiV2Project : Project({
    name = "teacher-ui-v2"

    vcsRoot(TeacherUiV2Git)

    buildType(Build)
})

object TeacherUiV2Git : GitVcsRoot({
    id("ProtonMath_Frontend_TeacherUiV2_Git")
    name = "git@github.com:ProtoMath2021/protomath-teacher-ui.git"
    url = "git@github.com:ProtoMath2021/protomath-teacher-ui.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-teacher-ui"
    }
    param("secure:password", "")
})

object Build : BuildType({
    id("ProtonMath_Frontend_TeacherUiV2_Build")
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
        root(TeacherUiV2Git)
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
                +:root=${TeacherUiV2Git.id}:**
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
