plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.3.6"

prism {
    curseMaven()
    modrinthMaven()
    maven("Prism", "https://maven.leclowndu93150.dev/releases")
    maven("Valkyrien Skies", "https://maven.valkyrienskies.org")
    maven("Sable Companion", "https://maven.ryanhcode.dev/releases")

    metadata {
        modId = "wakes"
        name = "Wakes"
        description = "Wakes aims to add simple wakes that fit the spirit of vanilla"
        license = "MIT"
        author("Leclowndu93150")
    }

    version("1.20.1") {
        parchmentMinecraftVersion = "1.20.1"
        parchmentMappingsVersion = "2023.09.03"

        publishingDependencies {
            optional("valkyrien-skies")
            optional("alekiships")
        }

        forge {
            loaderVersion = "47.3.0"

            dependencies {
                modCompileOnly("curse.maven:oculus-581495:6020952")
                modCompileOnly("curse.maven:embeddium-908741:5681725")
                modCompileOnly("curse.maven:valkyrien-skies-258371:7377431")
                modImplementation("curse.maven:kotlin-for-forge-351264:5402061")
                modImplementation("curse.maven:alekiships-1068445:5963449")
                compileOnly("org.joml:joml-primitives:1.10.0")
            }

            rawProject(Action {
                dependencies {
                    add("compileOnly", "org.valkyrienskies:valkyrienskies-120-forge:2.4.0") {
                        exclude(group = "com.simibubi")
                        exclude(group = "dev.engine-room")
                        exclude(group = "com.jozufozu")
                    }
                    add("compileOnly", "org.valkyrienskies.core:api:1.1.0+e26d9059c0") {
                        exclude(group = "org.joml")
                    }
                    add("compileOnly", "org.valkyrienskies.core:api-game:1.1.0+e26d9059c0") {
                        exclude(group = "org.joml")
                    }
                    add("compileOnly", "org.valkyrienskies.core:util:1.1.0+e26d9059c0") {
                        exclude(group = "org.joml")
                    }
                }
            })
        }
    }

    version("1.21.1") {
        parchmentMinecraftVersion = "1.21.4"
        parchmentMappingsVersion = "2025.02.16"


        publishingDependencies {
            optional("sable")
            optional("create-aeronautics")
            optional("create")
        }

        neoforge {
            loaderVersion = "21.1.230"

            dependencies {
                compileOnly("curse.maven:irisshaders-455508:6213632")
                implementation("curse.maven:sodium-394468:6211307")
                compileOnly("curse.maven:sable-1312371:8007005")
                runtimeOnly("curse.maven:sable-1312371:8007005")
                compileOnly("dev.ryanhcode.sable-companion:sable-companion-common-1.21.1:1.6.0")
                compileOnly("maven.modrinth:create-aeronautics:1.2.1+mc1.21.1")
                runtimeOnly("maven.modrinth:create-aeronautics:1.2.1+mc1.21.1")
                compileOnly("curse.maven:create-328085:7963363")
                runtimeOnly("curse.maven:create-328085:7963363")
            }
        }
    }

    version("26.1.2") {
        publishingDependencies {
            requires("baguettelib")
            optional("forge-config-api-port")
            optional("modmenu")
        }

        common {
            dependencies {
                compileOnly("maven.modrinth:forge-config-api-port:jUe0ucoE")
                compileOnly("curse.maven:baguettelib-1264423:8010960")
            }
        }

        fabric {
            loaderVersion = "0.19.3"
            fabricApi("0.152.1+26.1.2")

            dependencies {
                modImplementation("maven.modrinth:forge-config-api-port:jUe0ucoE")
                modImplementation("curse.maven:baguettelib-1264423:8010960")
                modImplementation("maven.modrinth:modmenu:p7gjPPpV")
            }
        }

        neoforge {
            loaderVersion = "26.1.2.54-beta"

            dependencies {
                implementation("com.leclowndu93150.baguettelib:baguettelib-26.1.2-neoforge:2.0.4")
            }
        }
    }

    publishing {
        changelog = """

        """.trimIndent()

        type = STABLE

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1223529"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "E0SdeAoH"
        }
    }
}
