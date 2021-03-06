# Next version

- Features
  - Fetch User-Agent asynchronously to speed up the SDK initialization.
  - MoPub Header-Bidding: clean MoPub's keyword to support Auto-Refreshing banners

# Version 3.8.0

- Features
  - Provide legal privacy text for native in `CriteoNativeAd#getLegalText`
  - Artifacts are now delivered through `JCenter` repository instead of a custom one: from this
  version, the line `maven { url "https://pubsdk-bin.criteo.com/publishersdk/android" }` can be
  removed.

# Version 3.7.0

- Features
  - *Advanced native ads* public release; integration instructions and documentation available on
  our [support website](https://publisherdocs.criteotilt.com/app/android/)

# Version 3.6.0

- Features
  - Insert `crt_size` keywords for DFP, MoPub and Custom Header-Bidding integration on banner
  - Insert `crt_size` keywords for DFP Header-Bidding integration on interstitial
  - Deactivate some debug logs in release build

- Bug fixes
  - Fix all known memory leaks
