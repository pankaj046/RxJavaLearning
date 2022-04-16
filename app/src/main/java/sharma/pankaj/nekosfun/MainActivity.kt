package sharma.pankaj.nekosfun

import android.R.attr.resource
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import sharma.pankaj.nekosfun.Api.apiService


class MainActivity : AppCompatActivity() {

    val nekosKey = arrayListOf(
        "kiss", "lick", "hug", "baka",
        "cry", "poke", "smug", "slap", "tickle", "pat",
        "laugh", "feed", "cuddle"
    )
    private lateinit var key: AutoCompleteTextView
    private lateinit var keyInputLayout: TextInputLayout
    private lateinit var submit: MaterialButton
    private lateinit var image: ImageView
    private lateinit var progress: ProgressBar

    private var selectedKey: String = "hug"
    var disposable: Disposable? = null

    var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

        submit.setOnClickListener {
            bitmap = null
            callApi()
        }

        key.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            selectedKey = nekosKey[position]
        }
    }

    private fun init() {
        key = findViewById<AutoCompleteTextView>(R.id.key)
        keyInputLayout = findViewById<TextInputLayout>(R.id.keyInputLayout)
        image = findViewById<ImageView>(R.id.image)
        progress = findViewById<ProgressBar>(R.id.progress)
        submit = findViewById<MaterialButton>(R.id.submit)
        key.setText("hug")
        val keyAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nekosKey)
        key.setAdapter(keyAdapter)
    }

    private fun callApi() {

        val keyUrl = if (!TextUtils.isEmpty(keyInputLayout.editText?.text)) {
            keyInputLayout.editText?.text.toString()
        } else {
            selectedKey
        }

        apiService().getImage(keyUrl).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<NekosResponse> {

                override fun onSubscribe(d: Disposable) {
                    disposable = d
                    progress.visibility = View.VISIBLE
                    image.visibility = View.GONE
                }

                override fun onError(e: Throwable) {
                    keyInputLayout.editText?.setText("")
                    progress.visibility = View.GONE
                    image.visibility = View.GONE
                    showToast(e.localizedMessage!!)
                }

                override fun onComplete() {
                    progress.visibility = View.VISIBLE
                    image.visibility = View.GONE
                }

                override fun onNext(t: NekosResponse) {
                    if (t.error.isNotEmpty()) {
                        showToast(t.error)
                        progress.visibility = View.VISIBLE
                        image.visibility = View.GONE
                    } else {
                        progress.visibility = View.VISIBLE
                        image.visibility = View.VISIBLE
                        loadImage(t.image)
                    }
                }

            })
    }

    override fun onDestroy() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        super.onDestroy()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
    }


    private fun loadImage(url: String) {
        if (url.contains(".gif")) {
            Glide.with(this@MainActivity)
                .asGif()
                .load(url)
                .listener(object : RequestListener<GifDrawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        image.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        bitmap = resource?.toBitmap()
                        progress.visibility = View.GONE
                        image.visibility = View.VISIBLE
                        keyInputLayout.editText?.setText("")
                        return false
                    }
                })
                .into(image)
        } else {
            Glide.with(this@MainActivity)
                .load(url)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        image.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        bitmap = resource?.toBitmap()
                        progress.visibility = View.GONE
                        image.visibility = View.VISIBLE
                        keyInputLayout.editText?.setText("")
                        return false
                    }
                }).into(image)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_download -> {

                return true
            }
            R.id.action_share -> {
                shareImage()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shareImage() {
        if (bitmap != null) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, "Hey view/download this image")
            val path: String = MediaStore.Images.Media.insertImage(
                contentResolver, bitmap, "", null
            )
            val screenshotUri: Uri = Uri.parse(path)
            intent.putExtra(Intent.EXTRA_STREAM, screenshotUri)
            intent.type = "image/gif"
            startActivity(Intent.createChooser(intent, "Share image via..."))
        } else {
            showToast("Please load image first")
        }

    }
}