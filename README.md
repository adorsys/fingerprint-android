# finger

[![Build Status](https://travis-ci.com/adorsys/fingerprint-android.svg?branch=master)](https://travis-ci.com/adorsys/fingerprint-android)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This fingerprint library aims to make the use of fingerprint authentication in your application as simple as possible and is best suited for developers who just want to have to deal with 3 things:

* was the fingerprint successfully accepted
* was there an error while validating the fingerprint (and which) and provide custom error messages
* show a system fingerprint dialog / biometric dialog without having to consider the API level or write boilerplate code

First, include _finger_ in your project by adding
  ````groovy
  // add the finger and also the biometric dependency  
implementation "de.adorsys.android:finger:${latestFingerVersion}"
implementation "androidx.biometric:biometric:1.0.0"
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
            strings = Finger.DialogStrings(
                title = getString(R.string.text_fingerprint),
                subTitle = "", // defaults to null if nothing is assigned    
                description = "", // defaults to null if nothing is assigned
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
	Pair(BiometricPrompt.ERROR_HW_UNAVAILABLE, getString(R.string.error_override_hw_unavailable)),
	Pair(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, getString(R.string.error_override_unable_to_process)),
	Pair(BiometricPrompt.ERROR_TIMEOUT, getString(R.string.error_override_error_timeout)),
	Pair(BiometricPrompt.ERROR_NO_SPACE, getString(R.string.error_override_no_space)),
	Pair(BiometricPrompt.ERROR_CANCELED, getString(R.string.error_override_canceled)),
	Pair(BiometricPrompt.ERROR_LOCKOUT, getString(R.string.error_override_lockout)),
	Pair(BiometricPrompt.ERROR_VENDOR, getString(R.string.error_override_vendor)),
	Pair(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, getString(R.string.error_override_lockout_permanent)),
	Pair(BiometricPrompt.ERROR_USER_CANCELED, getString(R.string.error_override_user_cancel)),
	Pair(Finger.ERROR_NOT_RECOGNIZED, getString(R.string.error_override_not_recognized)))
val finger = Finger(applicationContext, errors)
````

Usually, `errors` is defined as an `emptyMap()` as default argument.
   
```` kotlin
val finger = Finger(context = this)
````
or

```` kotlin
val finger = Finger(context = this, errors = errors)
````
The latter uses only the system error messages if no own error message can be found in the map. So you can very well customize when to show which message.

### Documentation
This project contains documentation. To generate it run `./gradlew finger:dokka`. 
You will find it at `project/documentation`. 

### Proguard
-keep class de.adorsys.android.finger.**

-dontwarn de.adorsys.android.finger.**

### Contributors
[@luckyhandler](https://github.com/luckyhandler)
