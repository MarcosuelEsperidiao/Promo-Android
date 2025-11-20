package com.example.lowprice.View

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lowprice.Model.ApiService.AddProduct_Api
import com.example.lowprice.Model.Product_Add
import com.example.lowprice.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var editTextDescription: EditText
    private lateinit var editTextLocario: EditText
    private lateinit var imageViewPreview: ImageView
    private lateinit var editTextLocation: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var sendButton: Button
    private var imageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_product)

        window.setNavigationBarColor(
            ContextCompat.getColor(this, R.color.navigation_bar_color)
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        editTextDescription = findViewById(R.id.description)
        editTextLocario = findViewById(R.id.text_locario)
        imageViewPreview = findViewById(R.id.imageViewPreview)
        editTextLocation = findViewById(R.id.text_location)
        editTextPrice = findViewById(R.id.price)
        val imageViewLocation: ImageView = findViewById(R.id.imageView_location)
        sendButton = findViewById(R.id.sendButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageViewCamera: ImageView = findViewById(R.id.imageViewCamera)

        imageViewCamera.setOnClickListener {
            abrirCamera()
        }

        // 🔹 Quando clicar no ícone de localização, ele some e busca a localização
        imageViewLocation.setOnClickListener {
            it.visibility = View.GONE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                getLastLocation()
            }
        }

        sendButton.setOnClickListener {
            salvarInformacoes()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFields()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editTextLocation.addTextChangedListener(textWatcher)
        editTextLocario.addTextChangedListener(textWatcher)
        editTextPrice.addTextChangedListener(textWatcher)

        imageViewPreview.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            checkAllFields()
        }

        // 🔹 Esconde ou mostra o ícone dependendo do texto em editTextLocario
        editTextLocario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                imageViewLocation.visibility =
                    if (s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        checkAllFields()
    }

    private fun abrirCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = createImageFile()
            photoFile?.let {
                imageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.lowprice.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageUri?.let {
                imageViewPreview.setImageURI(it)
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getAddressFromLocation(latitude, longitude)
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)!!
            if (addresses.isNotEmpty()) {
                val address: String = addresses[0].getAddressLine(0)
                editTextLocario.setText(address)
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Geocoder error", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error retrieving address", Toast.LENGTH_SHORT).show()
        }
    }



    private fun salvarInformacoes() {
        val locationText = editTextLocation.text.toString()
        val locarioText = editTextLocario.text.toString()
        val priceText = editTextPrice.text.toString()
        val descriptionText = editTextDescription.text.toString()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val sharedPreferencesUser = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val userName = sharedPreferencesUser.getString("userName", "")
        val userProfileImageString = sharedPreferencesUser.getString("profileImage", "R.mipmap.profile_image_default_round")

        val sharedPreferences = getSharedPreferences("ProductInfo", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val productCount = sharedPreferences.getInt("product_count", 0)

        editor.putString("text_location_$productCount", locationText)
        editor.putString("text_locario_$productCount", locarioText)
        editor.putString("price_$productCount", priceText)
        editor.putString("description_$productCount", descriptionText)
        editor.putString("timestamp_$productCount", timestamp)

        val bitmap = (imageViewPreview.drawable as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream)
        val byteArray = stream.toByteArray()
        val imageString = Base64.encodeToString(byteArray, Base64.DEFAULT)
        editor.putString("image_$productCount", imageString)

        editor.putInt("product_count", productCount + 1)
        editor.apply()

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

        val service = retrofit.create(AddProduct_Api::class.java)

        val productAdd = Product_Add(
            locationText,
            locarioText,
            priceText.toFloat(),
            imageString,
            userName ?: "",
            userProfileImageString ?: "",
            descriptionText,
            timestamp
        )

        service.addProduct(productAdd).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AddProductActivity,
                        "Produto adicionado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@AddProductActivity, LayoutUserActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@AddProductActivity,
                        "Erro ao adicionar produto",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@AddProductActivity, "Error: " + t.message, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun checkAllFields() {
        val isLocationFilled = editTextLocation.text.toString().isNotEmpty()
        val isLocarioFilled = editTextLocario.text.toString().isNotEmpty()
        val isPriceFilled = editTextPrice.text.toString().isNotEmpty()
        val isImageFilled = imageViewPreview.drawable != null

        sendButton.isEnabled = isLocationFilled && isLocarioFilled && isPriceFilled && isImageFilled
    }
}
