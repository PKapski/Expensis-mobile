package pl.polsl.expensis_mobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import kotlinx.android.synthetic.main.login_activity.*
import org.json.JSONObject
import pl.polsl.expensis_mobile.R
import pl.polsl.expensis_mobile.dto.LoginFormDTO
import pl.polsl.expensis_mobile.rest.BASE_URL
import pl.polsl.expensis_mobile.rest.Endpoint
import pl.polsl.expensis_mobile.rest.VolleySingleton
import pl.polsl.expensis_mobile.dto.LoginDTO
import pl.polsl.expensis_mobile.others.LoggedUser
import pl.polsl.expensis_mobile.utils.Messages
import pl.polsl.expensis_mobile.utils.Utils.Companion.createUserJsonBuilder
import pl.polsl.expensis_mobile.validators.LoginValidator

class LoginActivity : AppCompatActivity() {

    private val accessTokenConst = "access_token"
    private val refreshTokenConst = "refresh_token"
    private val userConst = "user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
    }

    fun onRegisterClicked(view: View) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    fun onLoginClicked(view: View) {

        val loginFormDTO = LoginFormDTO(emailInput, passwordInput)
        val loginValidator = LoginValidator()
        val validationResult = loginValidator.validate(loginFormDTO)

        if (validationResult.isValid) {
            val loginDTO = LoginDTO(loginFormDTO)
            val userJsonObject = JSONObject(Gson().toJson(loginDTO))
            val url = BASE_URL + Endpoint.AUTH

            val objectRequest = JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    userJsonObject,
                    { response ->
                        processResponse(response)
                        startActivity(Intent(this, MenuActivity::class.java))
                    },
                    { error ->
                        println("error! $error")
                        Toast.makeText(this, Messages.INVALID_EMAIL_OR_PASSWORD, Toast.LENGTH_SHORT).show()
                    }

            )
            VolleySingleton.getInstance(this).addToRequestQueue(objectRequest)
        }
        else {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_SHORT).show()
        }



    }

    private fun processResponse(response: JSONObject) {
        val accessToken: String = response.get(accessTokenConst) as String
        val refreshToken: String = response.get(refreshTokenConst) as String
        val userObj = response.get(userConst) as JSONObject

        val loggedUser = createUserJsonBuilder().fromJson<LoggedUser>(
                userObj.toString(),
                LoggedUser::class.java
        )

        println(loggedUser.toString())

        storeTokens(accessToken, refreshToken)
        println("Rest response = $response")
    }

    private fun storeTokens(accessToken: String, refreshToken: String) {
        val pref = applicationContext.getSharedPreferences("MyPref", Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(accessTokenConst, accessToken)
        editor.putString(refreshTokenConst, refreshToken)
        editor.apply()
    }



}