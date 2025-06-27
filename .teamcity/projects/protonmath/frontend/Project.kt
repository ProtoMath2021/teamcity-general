package projects.protonmath.frontend

import jetbrains.buildServer.configs.kotlin.*
import projects.protonmath.frontend.studentui.StudentUiProject
import projects.protonmath.frontend.teacherui.TeacherUiProject
import projects.protonmath.frontend.teacheruiv2.TeacherUiV2Project

object FrontendProject : Project({
    name = "Frontend"

    subProject(StudentUiProject)
    subProject(TeacherUiV2Project)
    subProject(TeacherUiProject)
})
