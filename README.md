# Android LiveEvent

[![Platform][platform_android_icon]][platform_android_link]
[![API][min_api_icon]][min_api_link]
[![Download][version_icon]][version_link]
[![License][license_icon]][license_link]

This is an event distribution library on Android, with the following characteristics:
* The source code is copied from LiveData 2.2.0 version, and the related code is modified to support subscription without receiving old messages, so there is no reflection
* Inherited from the advantages of LiveData, the life cycle of Activity or Fragment can be sensed through Lifecycle, and messages received when the page is inactive can be delayed to be sent when the page is active, and the monitoring can be actively cancelled when the page is destroyed

## Getting Started

This library has been published to `mavenCentral`. Add the following dependencies to your module `build.gradle` file: 

```grovvy
implementation "io.github.panpf.liveevent:liveevent:${LAST_VERSION}"
```

`${LAST_VERSION}`: [![Download][version_icon]][version_link] (No include 'v')

Dependencies：
* androidx.lifecycle:lifecycle-common: 2.2.0
* androidx.arch.core:core-common: 2.1.0
* androidx.arch.core:core-runtime: 2.1.0

## Use Guide

### 1. Define event

```kotlin
object EventService {
    val sampleLiveEvent = LiveEvent<Int>()
}
```

### 2. Listen for events

Monitor events through the `LiveEvent.listen(LifecycleOwner, Listener)` method, as follows:

```kotlin
EventService.sampleLiveEvent.listen(viewLifecycleOwner, Listener {
    //... Do things here
})
```

#### Sticky mode

If you want to receive previously sent messages when subscribing, you can use the `listenSticky(LifecycleOwner, Listener)`` method, as follows:

```kotlin
EventService.sampleLiveEvent.listenSticky(viewLifecycleOwner, Listener {
   //... Do things here
})
```

#### Don't care about page lifecycle

You can use the `listenForever(Listener)`` method when you don't need to care about the page life cycle, as follows:

```kotlin
EventService.sampleLiveEvent.listenForever(Listener {
   //... Do things here
})
```

This method also has Sticky mode, as follows:

```kotlin
EventService.sampleLiveEvent.listenForeverSticky(Listener {
  //... Do things here
})
```

### 3. Delete Listener

The listen() and listenSticky() methods will automatically delete the listener when the page is destroyed

The listenForever() and listenForeverSticky() methods require you to actively call the following code to delete the listener:

```kotlin
EventService.sampleLiveEvent.removeListener(listener)
```

## Change Log

Please view the [CHANGELOG.md] file


## License
    Copyright (C) 2020 panpf <panpfpanpf@outlook.com>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[platform_android_icon]: https://img.shields.io/badge/Platform-Android-brightgreen.svg
[platform_android_link]: https://android.com
[min_api_icon]: https://img.shields.io/badge/API-16%2B-orange.svg
[min_api_link]: https://developer.android.com/about/dashboards/
[license_icon]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
[version_icon]: https://img.shields.io/maven-central/v/io.github.panpf.liveevent/liveevent
[version_link]: https://repo1.maven.org/maven2/io/github/panpf/liveevent/

[CHANGELOG.md]: CHANGELOG.md
