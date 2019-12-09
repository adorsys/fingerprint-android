# finger

[![Build Status](https://travis-ci.org/adorsys/fingerprint-android.svg?branch=master)](https://travis-ci.org/adorsys/fingerprint-android)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This fingerprint library aims to make the use of fingerprint authentication in your application as simple as possible and is best suited for developers who just want to have to deal with 3 things:

* was the fingerprint successfully accepted
* was there an error while validating the fingerprint (and which) and provide custom error messages
* show a system fingerprint dialog / biometric dialog without having to consider the API level or write boilerplate code

First, include _finger_ in your project by adding
  ````groovy
  // minimum Version for this readme is 1.0.0
implementation "de.adorsys.android:finger:${latestFingerVersion}"
````

## usage
You can use _finger_ as follows:

```` kotlin 
val finger = Finger(context) // will internally always use application context
finger.subscribe(object : FingerListener {
                  override fun onFingerprintAuthenticationSuccess() {
                      // The user authenticated successfully -> go on with your logic
                  }
                  
                  override fun onFingerprintAuthenticationFailure(errorMessage: String, errorCode: Int) {
                      // Show the user the human readable error message and use the error code if necessary 
                      // and subscribe again
                  }
              })
              
finger.showDialog(
            activity = this,
            strings = DialogStrings(
                title = getString(R.string.text_fingerprint),  
                subtitle = "" // defaults to null if nothing is assigned    
                description = "" // defaults to null if nothing is assigned
                cancelButtonText = "login with password" // default parameter is android.R.cancel 
            )                         
        )
````

You should subscribe in onResume and unsubscribe in onPause:
````kotlin
override fun onResume() {
    super.onResume()
    finger.subscribe(this)
}

override fun onPause() {
    super.onPause()
    finger.unSubscribe()
}
````

##  error handling
_finger_ usually emits the standard system error messages. But you can also assign _finger_ a map of errors for each error type:
 
```` kotlin
val errors = mapOf(
	Pair(FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE, getString(R.string.error_override_hw_unavailable)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS, getString(R.string.error_override_unable_to_process)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_TIMEOUT, getString(R.string.error_override_error_timeout)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_NO_SPACE, getString(R.string.error_override_no_space)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_CANCELED, getString(R.string.error_override_canceled)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_LOCKOUT, getString(R.string.error_override_lockout)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_VENDOR, getString(R.string.error_override_vendor)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT, getString(R.string.error_override_lockout_permanent)),
    Pair(FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED, getString(R.string.error_override_user_cancel)),
    Pair(Finger.ERROR_NOT_RECOGNIZED, getString(R.string.error_override_not_recognized)))
val finger = Finger(applicationContext, errors)
````

Usually, errors is defined as an emptyMap() as default argument.
   
```` kotlin
val finger = Finger(context = this)
````
or

```` kotlin
val finger = Finger(context = this, errors = errors)
````
The latter uses only the system error messages if no own error message can be found in the map. So you can very well customize when to show which message.



### Proguard
-keep class de.adorsys.android.finger.**
-dontwarn de.adorsys.android.finger.**

### Contributors
[@luckyhandler](https://github.com/luckyhandler)
