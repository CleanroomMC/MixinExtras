import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublishing(artifactName: String, setup: MavenPublication.() -> Unit) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    val shouldSign = (properties["signMixinExtras"] as? String).toBoolean()

    val localStagingDir: Directory by rootProject.extra

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "Staging"
                url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
            }
            maven {
                name = "CleanroomMC"
                url = uri("https://maven.cleanroommc.com")
                credentials {
                    username = properties["cleanroomMavenUser"] as? String
                        ?: System.getenv("MAVEN_USER")
                    password = properties["cleanroomMavenPassword"] as? String
                        ?: System.getenv("MAVEN_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.cleanroommc"
                artifactId = "mixinextras-$artifactName"

                setup()

                pom {
                    name = "MixinExtras"
                    description = "Companion library to Mixin with lots of features to improve the compatibility and concision of your mixins!"
                    url = "https://github.com/CleanroomMC/MixinExtras"
                    licenses {
                        license {
                            name = "MIT License"
                            url = "http://www.opensource.org/licenses/mit-license.php"
                        }
                    }
                    developers {
                        developer {
                            name = "LlamaLad7"
                            url = "https://github.com/LlamaLad7"
                        }
                        developer {
                            name = "Rongmario"
                            url = "https://github.com/Rongmario"
                        }
                    }
                    scm {
                        connection = "scm:git:git://github.com/CleanroomMC/MixinExtras.git"
                        developerConnection = "scm:git:git://github.com/CleanroomMC/MixinExtras.git"
                        url = "https://github.com/CleanroomMC/MixinExtras/tree/master"
                    }
                    withXml {
                        val node = asNode().appendNode("dependencies")
                        configurations.getByName("api").dependencies.forEach {
                            val dep = node.appendNode("dependency")
                            dep.appendNode("groupId", it.group)
                            dep.appendNode("artifactId", it.name)
                            dep.appendNode("version", it.version)
                            dep.appendNode("scope", "runtime")
                        }
                    }
                }
            }
        }
    }
    afterEvaluate {
        tasks.withType<PublishToMavenRepository>().configureEach {
            if (repository.url == uri(localStagingDir)) {
                dependsOn(":cleanStagingRepo")
            }
        }
    }
    if (shouldSign) {
        extensions.configure<SigningExtension> {
            useGpgCmd()
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}
