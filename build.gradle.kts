import com.bmuschko.gradle.docker.tasks.AbstractReactiveStreamsTask
import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage

buildscript {

    repositories {
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
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

open class NoArgKotlinClosure<V: Any>(
        val function: (Exception) -> V?
) : groovy.lang.Closure<V?>(null, null) {

    @Suppress("unused")
    fun doCall(ex: Exception): V? = function(ex)
}

fun AbstractReactiveStreamsTask.onError2(onErrorAction: (Exception) -> Unit) {
    onError = NoArgKotlinClosure(onErrorAction)
}

tasks {

    val war = "war"(War::class) {
        archiveName = "playground.war"
    }

    val stopContainer = "stopContainer"(DockerStopContainer::class) {
        if (dockerContainerIdFile.exists()) {
            containerId = readContainerId()
        } else {
            enabled = false
        }

        onError2 { exception: Exception ->
            println(exception.javaClass)
            println(exception.message)
        }
    }

    val removeContainer = "removeContainer"(DockerRemoveContainer::class) {
        dependsOn(stopContainer)

        if (dockerContainerIdFile.exists()) {
            containerId = readContainerId()
        } else {
            enabled = false;
        }

        doLast {
            dockerContainerIdFile.delete()
        }
    }

    val removeImage =  "removeImage"(DockerRemoveImage::class) {
        dependsOn(removeContainer)

        if (dockerImageIdFile.exists()) {
            imageId = readImageId()
            force = true
        } else {
            enabled = false
        }

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

        imageId = "empty"

        doFirst {
            imageId = buildImage.imageId
        }

        doLast {
            dockerContainerIdFile.printWriter().use { out -> out.println(containerId) }
        }
    }

    val startContainer = "startContainer"(DockerStartContainer::class) {
        dependsOn(createContainer)

        containerId = "empty"

        doFirst {
            containerId = createContainer.containerId
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
    compile(kotlinModule("stdlib"))
    compile("com.fasterxml.jackson.core:jackson-databind:${extra["jackson_version"]}")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:${extra["jackson_version"]}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${extra["kotlin_version"]}") // must be "compile" for conflict resolution
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:${extra["kotlin_version"]}")

    compileOnly("javax:javaee-api:7.0")
}

repositories {
    gradleScriptKotlin()
}
