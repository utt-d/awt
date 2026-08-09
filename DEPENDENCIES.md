# Dependency inventory

Direct production dependencies are intentionally limited to the Android and Kotlin toolchains.

| Component | Declared version | Purpose | License |
|---|---:|---|---|
| Android Gradle Plugin | 9.3.0 | Android build pipeline | Apache-2.0 |
| Kotlin / Compose compiler plugin | 2.3.21 | Language and Compose compilation | Apache-2.0 |
| Jetpack Compose BOM | 2026.06.00 | Compatible UI dependency versions | Apache-2.0 |
| AndroidX Core KTX | 1.17.0 | Android Kotlin helpers | Apache-2.0 |
| AndroidX Activity Compose | 1.12.3 | Compose activity integration | Apache-2.0 |
| Compose UI, Foundation, Material 3, Icons | BOM-managed | User interface | Apache-2.0 |
| JUnit 4 | 4.13.2 | Unit tests only | EPL-1.0 |

The resolved dependency graph can be inspected with:

```powershell
.\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath
```

Transitive versions are selected by Gradle and the Compose BOM. Review the resolved graph and generated release artifacts before distribution, particularly after dependency upgrades.

