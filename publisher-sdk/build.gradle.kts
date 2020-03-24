plugins {
    id("com.android.library")
    `maven-publish`
    kotlin("android")
}

val support_version = "28.0.0"
val kotlin_version = "1.3.61"
val admob_version = "15.0.1"

androidLibModule() {
    addStringBuildConfigField("cdbUrl")
    addStringBuildConfigField("remoteConfigUrl")
    addStringBuildConfigField("eventUrl")
    addStringBuildConfigField("pubSdkSharedPreferences")
    addStringBuildConfigField("csmDirectory")
    addBooleanBuildConfigField("debugLogging")
}

addAzureRepository()

addPublication("release") {
    from(components["release"])
    groupId = "com.criteo.publisher"
    artifactId = "criteo-publisher-sdk"
}

dependencies {
    implementation("com.android.support:support-core-utils:$support_version")

    compileOnly("com.google.android.gms:play-services-ads:$admob_version") {
        exclude(group = "com.android.support")
    }

    implementation("com.google.auto.value:auto-value-annotations:1.6.6")
    annotationProcessor("com.google.auto.value:auto-value:1.6.6")

    implementation("com.ryanharter.auto.value:auto-value-gson-runtime:1.3.0")
    annotationProcessor("com.ryanharter.auto.value:auto-value-gson-extension:1.3.0")

    // Optional @GsonTypeAdapterFactory support
    annotationProcessor("com.ryanharter.auto.value:auto-value-gson-factory:1.3.0")

    testImplementation("junit:junit:4.12")
    testImplementation("org.json:json:20140107")
    testImplementation("org.mockito:mockito-core:2.7.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.1.10")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.mock-server:mockserver-netty:5.8.1")
    testImplementation("org.mock-server:mockserver-client-java:5.8.1")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test:rules:1.0.2")
    androidTestImplementation("org.mockito:mockito-android:3.3.0")
    androidTestImplementation("com.google.android.gms:play-services-ads:$admob_version") {
        exclude(group = "com.android.support")
    }

    androidTestImplementation("com.mopub:mopub-sdk-banner:5.6.0@aar") {
        isTransitive = true
        exclude(group = "com.android.support")
    }

    // Debug is needed because MoPub need some activities to be declare in the AndroidManifest.xml
    debugImplementation("com.mopub:mopub-sdk-interstitial:5.6.0@aar") {
        isTransitive = true
        exclude(group = "com.android.support")
    }
}
