package com.kuts.klaf.firestore

import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.managers.IAuthenticationSessionManager

class FirebaseAuthenticationSessionManager(
    private val auth: FirebaseAuth,
) : IAuthenticationSessionManager {

    override fun isSignedIn(): Boolean = auth.currentUser != null
}
