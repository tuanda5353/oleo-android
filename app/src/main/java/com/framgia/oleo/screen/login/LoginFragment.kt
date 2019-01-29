package com.framgia.oleo.screen.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.framgia.oleo.R
import com.framgia.oleo.base.BaseFragment
import com.framgia.oleo.databinding.FragmentLoginBinding
import com.framgia.oleo.screen.home.HomeFragment
import com.framgia.oleo.screen.signup.SignUpFragment
import com.framgia.oleo.utils.Constant.MIN_CHARACTER_INPUT_PASSWORD
import com.framgia.oleo.utils.extension.isCheckClickableButtonClick
import com.framgia.oleo.utils.extension.isCheckMultiClick
import com.framgia.oleo.utils.extension.replaceFragment
import com.framgia.oleo.utils.extension.showSnackBar
import com.framgia.oleo.utils.extension.validInputPassword
import com.framgia.oleo.utils.extension.validInputPhoneNumber
import com.framgia.oleo.utils.liveData.autoCleared
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.android.synthetic.main.fragment_login.buttonLogin
import kotlinx.android.synthetic.main.fragment_login.buttonLoginFB
import kotlinx.android.synthetic.main.fragment_login.editTextPassword
import kotlinx.android.synthetic.main.fragment_login.editTextPhoneNumber
import kotlinx.android.synthetic.main.fragment_login.textLayoutPassWord
import kotlinx.android.synthetic.main.fragment_login.textLayoutPhoneNumber
import kotlinx.android.synthetic.main.fragment_login.textViewLoginFB
import kotlinx.android.synthetic.main.fragment_login.textViewLoginGG
import kotlinx.android.synthetic.main.fragment_login.textViewSignUp
import java.util.Arrays

class LoginFragment : BaseFragment(), View.OnClickListener {

    private lateinit var callBackManager: CallbackManager
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var viewModel: LoginViewModel
    private var binding by autoCleared<FragmentLoginBinding>()

    override fun createView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = LoginViewModel.create(this, viewModelFactory)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.viewModel = viewModel
        binding.setLifecycleOwner(this)
        return binding.root
    }

    override fun setUpView() {
        callBackManager = CallbackManager.Factory.create()
        onCheckTextChanged()
        initGoogle()
        textLayoutPassWord.isPasswordVisibilityToggleEnabled = true

        buttonLogin.setOnClickListener(this)
        textViewLoginFB.setOnClickListener(this)
        textViewLoginGG.setOnClickListener(this)
        buttonLogin.setOnClickListener(this)
        textViewSignUp.setOnClickListener(this)
    }

    override fun bindView() {
        signInWithFacebook()
        buttonLoginFB.fragment = this
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.textViewLoginFB -> if (isCheckMultiClick()) buttonLoginFB.performClick()
            R.id.textViewLoginGG -> if (isCheckMultiClick()) signInWithGoogle()
            R.id.buttonLogin -> onCheckValidateFormAndLogin()
            R.id.textViewSignUp -> SignUpFragment.newInstance().show(fragmentManager, TAG_DIALOG)
        }
    }

    private fun signInWithFacebook() {
        buttonLoginFB.setReadPermissions(Arrays.asList(PUBLIC_PROFILE, EMAIL))
        buttonLoginFB.registerCallback(callBackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                if (result != null) {
                    viewModel.receiveDataUserFacebook(result)
                    replaceFragment(R.id.containerMain, HomeFragment.newInstance())
                } else {
                    Toast.makeText(context, REQUEST_NULL, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancel() {
            }

            override fun onError(error: FacebookException?) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callBackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_REQUEST) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                viewModel.receiveDataUserGoogle(task)
                if (task.exception == null) replaceFragment(R.id.containerMain, HomeFragment.newInstance())
            } catch (e: ApiException) {
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> view!!.showSnackBar(GOOGLE_SIGN_CANCELLED)
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> view!!.showSnackBar(GOOGLE_SIGN_FAILED)
                }
            }
        }
    }

    private fun initGoogle() {
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(activity!!, googleSignInOptions)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, LoginFragment.GOOGLE_REQUEST)
    }

    private fun revokeAccess() {
        googleSignInClient.revokeAccess().addOnCompleteListener(activity!!) { task ->
            if (task.isSuccessful) {
            }
        }
    }

    private fun onCheckValidateFormAndLogin() {
        if (validInputPhoneNumber(
                context!!, editTextPhoneNumber.text.toString(), textLayoutPhoneNumber
            ) && validInputPassword(
                context!!, editTextPassword.text.toString(), textLayoutPassWord
            )
        ) {
            replaceFragment(R.id.containerMain, HomeFragment.newInstance())
        }
        isCheckClickableButtonClick(buttonLogin)
    }

    private fun onCheckTextChanged() {
        editTextPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {}

            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                textLayoutPhoneNumber.error = ""
                onSetEnableButtonLogin(editTextPhoneNumber.text.toString(), editTextPassword.text.toString())
            }
        })

        editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {}

            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                textLayoutPassWord.error = ""
                onSetEnableButtonLogin(editTextPhoneNumber.text.toString(), editTextPassword.text.toString())
            }
        })
    }

    private fun onSetEnableButtonLogin(textPhone: String, textPassword: String) {
        if (textPhone.isNotBlank() && textPassword.length >= MIN_CHARACTER_INPUT_PASSWORD) {
            buttonLogin.isEnabled = true
            buttonLogin.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorButtonLogin))
        } else {
            buttonLogin.isEnabled = false
            buttonLogin.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorGrey400))
        }
    }

    companion object {
        const val GOOGLE_REQUEST = 1
        const val REQUEST_NULL = "Data Null"
        const val PUBLIC_PROFILE = "public_profile"
        const val EMAIL = "email"
        const val GOOGLE_SIGN_CANCELLED = "Sign Cancelled"
        const val GOOGLE_SIGN_FAILED = "Sign Failed"
        const val TAG_DIALOG = "SignUp"

        fun newInstance() = LoginFragment().apply {
            val bundle = Bundle()
            arguments = bundle
        }
    }
}
