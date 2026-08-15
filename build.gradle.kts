plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.4.1"

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
                modRuntimeOnly("curse.maven:embeddium-908741:5681725")
                modRuntimeOnly("curse.maven:oculus-581495:6020952")
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
                runtimeOnly("curse.maven:irisshaders-455508:6213632")
                compileOnly("curse.maven:sable-1312371:8007005")
                runtimeOnly("curse.maven:sable-1312371:8007005")
                compileOnly("dev.ryanhcode.sable-companion:sable-companion-common-1.21.1:1.6.0")
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
                compileOnly("curse.maven:irisshaders-455508:8571919")
            }
        }

        fabric {
            loaderVersion = "0.19.3"
            fabricApi("0.152.1+26.1.2")

            dependencies {
                modImplementation("maven.modrinth:forge-config-api-port:jUe0ucoE")
                modImplementation("curse.maven:baguettelib-1264423:8010960")
                modImplementation("maven.modrinth:modmenu:p7gjPPpV")
                modRuntimeOnly("curse.maven:sodium-394468:8396480")
                modRuntimeOnly("curse.maven:irisshaders-455508:8571919")
            }
        }

        neoforge {
            loaderVersion = "26.1.2.54-beta"

            dependencies {
                implementation("com.leclowndu93150.baguettelib:baguettelib-26.1.2-neoforge:2.0.4")
                modRuntimeOnly("curse.maven:sodium-394468:8396481")
                modRuntimeOnly("curse.maven:irisshaders-455508:8571918")
            }
        }
    }

    version("26.2") {
        publishingDependencies {
            requires("baguettelib")
            optional("forge-config-api-port")
            optional("modmenu")
        }

        common {
            dependencies {
                compileOnly("maven.modrinth:forge-config-api-port:86ROVP2H")
                compileOnly("maven.modrinth:baguettelib:2eXYouke")
                compileOnly("curse.maven:irisshaders-455508:8396841")
            }
        }

        fabric {
            loaderVersion = "0.19.3"
            fabricApi("0.152.2+26.2")

            dependencies {
                modImplementation("maven.modrinth:forge-config-api-port:86ROVP2H")
                modImplementation("maven.modrinth:baguettelib:2eXYouke")
                modImplementation("maven.modrinth:modmenu:TLnEHUyx")
                modRuntimeOnly("curse.maven:sodium-394468:8396428")
                modRuntimeOnly("curse.maven:irisshaders-455508:8396841")
            }
        }

        neoforge {
            loaderVersion = "26.2.0.3-beta"

            dependencies {
                modImplementation("maven.modrinth:baguettelib:h4oVSDVz")
                modRuntimeOnly("curse.maven:sodium-394468:8396429")
                modRuntimeOnly("curse.maven:irisshaders-455508:8396844")
            }
        }
    }

    publishing {
        changelog = """
        Shader support improvements:
        - Fixed wakes clipping through the water surface when using shaders that animate water. Wakes are now lifted by the exact wave height of the loaded shader pack, read from the pack itself along with your in-game shader settings, instead of a one-size-fits-all guess
        - Supported out of the box: Complementary (Reimagined, Unbound, Spooklementary, Voxlementary), Rethinking Voxels, BSL and BSL Classic, AstraLex, Insanity, Pastel, Photon, Hysteria, Sildur's Vibrant Shaders and Kappa. Packs that do not animate water are detected too and get no offset at all
        - Added "Extra shader water offset" for the rare pack whose water animation cannot be detected automatically
        - Fixed wake lighting under shaders. Wakes were being dimmed by a flat 50% that had nothing to do with actual light levels, which fought against the lighting the shader pack was already applying. Wakes are now lit entirely by the shader pack, so they respond properly to time of day, shadows and nearby light sources

        Fixes:
        - Fixed dedicated servers failing to start when Wakes was left in the server's mods folder.
        - Fixed wake trails breaking up into hard square blocks when a lot of wakes were made at once. Ripples now spread smoothly across block borders instead of stopping at them, and wakes fade out gradually instead of whole blocks popping out of existence
        - Wakes no longer flicker between neighbouring blocks of different brightness as they fade

        Additions:
        - Paddling with a shovel while standing on a floating structure now makes a wake (needs Sable, 1.21.1 only)
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
