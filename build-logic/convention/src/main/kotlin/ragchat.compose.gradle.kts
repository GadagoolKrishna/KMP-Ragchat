import com.android.build.api.dsl.CommonExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.findByType(CommonExtension::class.java)?.apply {
    buildFeatures {
        compose = true
    }
}
