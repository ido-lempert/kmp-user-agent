package site.lempert.kmp_user_agent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform