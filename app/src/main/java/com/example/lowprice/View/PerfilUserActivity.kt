package com.example.lowprice.View

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lowprice.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PerfilUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_user)

        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "")
        

        val textViewName: TextView = findViewById(R.id.name_profile)
        textViewName.text = "Olá, ${userName ?: "usuário"}"


        val imgProfile: ImageView = findViewById(R.id.photo_profile)
        loadProfileImage(imgProfile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // === Logout ===
        val logoutProfile: ImageView = findViewById(R.id.logout_profile)
        val logoutText: TextView = findViewById(R.id.logout_text)
        val rightProfileLogout: ImageView = findViewById(R.id.right_profile_logout)

        val logoutClickListener = {
            showLogoutConfirmationDialog()
        }

        logoutProfile.setOnClickListener { logoutClickListener() }
        logoutText.setOnClickListener { logoutClickListener() }
        rightProfileLogout.setOnClickListener { logoutClickListener() }

        // === Termos e Condições ===
        val termsTextView: TextView = findViewById(R.id.form_text_profile)
        val profileImageView: ImageView = findViewById(R.id.form_profile)

        val showTermsListener = {
            showTermsAndConditions()
        }

        termsTextView.setOnClickListener { showTermsListener() }
        profileImageView.setOnClickListener { showTermsListener() }

        // === Configurações ===
        val settingsText: TextView = findViewById(R.id.txt_setting_profile)
        val settingsIcon: ImageView = findViewById(R.id.setting_profile)
        val rightProfileConfig: ImageView = findViewById(R.id.right_profile_config)

        val showSettingsListener = {
            showSettingsDialog()
        }

        settingsText.setOnClickListener { showSettingsListener() }
        settingsIcon.setOnClickListener { showSettingsListener() }
        rightProfileConfig.setOnClickListener { showSettingsListener() }

        // === Informações Pessoais ===
        val infoText: TextView = findViewById(R.id.info_profile)
        val infoIcon: ImageView = findViewById(R.id.person_profile)
        val rightInfo: ImageView = findViewById(R.id.right_profile)

        val showInfoListener = {
            showPersonalInfoDialog()
        }

        infoText.setOnClickListener { showInfoListener() }
        infoIcon.setOnClickListener { showInfoListener() }
        rightInfo.setOnClickListener { showInfoListener() }
    }

    private fun loadProfileImage(imageView: ImageView) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val encodedImage = sharedPreferences.getString("profileImage", null)
        encodedImage?.let {
            val byteArray = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val bitmapDrawable = BitmapDrawable(resources, bitmap)

            Glide.with(this)
                .load(bitmapDrawable)
                .transform(CircleCrop())
                .into(imageView)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Você realmente deseja sair?")
            .setPositiveButton("Sim") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun performLogout() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showTermsAndConditions() {
        val terms = getString(R.string.terms_and_conditions)

        AlertDialog.Builder(this)
            .setTitle("Termos e Condições")
            .setMessage(terms)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun showSettingsDialog() {
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName

        val message = """
            Configurações do App
            
            Versão: $appVersion
            Desenvolvido por: PromoApp
            Contato: suporte@promoapp.com
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Configurações")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPersonalInfoDialog() {
        val prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)

        val userName = prefs.getString("userName", "")
        val userPhone = prefs.getString("userPhone", "")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        // Nome (somente leitura)
        val tilName = TextInputLayout(this)
        val etName = TextInputEditText(tilName.context).apply {
            hint = "Nome"
            setText(userName)
            isEnabled = false // torna somente leitura
        }
        tilName.addView(etName)

        // Telefone (somente leitura)
        val tilPhone = TextInputLayout(this)
        val etPhone = TextInputEditText(tilPhone.context).apply {
            hint = "Número"
            setText(userPhone)
            isEnabled = false // torna somente leitura
        }
        tilPhone.addView(etPhone)

        container.addView(tilName)
        container.addView(tilPhone)

        // Título mostrando nome + telefone
        val dialogTitle = if (!userName.isNullOrEmpty() && !userPhone.isNullOrEmpty()) {
            "Informações Pessoais"
        } else {
            "Informações Pessoais"
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(container)
            .setPositiveButton("Fechar", null)
            .show()

    }
}
