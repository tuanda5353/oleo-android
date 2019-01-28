package com.framgia.oleo.screen.login

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.framgia.oleo.base.BaseViewModel
import com.framgia.oleo.data.source.UserRepository
import com.framgia.oleo.data.source.model.User
import com.framgia.oleo.utils.Constant
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
    private val fireBaseAuth: FirebaseAuth,
    private val fireBaseDatabase: FirebaseDatabase
) : BaseViewModel() {

    fun checkLastLogin(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(application) != null
    }

    fun receiveDataUserGoogle(completedTask: Task<GoogleSignInAccount>) {
        val account = completedTask.getResult(ApiException::class.java)
        //Xử lý lưu vào room database
        val user = User(
            account?.id.toString(),
            account?.displayName.toString(),
            account?.email.toString()
        )
        userRepository.deleteUser()
        userRepository.insertUser(user)
        //Xử lý lưu vào firebase database
        fireBaseDatabase.reference.child(Constant.PATH_STRING_USER).child(account!!.id.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(dataSnapshot: DatabaseError) {}

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        fireBaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(account.idToken, null))
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    fireBaseDatabase.reference.child(Constant.PATH_STRING_USER)
                                        .child(account.id.toString()).setValue(user)
                                }
                            }
                    }
                }
            })
    }

    fun receiveDataUserFacebook(result: LoginResult) {
        val request: GraphRequest =
            GraphRequest.newMeRequest(result.accessToken) { jsonObject, response ->
                //Handle get value by key
                userRepository.insertUser(
                    User(
                        jsonObject.getString(ID_KEY),
                        jsonObject.getString(NAME_KEY),
                        jsonObject.getString(EMAIL_KEY)
                    )
                )
            }
        //Request Graph API
        val bundle = Bundle()
        bundle.putString(BUNDLE_FIELDS, BUNDLE_REQUEST_KEY)
        request.parameters = bundle
        request.executeAsync()
    }

    companion object {
        const val BUNDLE_FIELDS = "fields"
        const val BUNDLE_REQUEST_KEY = "id,name,email"
        const val ID_KEY = "id"
        const val NAME_KEY = "name"
        const val EMAIL_KEY = "email"
        fun create(fragment: Fragment, factory: ViewModelProvider.Factory): LoginViewModel =
            ViewModelProvider(fragment, factory).get(LoginViewModel::class.java)
    }
}
