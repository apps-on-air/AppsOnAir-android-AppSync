## 1.1.0

* Standardized the update alert response across platforms.

**Breaking Changes:**
* There are breaking changes in the sync method. Moving forward, the update response will be shared based on the respective Android and iOS platforms. Detailed changes to the properties are outlined below.

    * Replaced `androidBuildNumber` with `buildNumber`
    * Replaced `isAndroidUpdate` with `isUpdateEnabled`
    * Replaced `isAndroidForcedUpdate` with `isForcedUpdate`
    * Replaced `androidMinBuildVersion` with `minBuildVersion`
    * Replaced `androidUpdateLink` with `updateLink`

## 1.0.4

* Added manually update alert callback

## 1.0.3

* Downgraded dependency to compatible with kotlin 1.7.10

## 1.0.2

* Code Enhancement.

## 1.0.1

* Appsonair services for force update and maintenance for android mobile apps.

## 1.0.0

* Initial Release.
