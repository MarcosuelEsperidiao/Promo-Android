package com.example.lowprice.View

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lowprice.Model.ApiService.CreatUser_Api
import com.example.lowprice.Model.CreatUser
import com.example.lowprice.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserCreatActivity : AppCompatActivity() {

    private lateinit var userService: CreatUser_Api


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_creat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.18.170:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        userService = retrofit.create(CreatUser_Api::class.java)

        val nameEditText: EditText = findViewById(R.id.text_name)
        val phoneEditText: EditText = findViewById(R.id.text_phone)
        val passwordEditText: EditText = findViewById(R.id.text_creat_password)
        val createAccountButton: Button = findViewById(R.id.btn_creat_count)

        // Adicionar TextWatcher ao EditText de telefone para formatação
        phoneEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val hint = phoneEditText.hint // Armazenar o hint original

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val unmasked = s.toString().replace("[^\\d]".toRegex(), "")
                val masked = formatPhoneNumber(unmasked)

                isUpdating = true
                s?.replace(0, s.length, masked)
                isUpdating = false

                if (s.isNullOrEmpty()) {
                    phoneEditText.hint = hint // Restaurar o hint quando o campo estiver vazio
                }
            }

            private fun formatPhoneNumber(s: String): String {
                val length = s.length
                return when {
                    length == 0 -> ""
                    length <= 2 -> "($s"
                    length <= 7 -> "(${s.substring(0, 2)}) ${s.substring(2)}"
                    length <= 11 -> "(${s.substring(0, 2)}) ${s.substring(2, 3)} ${s.substring(3, 7)}-${s.substring(7)}"
                    else -> "(${s.substring(0, 2)}) ${s.substring(2, 3)} ${s.substring(3, 7)}-${s.substring(7, 11)}"
                }
            }
        })

        createAccountButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else if (phone.length < 16) { // Número de telefone formatado deve ter 16 caracteres
                Toast.makeText(this, "O número de telefone deve ter no mínimo 11 dígitos", Toast.LENGTH_SHORT).show()
            } else if (password.length != 6) { // Senha deve ter no mínimo 6 dígitos
                Toast.makeText(this, "A senha deve ter  6 dígitos", Toast.LENGTH_SHORT).show()
            } else {
                val userCreateRequest = CreatUser(name, phone, password)

                userService.createUser(userCreateRequest).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@UserCreatActivity, "Conta criada com sucesso", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@UserCreatActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@UserCreatActivity, "Numéro já em uso", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(this@UserCreatActivity, "Erro: " + t.message, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
