package patches.projects

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.projectFeatures.buildReportTab
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a project with id = 'TeamcityGeneral'
in the root project, and delete the patch script.
*/
create(DslContext.projectId, Project({
    id("TeamcityGeneral")
    name = "Teamcity General"
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
}))

