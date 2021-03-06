package com.jianghongkui.lint

import com.android.build.gradle.internal.api.BaseVariantImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author hongkui.jiang* @Date 2019/3/12
 */
class LintPlugin implements Plugin<Project> {

    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        def lintExtension = project.extensions.create("incrementLint", LintExtension.class)
        project.extensions.create("incrementLintOptions", IncrementLintOptions.class);
        lintExtension.init(project)

        ChangeFileCheckTask.create(project)

        MLogger.setOutFile(lintExtension.logPath)
        if (project.plugins.hasPlugin("com.android.application")) {
            project.android.applicationVariants.all { variant ->
                maybeLint(variant, "App")
            }
        } else if (project.plugins.hasPlugin("com.android.library")) {
            project.android.libraryVariants.all { variant ->
                maybeLint(variant, "Lib")
            }
        } else {
            return
        }

        MLogger.flush()
    }

    private void maybeLint(BaseVariantImpl variant, String type) {
        String suffix = variant.name.capitalize()
        if (!suffix.endsWith("Debug")) {
            return
        }
        addDebugLintTask(suffix, variant, type)
    }

    private void addDebugLintTask(String suffix, BaseVariantImpl variant, String type) {
        if (type == "App") {
            setAppLintTask(suffix, variant)
        } else {
            setLibLintTask(suffix, variant)
        }
    }

    private void setAppLintTask(String suffix, BaseVariantImpl variant) {
        String lintTaskName = "AppIncrementLint${suffix}Task"
        //创建FnLintTask 任务
        IncrementLintTask lintTask = IncrementLintTask.create(variant.getVariantData().getScope(), lintTaskName)
        dependTask(lintTask, "prepareLintJar", "compile${suffix}JavaWithJavac", "process${suffix}Manifest", "getChangedFile")
        dependTask("assemble${suffix}", lintTaskName)
    }

    private void setLibLintTask(String suffix, BaseVariantImpl variant) {
        String lintTaskName = "LibIncrementLint${suffix}Task"
        //创建FnLintTask 任务
        IncrementLintTask lintTask = IncrementLintTask.create(variant.getVariantData().getScope(), lintTaskName)
        dependTask(lintTask, "prepareLintJar", "compile${suffix}JavaWithJavac", "process${suffix}Manifest", "getChangedFile")
        dependTask("bundleLibRuntime${suffix}", lintTaskName)
    }

    private void dependTask(String taskName, String... dependTaskNames) {
        def task = project.tasks[taskName]
        dependTask(task, dependTaskNames)
    }

    private void dependTask(Task task, String... dependTaskNames) {
        int length = dependTaskNames.length
        def tasks = new Object[length]
        for (int i = 0; i < length; i++) {
            tasks[i] = project.tasks[dependTaskNames[i]]
        }
        task.dependsOn(tasks)
    }
}