@file:Suppress("DEPRECATION")

package io.gitlab.arturbosch.detekt.internal

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal class DetektJvm(private val project: Project) {
    fun registerTasks(extension: DetektExtension) {
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { sourceSet ->
            project.registerJvmDetektTask(extension, sourceSet)
            project.registerJvmCreateBaselineTask(extension, sourceSet)
        }
    }

    private fun Project.registerJvmDetektTask(extension: DetektExtension, sourceSet: SourceSet) {
        val kotlinSourceSet = (sourceSet as HasConvention).convention.plugins["kotlin"] as? KotlinSourceSet
            ?: throw GradleException("Kotlin source set not found. Please report on detekt's issue tracker")
        registerDetektTask(DetektPlugin.DETEKT_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = kotlinSourceSet.kotlin
            classpath.setFrom(sourceSet.compileClasspath.existingFiles(), sourceSet.output.classesDirs.existingFiles())
            // If a baseline file is configured as input file, it must exist to be configured, otherwise the task fails.
            // We try to find the configured baseline or alternatively a specific variant matching this task.
            extension.baseline?.existingVariantOrBaseFile(sourceSet.name)?.let { baselineFile ->
                baseline.convention(layout.file(project.provider { baselineFile }))
            }
            setReportOutputConventions(reports, extension, sourceSet.name)
            description = "EXPERIMENTAL: Run detekt analysis for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun Project.registerJvmCreateBaselineTask(extension: DetektExtension, sourceSet: SourceSet) {
        val kotlinSourceSet = (sourceSet as HasConvention).convention.plugins["kotlin"] as? KotlinSourceSet
            ?: throw GradleException("Kotlin source set not found. Please report on detekt's issue tracker")
        registerCreateBaselineTask(DetektPlugin.BASELINE_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = kotlinSourceSet.kotlin
            classpath.setFrom(sourceSet.compileClasspath.existingFiles(), sourceSet.output.classesDirs.existingFiles())
            val variantBaselineFile = extension.baseline?.addVariantName(sourceSet.name)
            baseline.convention(project.layout.file(project.provider { variantBaselineFile }))
            description = "EXPERIMENTAL: Creates detekt baseline for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun FileCollection.existingFiles() = filter { it.exists() }
}
