plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// Relocate the shaded libraries' META-INF/services entries along with their classes.
tasks.named("shadowJar") { withGroovyBuilder { "mergeServiceFiles"() } }

// ./gradlew install -PgtnhDir=<instance .minecraft>, or set gtnhDir in ~/.gradle/gradle.properties.
tasks.register<Copy>("install") {
    group = "distribution"
    description = "Builds the mod and copies it into <gtnhDir>/mods."
    from(tasks.named("reobfJar"))
    into(providers.gradleProperty("gtnhDir").map { File(it, "mods") })
    rename { "gtnh-mcp.jar" }
}
