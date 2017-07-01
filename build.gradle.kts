import com.bmuschko.gradle.docker.tasks.AbstractReactiveStreamsTask
import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage

buildscript {
    repositories {
        gradleScriptKotlin()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlin_version"]}")
    }
}

plugins {
    id("org.jetbrains.kotlin.plugin.allopen") version "1.1.2"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.1.2"
    id("com.bmuschko.docker-remote-api") version "3.0.7"
}

apply {
    plugin("kotlin")
    plugin("kotlin-allopen")
    plugin("kotlin-noarg")
    plugin("kotlin-jpa")
    plugin("war")
    plugin("com.bmuschko.docker-remote-api")
}

val dockerImageIdFile: File = File("$buildDir/imageId")
val dockerContainerIdFile: File = File("$buildDir/containerId")

fun readImageId(): String = dockerImageIdFile.readText().trim()
fun readContainerId(): String = dockerContainerIdFile.readText().trim()

open class ExceptionClosure<V: Any>(
        val function: (Exception) -> V?
) : groovy.lang.Closure<V?>(null, null) {

    fun doCall(ex: Exception): V? = function(ex)
}

open class NoArgClosure<V: Any>(
        val function: () -> V?
) : groovy.lang.Closure<V?>(null, null) {

    fun doCall(): V? = function()
}

fun AbstractReactiveStreamsTask.onError2(onErrorAction: (Exception) -> Unit) {
    onError = ExceptionClosure(onErrorAction)
}

fun DockerExistingContainer.targetContainerId(containerIdSupplier: () -> String) =
        targetContainerId(NoArgClosure(containerIdSupplier))

fun DockerCreateContainer.targetImageId(imageIdSupplier : () -> String) =
        targetImageId(NoArgClosure(imageIdSupplier))

fun DockerExistingContainer.containerIdFromFile(file: File) {
    if (file.exists()) {
        targetContainerId(NoArgClosure { readContainerId() })
    } else {
        enabled = false
    }
}

fun DockerRemoveImage.imageIdFromFile(file: File) {
    if (file.exists()) {
        targetImageId(NoArgClosure { readImageId() })
        force = true
    } else {
        enabled = false
    }
}

tasks {

    val war = "war"(War::class) {
        archiveName = "playground.war"
    }

    val stopContainer = "stopContainer"(DockerStopContainer::class) {
        containerIdFromFile(dockerContainerIdFile)

        onError2 { exception: Exception ->
            println(exception.javaClass)
            println(exception.message)
        }
    }

    val removeContainer = "removeContainer"(DockerRemoveContainer::class) {
        dependsOn(stopContainer)

        containerIdFromFile(dockerContainerIdFile)

        doLast {
            dockerContainerIdFile.delete()
        }
    }

    val removeImage =  "removeImage"(DockerRemoveImage::class) {
        dependsOn(removeContainer)

        imageIdFromFile(dockerImageIdFile)

        doLast {
            dockerImageIdFile.delete()
        }
    }

    val buildImage = "buildImage"(DockerBuildImage::class) {
        dependsOn(removeImage, war)

        inputDir = projectDir
        tag = "playground:development"

        doLast {
            dockerImageIdFile.printWriter().use { out -> out.println(imageId) }
        }
    }

    val createContainer = "createContainer"(DockerCreateContainer::class) {
        dependsOn(buildImage)

        targetImageId {
            buildImage.imageId
        }

        portBindings = listOf("8080:8080")

        doLast {
            dockerContainerIdFile.printWriter().use { out -> out.println(containerId) }
        }
    }

    val startContainer = "startContainer"(DockerStartContainer::class) {
        dependsOn(createContainer)

        targetContainerId {
            createContainer.containerId
        }
    }
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.ejb.Stateless")
}

noArg {
    annotation("javax.ws.rs.Path")
    annotation("javax.ejb.Stateless")
}

dependencies {
    compile("com.fasterxml.jackson.core:jackson-databind:${extra["jackson_version"]}")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["jackson_version"]}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${extra["kotlin_version"]}") // must be "compile" for conflict resolution
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:${extra["kotlin_version"]}")

    compileOnly("javax:javaee-api:7.0")
}

repositories {
    gradleScriptKotlin()
}
