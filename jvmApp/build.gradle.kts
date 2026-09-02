plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

application {
    mainClass.set("site.lempert.kmp_user_agent.MainKt")
}

dependencies {
    implementation(project(":library"))
}
