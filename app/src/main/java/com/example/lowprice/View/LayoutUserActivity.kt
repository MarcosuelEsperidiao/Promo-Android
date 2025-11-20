package com.example.lowprice.View

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.lowprice.Model.ApiService.AddProduct_Api
import com.example.lowprice.Model.Product_Add
import com.example.lowprice.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LayoutUserActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var scrollView: ScrollView
    private lateinit var imgPerfil: ImageView
    private lateinit var camProfileImage: ImageView

    // 🔥 WEBSOCKET - Notificações em tempo real
    private lateinit var socket: Socket
    private val SOCKET_URL = "http://192.168.18.170:5000" // Altere para seu IP

    // Handler para atualizar os timestamps periodicamente
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeUpdater = object : Runnable {
        override fun run() {
            updateTimestampViews()
            timeHandler.postDelayed(this, 60_000) // a cada 60s
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_layout_user)

        window.setNavigationBarColor(
            ContextCompat.getColor(
                this, R.color.navigation_bar_color
            )
        )

        camProfileImage = findViewById(R.id.cam_profile_image)
        scrollView = findViewById(R.id.scroll_view)
        imgPerfil = findViewById(R.id.img_perfil)

        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "")
        val textViewName: TextView = findViewById(R.id.t_name)
        textViewName.text = "Olá, ${userName ?: "usuário"}"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileUser: ImageView = findViewById(R.id.profile_user_)
        profileUser.setOnClickListener {
            val intent = Intent(this, PerfilUserActivity::class.java)
            startActivity(intent)
        }

        val iconAddCircle: ImageView = findViewById(R.id.icon_add_circle)
        iconAddCircle.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            startActivity(intent)
        }

        val iconHome: ImageView = findViewById(R.id.icon_home)
        iconHome.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            showSkeletonLoading()
            fetchProducts()
        }

        imgPerfil.setOnClickListener {
            showImagePickerDialog()
        }

        loadProfileImage()
        showSkeletonLoading()

        // 🔥 INICIALIZAR WEBSOCKET PARA NOTIFICAÇÕES
        setupWebSocket()

        Handler(Looper.getMainLooper()).postDelayed({
            fetchProducts()
        }, 100)
    }

    // 🔥 CONFIGURAÇÃO DO WEBSOCKET
    private fun setupWebSocket() {
        try {
            val options = IO.Options()
            options.timeout = -1
            options.reconnection = true
            options.forceNew = true

            socket = IO.socket(SOCKET_URL)

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("WebSocket", "✅ Conectado ao servidor de notificações")
                runOnUiThread {
                }
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("WebSocket", "❌ Desconectado do servidor")
            }

            // 🔥 OUVINDO NOVOS PRODUTOS EM TEMPO REAL
            socket.on("novo_produto") { args ->
                Log.d("WebSocket", "🎉 Nova notificação recebida")
                val data = args[0] as JSONObject
                runOnUiThread {
                    handleNewProductNotification(data)
                }
            }

            socket.connect()

        } catch (e: Exception) {
            Log.e("WebSocket", "Erro na conexão: ${e.message}")
        }
    }

    // 🔥 TRATAR NOTIFICAÇÃO DE NOVO PRODUTO
    private fun handleNewProductNotification(data: JSONObject) {
        try {
            val mensagem = data.getString("mensagem")
            val produtoJson = data.getJSONObject("produto")

            val nomeProduto = produtoJson.getString("location")  // NOME DO PRODUTO
            val preco = produtoJson.getDouble("price")           // PREÇO
            val imgProduto = produtoJson.getString("image")      // IMAGEM
            val local = produtoJson.getString("locario")         // LOCAL
            val usuario = produtoJson.getString("userName")

            // Mostrar notificação do sistema NA SEQUÊNCIA CORRETA
            showSystemNotification(nomeProduto, preco, imgProduto, local)

            // Mostrar Toast
            Toast.makeText(this, "🔥 $nomeProduto por R$$preco", Toast.LENGTH_LONG).show()

            // Atualizar a lista automaticamente
            fetchProducts()

        } catch (e: Exception) {
            Log.e("WebSocket", "Erro ao processar notificação: ${e.message}")
        }
    }

    // 🔥 NOTIFICAÇÃO DO SISTEMA NA SEQUÊNCIA: NOME → PREÇO → IMAGEM → LOCAL
    @SuppressLint("MissingPermission")
    private fun showSystemNotification(nomeProduto: String, preco: Double, imgProdutoBase64: String, local: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(this)

            // Criar canal (para Android 8+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "promo_channel",
                    "Promoções",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            // 🔥 CONVERTER BASE64 PARA BITMAP
            val imagemBitmap = decodeBase64ToBitmap(imgProdutoBase64)

            val notificationBuilder = NotificationCompat.Builder(this, "promo_channel")
                .setContentTitle("🔥 $nomeProduto")  // NOME DO PRODUTO
                .setContentText("R$ $preco\n📍 $local") // PREÇO + LOCAL
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))

            // 🔥 ADICIONAR IMAGEM GRANDE
            imagemBitmap?.let { bitmap ->
                notificationBuilder.setLargeIcon(bitmap)

                // 🔥 PARA ANDROID 7+ - NOTIFICAÇÃO COM IMAGEM GRANDE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val bigPictureStyle = NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)                    // IMAGEM GRANDE
                        .setBigContentTitle("🔥 $nomeProduto") // NOME
                        .setSummaryText("R$ $preco\n📍 $local")  // PREÇO + LOCAL
                    notificationBuilder.setStyle(bigPictureStyle)
                } else {
                    // Para Android antigo
                    notificationBuilder.setStyle(NotificationCompat.BigTextStyle()
                        .bigText("$nomeProduto\nR$ $preco\n📍 $local")) // SEQUÊNCIA CORRETA
                }
            } ?: run {
                // 🔥 SE NÃO CONSEGUIR A IMAGEM, USA SÓ TEXTO NA SEQUÊNCIA
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$nomeProduto\nR$ $preco\n📍 $local")) // SEQUÊNCIA CORRETA
            }

            val notification = notificationBuilder.build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            Log.d("Notification", "Notificação exibida: $nomeProduto - R$$preco")

        } catch (e: Exception) {
            Log.e("Notification", "Erro na notificação: ${e.message}")
            // Fallback na sequência correta
            showFallbackNotification(nomeProduto, preco, local)
        }
    }

    // 🔥 CONVERTER BASE64 PARA BITMAP
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Limpar o base64 se tiver prefixo
            val cleanedBase64 = base64String.replace("data:image/[^;]+;base64,", "")
            val imageBytes = Base64.decode(cleanedBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e("Bitmap", "Erro ao decodificar base64: ${e.message}")
            null
        }
    }

    // 🔥 NOTIFICAÇÃO FALLBACK (SEM IMAGEM) - NA SEQUÊNCIA CORRETA
    @SuppressLint("MissingPermission")
    private fun showFallbackNotification(nomeProduto: String, preco: Double, local: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(this)

            val notification = NotificationCompat.Builder(this, "promo_channel")
                .setContentTitle("🔥 $nomeProduto")        // NOME
                .setContentText("R$ $preco • $local")      // PREÇO + LOCAL
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$nomeProduto\nR$ $preco\n📍 $local")) // SEQUÊNCIA: NOME → PREÇO → LOCAL
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt() + 1, notification)

        } catch (e: Exception) {
            Log.e("Notification", "Erro na notificação fallback: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        timeHandler.post(timeUpdater)

        // Reconectar WebSocket se necessário
        if (!socket.connected()) {
            socket.connect()
        }
    }

    override fun onPause() {
        super.onPause()
        timeHandler.removeCallbacks(timeUpdater)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 🔥 DESCONECTAR WEBSOCKET
        socket.disconnect()
        timeHandler.removeCallbacks(timeUpdater)
    }

    private fun showEmptyState() {
        findViewById<View>(R.id.empty_state).visibility = View.VISIBLE
        findViewById<View>(R.id.scroll_view).visibility = View.GONE
    }

    private fun hideEmptyState() {
        findViewById<View>(R.id.empty_state).visibility = View.GONE
        findViewById<View>(R.id.scroll_view).visibility = View.VISIBLE
    }

    private fun parseTimestampToEpochMillis(timestamp: String?): Long? {
        if (timestamp.isNullOrEmpty()) return null
        val trimmed = timestamp.trim()

        trimmed.toLongOrNull()?.let { num ->
            return if (num > 1_000_000_000_000L) {
                num
            } else {
                num * 1000L
            }
        }

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(trimmed)?.time
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatTimeFromEpoch(epochMillis: Long?): String {
        if (epochMillis == null) return "agora"
        val now = System.currentTimeMillis()
        val diff = now - epochMillis
        if (diff < 0) return "agora"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            seconds < 60 -> "agora"
            minutes < 60 -> if (minutes == 1L) "Há 1 minuto" else "Há $minutes minutos"
            hours < 24 -> if (hours == 1L) "Há 1 hora" else "Há $hours horas"
            else -> "Expirando em breve"
        }
    }

    private fun updateTimestampViews() {
        val productListLayout = findViewById<LinearLayout>(R.id.product_list_layout)
        for (i in 0 until productListLayout.childCount) {
            val child = productListLayout.getChildAt(i) ?: continue
            val textTimestamp = child.findViewById<TextView>(R.id.text_timestamp) ?: continue
            val tag = textTimestamp.tag
            val epochMillis = when (tag) {
                is Long -> tag
                is Int -> tag.toLong()
                is String -> tag.toLongOrNull()
                else -> null
            }
            textTimestamp.text = formatTimeFromEpoch(epochMillis)
        }
    }

    private fun showSkeletonLoading() {
        val productListLayout = findViewById<LinearLayout>(R.id.product_list_layout)
        productListLayout.removeAllViews()

        for (i in 0..2) {
            val skeletonView = layoutInflater.inflate(R.layout.product_item_skeleton, null)
            productListLayout.addView(skeletonView)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tirar Foto", "Escolher da Galeria")
        MaterialAlertDialogBuilder(this)
            .setTitle("Escolha uma opção")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun openGallery() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    saveProfileImage(imageBitmap)
                    loadProfileImage()
                }

                REQUEST_IMAGE_PICK -> {
                    val selectedImage = data?.data
                    selectedImage?.let {
                        val inputStream = contentResolver.openInputStream(it)
                        val imageBitmap = BitmapFactory.decodeStream(inputStream)
                        saveProfileImage(imageBitmap)
                        loadProfileImage()
                    }
                }
            }
        }
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
        editor.putString("profileImage", encodedImage)
        editor.apply()
    }

    private fun loadProfileImage() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val encodedImage = sharedPreferences.getString("profileImage", null)
        if (encodedImage != null) {
            val byteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            Glide.with(this)
                .load(bitmap)
                .transform(CircleCrop())
                .into(imgPerfil)
            camProfileImage.visibility = ImageView.INVISIBLE
        } else {
            camProfileImage.visibility = ImageView.VISIBLE
        }
    }

    private fun fetchProducts() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.18.170:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AddProduct_Api::class.java)
        service.getProducts().enqueue(object : Callback<List<Product_Add>> {
            override fun onResponse(
                call: Call<List<Product_Add>>,
                response: Response<List<Product_Add>>
            ) {
                if (response.isSuccessful) {
                    val productList = response.body() ?: emptyList()
                    if (productList.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        addProductsToLayout(productList)
                    }
                } else {
                    showEmptyState()
                    Toast.makeText(
                        this@LayoutUserActivity,
                        "Falha ao carregar produtos",
                        Toast.LENGTH_SHORT
                    ).show()
                    val productListLayout = findViewById<LinearLayout>(R.id.product_list_layout)
                    productListLayout.removeAllViews()
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Product_Add>>, t: Throwable) {
                Toast.makeText(this@LayoutUserActivity, "Erro: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
                swipeRefreshLayout.isRefreshing = false
                showEmptyState()
            }
        })
    }

    private fun addProductsToLayout(productAdds: List<Product_Add>) {
        val productListLayout = findViewById<LinearLayout>(R.id.product_list_layout)
        productListLayout.removeAllViews()

        for (product in productAdds) {
            try {
                val productView = layoutInflater.inflate(R.layout.product_item, null)
                val textLocation = productView.findViewById<TextView>(R.id.text_location)
                val textPriceDetail = productView.findViewById<TextView>(R.id.text_price_detail)
                val textDescription = productView.findViewById<TextView>(R.id.text_description)
                val textTimestamp = productView.findViewById<TextView>(R.id.text_timestamp)
                val imageViewPreview = productView.findViewById<ImageView>(R.id.imageViewPreview)
                val textUserName = productView.findViewById<TextView>(R.id.text_user_name)
                val imageViewProfile = productView.findViewById<ImageView>(R.id.imageViewProfile)
                val iconLocation = productView.findViewById<ImageView>(R.id.icon_location)

                textLocation.text = product.location
                textPriceDetail.text = "Preço: R$ %.2f".format(product.price).replace('.', ',')
                textDescription.text = "Descrição: ${product.description}"

                val epochMillis = parseTimestampToEpochMillis(product.timestamp)
                textTimestamp.tag = epochMillis
                textTimestamp.text = formatTimeFromEpoch(epochMillis)

                textUserName.text = product.userName

                iconLocation.setOnClickListener {
                    val endereco = product.locario
                    if (!endereco.isNullOrBlank()) {
                        try {
                            val gmmIntentUri = Uri.parse("google.navigation:q=$endereco")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            val chooser = Intent.createChooser(mapIntent, "Escolha um app para navegação")
                            startActivity(chooser)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Endereço indisponível", Toast.LENGTH_SHORT).show()
                    }
                }

                product.image?.let { base64String ->
                    if (base64String.isNotEmpty()) {
                        try {
                            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                            Glide.with(this@LayoutUserActivity)
                                .load(byteArray)
                                .error(android.R.drawable.ic_dialog_alert)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageViewPreview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            imageViewPreview.setImageResource(android.R.drawable.ic_dialog_alert)
                        }
                    } else {
                        imageViewPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                } ?: run {
                    imageViewPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                product.profileImage?.let { base64String ->
                    if (base64String.isNotEmpty()) {
                        try {
                            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                            Glide.with(this@LayoutUserActivity)
                                .load(byteArray)
                                .transform(CircleCrop())
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(R.drawable.profile_person_24)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageViewProfile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            imageViewProfile.setImageResource(android.R.drawable.ic_dialog_alert)
                        }
                    } else {
                        imageViewProfile.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                } ?: run {
                    imageViewProfile.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                productListLayout.addView(productView)
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }
}