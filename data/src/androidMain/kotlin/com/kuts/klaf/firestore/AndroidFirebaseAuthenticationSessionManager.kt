package com.kuts.klaf.firestore

import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.managers.IAuthenticationSessionManager

class AndroidFirebaseAuthenticationSessionManager(
    private val auth: FirebaseAuth,
) : IAuthenticationSessionManager {

    override fun isSignedIn(): Boolean = auth.currentUser != null
}
