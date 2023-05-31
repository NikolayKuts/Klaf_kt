package com.kuts.klaf.data.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

fun FirebaseFirestore.rootCollection(email: String): CollectionReference {
    return collection("$ROOT_COLLECTION_NAME_PREFIX$email")
}