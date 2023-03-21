package com.example.klaf.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.klaf.presentation.common.log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val auth: FirebaseAuth
): ViewModel() {

    init {


//        auth.currentUser?.delete()

        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
//            log(user?.email, "user is email verified")
            log(user?.isEmailVerified, "auth state is email verified")
            log(user?.displayName, "auth state user name")
//            log(user?.isAnonymous, "user isAnonymous")
//            log(user?.metadata., "user ")
//            log(user, "user ")
//            log(user, "user ")
//            viewModelScope.launchWithState(Dispatchers.IO) {
//                delay(3000)
//                val profileUpdates = userProfileChangeRequest {
//                    displayName = "11111111111111111111111"
//                }

//                user?.apply {
//                    updateProfile(profileUpdates).await()
//                    log("name is updated")
//                }
//                log(user?.displayName, "name after updating")
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Log.d(TAG, "User profile updated.")
//                        }
//                    }
//                }

//                auth.currentUser?.reload()
//            }
        }
        auth.addIdTokenListener { firebaseAuth: FirebaseAuth ->
            log(firebaseAuth.currentUser?.isEmailVerified, "token listener isEmailVerified")
//            val user = firebaseAuth.currentUser
//            if (user != null) {
//                val isEmailVerified = user.isEmailVerified
//                // Update your UI or perform any necessary action based on the email verification state.
//            }
        }

        viewModelScope.launchWithState(Dispatchers.IO) {
            repeat(times = 100) {
                delay(5000)
                log(auth.currentUser?.isEmailVerified, "email checker isEmailVerified")
            }
        }







        val user = auth.currentUser
        log(user, "------ user -------")

        if (user != null) {

            viewModelScope.launchWithState {
                user.delete().await()
//                    auth.signOut()
                log("sign out or deleting is completed")
            } onException { _, error ->
                log(error, "sign out or deleting task exception")
                viewModelScope.launchWithState {
                    user.reauthenticate(
                        EmailAuthProvider.getCredential("survivenik@gmail.com", "password")
                    ).await()
                    log("reuthentication is finished")
                } onException { _, reauthError ->
                    log(reauthError, "reauthentication is failed")
                }

            }

//            log(user.isEmailVerified, "is email verified?")

//            if (user.isEmailVerified) {
//                viewModelScope.launchWithState {
//                    user.delete().await()
////                    auth.signOut()
//                    log("sign out or deleting is completed")
//                } onException { _, error ->
//                    log(error, "sign out or deleting task exception")
//                    viewModelScope.launchWithState {
//                        user.reauthenticate(
//                            EmailAuthProvider.getCredential("survivenik@gmail.com", "password")
//                        ).await()
//                        log("reuthentication is finished")
//                    } onException { _, reauthError ->
//                        log(reauthError, "reauthentication is failed")
//                    }
//
//                }
//            } else {
////                viewModelScope.launchWithState(Dispatchers.IO) {
////                    log("start email verification ")
////
////                    user.sendEmailVerification().await()
////
////                    log("finish email verification")
////                    log(user.isEmailVerified, "is email verified after sending request?")
////                } onException { _, error ->
////                    log(error, "email verification is failed")
////                }
//            }

//            workManager.performDataSynchronization()
        } else {
            /** sine up **/
            val job = viewModelScope.launchWithState {
                val some = auth.createUserWithEmailAndPassword(
                    "survivenik@gmail.com",
                    "password"
                ).await()
                log(some.user, "sign up result")
            } onException { _, error ->
                log(error, "sign up is failed")
                if (error is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    viewModelScope.launchWithState {
                        val authResult = auth.signInWithEmailAndPassword(
                            "survivenik@gmail.com",
                            "password"
                        ).await()
                        log(authResult.user, "sign in is finished")
                    } onException { _, error ->
                        log(error, "sign in is failed")
                    }
                }
            }

//            viewModelScope.launchWithState {
//                job.join()
//                val some = auth.signInWithEmailAndPassword(
//                    "survivenik@gmail.com",
//                    "password"
//                ).await()
//                log(some.user, "sign in result ")
//            } onException { _, _ ->
//                log("sign is failed")
//            }


        }
    }
}