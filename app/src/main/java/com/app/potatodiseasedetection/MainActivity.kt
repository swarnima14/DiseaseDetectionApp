package com.app.potatodiseasedetection


import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.ETC1
import android.os.*
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.potatodiseasedetection.ml.Potatodiseasedetection
import com.app.potatodiseasedetection.ml.Resnet50ForPotatoLeafRgbNew
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_custom.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import pyxis.uzuki.live.mediaresizer.MediaResizer
import pyxis.uzuki.live.mediaresizer.data.ImageResizeOption
import pyxis.uzuki.live.mediaresizer.data.ResizeOption
import pyxis.uzuki.live.mediaresizer.model.ImageMode
import pyxis.uzuki.live.mediaresizer.model.MediaType
import pyxis.uzuki.live.mediaresizer.model.ScanRequest
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    lateinit var bitmap: Bitmap
    lateinit var date: String
    lateinit var photoFile: File
    lateinit var fileProvider: Uri
    var pfile: File? = null
    lateinit var interpreter: Interpreter
    lateinit var currentLang: String
    lateinit var fileName:String
    lateinit var output: ByteBuffer

    var uri: Uri? = null
    val FILE_NAME = "pic"
    var name: String? = null

    var count =0

    var list: MutableList<String> = ArrayList()

    internal var myExternalFile: File?=null

    var pressGal = false
    var pressCam = false

    var btnGal = false
    var btnCam = false


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*if(isPermissionGranted())
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        else
            takePermission()*/

        setSupportActionBar(toolbar)
        toolbar.title = "Potato Disease Detection"

        val prefs = getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE)
        currentLang = prefs.getString("myLanguage", "eng").toString()

        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)
        date = sdf.format(Date())

        list.add("DISEASED")
        list.add("HEALTHY")
        list.add("रोगी")
        list.add("स्वस्थ")

        btnGallery.setOnClickListener {
            btnGal = true
            btnCam = false

                if(isPermissionGranted())
                takeWritePermission()
            else{
                takePermission()
                }



        }

        btnCapture.setOnClickListener(View.OnClickListener {
            btnCam = true
            btnGal = false

            if (isPermissionGranted())
                openCamera()
            else {
                takePermission()
            }


        })

        ibSave.setOnClickListener {

            if(pressGal) {
                val s = FileUtil.getPath(uri!!, this)
                photoFile = File(s)

                if(name == "Invalid")
                    Toast.makeText(this, getString(R.string.invalid), Toast.LENGTH_SHORT).show()

                else if(photoFile != null && name != null && name != getString(R.string.invalid))
                    saveImageInDevice(photoFile)

                else
                    Toast.makeText(this, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()
            }
            else {

                val prefs = getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
                val fi = prefs.getString("myFile", "").toString()
                photoFile = File(fi)
                if(name == "Invalid")
                    Toast.makeText(this, getString(R.string.invalid), Toast.LENGTH_SHORT).show()

                else if(photoFile != null && name != null && name != getString(R.string.invalid))
                    saveImageInDevice(photoFile)

                else
                    Toast.makeText(this, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()
            }



            ivImg.setImageResource(R.drawable.no_image)
            tvResult.text = getString(R.string.health_status_text)
        }

        ibUpload.setOnClickListener {
            uploadAsync(this, date, list).execute()
        }
    }

    private fun takeWritePermission() {
        /*if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        }

        else {*/
            // Permission has already been granted

            pressGal = false
            ivImg.setImageResource(R.drawable.no_image)
            tvResult.text = getString(R.string.health_status_text)

            var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, 97)

    }

    private fun takePermission() {
        /*if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.setData(Uri.parse(String.format("package:%s", applicationContext.packageName)))
                startActivityForResult(intent, 1)
            }catch (e: Exception){
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 1)
            }
        }
        else{*/
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), 2)
        
    }

    private fun isPermissionGranted(): Boolean {
        /*if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R)
            return Environment.isExternalStorageManager()
        else{*/
            val readExternalStoragePermission = ContextCompat.checkSelfPermission(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())
            return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if(grantResults.size > 0 && requestCode ==2){
            val readExtStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if(readExtStorage){
                Toast.makeText(this, "perm granted now", Toast.LENGTH_SHORT).show()
                if(btnCam)
                openCamera()
                if(btnGal)
                    takeWritePermission()
            }
            else{
                takePermission()
            }
        }

    }

    fun openCamera()
    {
        pressCam = false

        ivImg.setImageResource(R.drawable.no_image)
        tvResult.text = getString(R.string.health_status_text)

        var camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getPhotoFile(FILE_NAME)

        val editor = getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString("myFile", photoFile.toString())
        editor.apply()
        editor.commit()

        fileProvider = FileProvider.getUriForFile(this, "com.app.potatodiseasedetection.fileprovider", photoFile!!)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        pfile = photoFile

        startActivityForResult(camIntent, 99)

    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

         if(resultCode == Activity.RESULT_OK){
             if(requestCode == 1){
                 if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
                     if(Environment.isExternalStorageManager()){
                         Toast.makeText(this, "perm granted", Toast.LENGTH_SHORT).show()
                        // openCamera()
                     }
                 }
             }
         }

         if(requestCode == 99 && resultCode == Activity.RESULT_OK) {

             pressCam = true

             setLayout(getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).getString("myLanguage", "eng").toString())

             val editor = getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
             photoFile = File(editor.getString("myFile", "").toString())

             bitmap = BitmapFactory.decodeFile(photoFile.path)
             ivImg.setImageBitmap(bitmap)

             val resizeOption = ImageResizeOption.Builder()
                     .setImageProcessMode(ImageMode.ResizeAndCompress)
                     .setImageResolution(1280, 720)
                     .setBitmapFilter(false)
                     .setCompressFormat(Bitmap.CompressFormat.JPEG)
                     .setCompressQuality(75)
                     .setScanRequest(ScanRequest.TRUE)
                     .build()

             val option = ResizeOption.Builder()
                     .setMediaType(MediaType.IMAGE)
                     .setImageResizeOption(resizeOption)
                     .setTargetPath(photoFile.absolutePath)
                     .setOutputPath(photoFile.absolutePath)
                     .build()

             MediaResizer.process(option)

             uri = Uri.fromFile(photoFile)
             bitmap = BitmapFactory.decodeFile(photoFile.path)

            // downloadCustomModel()

             CoroutineScope(Dispatchers.Main).launch {
                 predictName()
                 //predictAsync(this@MainActivity, bitmap, tvResult).execute()
                 //downloadCustomModel()

             }


             try {

                 //predictAsync(this, bitmap, tvResult).execute()
             }catch (e: Exception){
                 Toast.makeText(this, "${getString(R.string.error_toast)} ${e.message}", Toast.LENGTH_LONG).show()
             }
         }

         if(resultCode == RESULT_OK && requestCode == 97 && data!=null) {

             pressGal = true

             ivImg.setImageURI(data!!.data)
             uri = data!!.data


             photoFile = File(uri.toString())
             predictName()

             CoroutineScope(Dispatchers.Main).launch {
                // predictName()
                 //predictAsync(this@MainActivity, bitmap, tvResult).execute()
                 //downloadCustomModel()

             }


             try {

                 //predictAsync(this, bitmap, tvResult).execute()
             }catch (e: Exception){
                 Toast.makeText(this, "${getString(R.string.error_toast)} ${e.message}", Toast.LENGTH_LONG).show()
             }

         }

    }

    private  fun predictName(){


      //  withContext(Dispatchers.Main) {
            val a = FileUtil.getPath(uri!!, applicationContext)

            bitmap = BitmapFactory.decodeFile(File(a.toString()).absolutePath)
            fileName = if(currentLang == "hin"){
                "classHindi.txt"
            }

            else {
                "plant_labels.txt"
            }
            var max = 0
            val inpString = application.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            var resized: Bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

             output = convertBitmapToByteBuffer(resized)

            val model = Resnet50ForPotatoLeafRgbNew.newInstance(this@MainActivity)

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            inputFeature0.loadBuffer(output!!)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer



/*val tbuffer = TensorImage.fromBitmap(resized)
            var byteBuffer = tbuffer.buffer
            inputFeature0.loadBuffer(byteBuffer)
            val outputs = model.process(inputFeature0)
            var outputFeature0 = outputs.outputFeature0AsTensorBuffer*/

            max = getMax(outputFeature0.floatArray)


            if (max == -1) {
                tvResult.text = getString(R.string.health_status_text) + " ${getString(R.string.invalid)}"
                name = "Invalid"
            } else {
                name = cropList[max]
                tvResult.text = getString(R.string.health_status_text) + " ${cropList[max]}"
            }
           // ivImg.setImageBitmap(getOutputImage())
            model.close()


    }

    private fun convertBitmapToByteBuffer(bmp: Bitmap): ByteBuffer {
        // Specify the size of the byteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        // Calculate the number of pixels in the image
        val pixels = IntArray(224 * 224)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        var pixel = 0
        // Loop through all the pixels and save them into the buffer
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val pixelVal = pixels[pixel++]

                // Do note that the method to add pixels to byteBuffer is different for quantized models over normal tflite models
                /* byteBuffer.put((pixelVal shr 16 and 0xFF).toByte())
                 byteBuffer.put((pixelVal shr 8 and 0xFF).toByte())
                 byteBuffer.put((pixelVal and 0xFF).toByte())*/

                byteBuffer.putFloat((pixelVal shr 16 and 0xFF) / 255f)
                byteBuffer.putFloat((pixelVal shr 8 and 0xFF) / 255f)
                byteBuffer.putFloat((pixelVal and 0xFF) / 255f)
            }
        }

        // Recycle the bitmap to save memory
        bmp.recycle()
        return byteBuffer




    }

   /* @JvmName("setBitmap1")
    fun setBitmap(bmp: Bitmap): Array<ByteBuffer> {

        val imageSize = bmp.rowBytes * bmp.height
        val uncompressedBuffer = ByteBuffer.allocateDirect(imageSize)
        bmp.copyPixelsToBuffer(uncompressedBuffer)
        uncompressedBuffer.position(0)
        val compressedBuffer = ByteBuffer.allocateDirect(
                ETC1.getEncodedDataSize(bmp.width, bmp.height)).order(ByteOrder.nativeOrder())
        ETC1.encodeImage(uncompressedBuffer, bmp.width, bmp.height, 2, 2 * bmp.width,
                compressedBuffer)
        var mByteBuffers = arrayOf(compressedBuffer)
        return mByteBuffers
    }*/

    private fun getOutputImage(): Bitmap {
        output?.rewind() // Rewind the output buffer after running.

        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(300 * 300) // Set your expected output's height and width
        for (i in 0 until 300 * 300) {
            val a = 0xFF
            val r: Float = output?.float!! * 255.0f
            val g: Float = output?.float!! * 255.0f
            val b: Float = output?.float!! * 255.0f
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }

        bitmap.setPixels(pixels, 0, 300, 0, 0, 300, 300)

        return bitmap
    }

    fun getMax(arr: FloatArray): Int{

        var ind = -1
        var min = 0.0f

        for (i in 0..1) {
            Toast.makeText(this, "val: ${arr[i]}", Toast.LENGTH_SHORT).show()

            if (arr[i] > min && arr[i]>0.9) { // can be changed

                ind = i
                min = arr[i]

            }

        }

        return ind
    }

    override fun onBackPressed() {
        super.onBackPressed()
        tvResult.text = ""
        ivImg.setImageBitmap(null)
    }

    class uploadAsync(var context: Context, var date: String, var list: MutableList<String>): AsyncTask<Void, Void, Boolean>(){

        var customProgressBar = CustomProgressBar()
        var imageRef = FirebaseStorage.getInstance().reference.child("Images")
        var check = false
        var dUrl =""

        override fun onPreExecute() {
            super.onPreExecute()
            customProgressBar.show(context, context.getString(R.string.please_wait))
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun doInBackground(vararg params: Void?): Boolean {

            for (l in list) {
                val file = File(context.getExternalFilesDir(l).toString()).listFiles()

                if (file.isNotEmpty()) {

                    check = true
                    for (f in file) {

                        var listUri = Uri.fromFile(f)

                        var i = context.contentResolver.openInputStream(listUri)

                        var s = UUID.randomUUID().toString()
                        imageRef.child(s).putFile(listUri).addOnSuccessListener {
                            f.delete()
                            val task = it.metadata!!.reference!!.downloadUrl
                            task.addOnSuccessListener {
                                dUrl = it.toString()
                                var ref = FirebaseDatabase.getInstance().reference.child("Data")
                                        .child(l.toUpperCase())
                                var hashMap: HashMap<String, String> = HashMap<String, String>()

                                hashMap.put("Date", date)
                                hashMap.put("Type", l)
                                hashMap.put("Image URL", dUrl)

                                ref.push().setValue(hashMap).addOnSuccessListener {

                                }.addOnFailureListener {
                                    Toast.makeText(context, "${context.getString(R.string.error_toast)}" + it.message, Toast.LENGTH_SHORT).show()
                                }
                            }


                        }.addOnFailureListener {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }


                    }
                }
            }


            return check
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            if(check)
                Toast.makeText(context, context.getString(R.string.uploaded_toast), Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, context.getString(R.string.no_image_found_in_storage), Toast.LENGTH_SHORT).show()

            customProgressBar.dialog.dismiss()
        }
    }


    fun getNumberOfFiles(path: File): Int {
        var numberOfFiles = 0
        if (path.exists()) {
            val files = path.listFiles() ?: return numberOfFiles
            return files.size
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInDevice(ph: File) {



        if(File(getExternalFilesDir(name!!.toUpperCase()).toString()).exists()) {
            count = getExternalFilesDir(name!!.toUpperCase())?.let { getNumberOfFiles(it) }!!
        }
        count++
        myExternalFile = File(getExternalFilesDir(name!!.toUpperCase()), "${name!!.toLowerCase()}${count}.jpg")



        val fileOutPutStream = FileOutputStream(myExternalFile)

        try {
            val bitmap = BitmapFactory.decodeFile(ph.absolutePath)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)

            fileOutPutStream.close()
            ph.delete()


            Toast.makeText(this, getString(R.string.saved_device_toast), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "${applicationContext.getString(R.string.could_not_save_toast)} " + e.message, Toast.LENGTH_SHORT).show()

        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.menuReset){
            // Toast.makeText(context, "reset clicked", Toast.LENGTH_SHORT).show()
            /*val intent = Intent(activity, LangSelActivity::class.java)
            startActivity(intent)*/

            val customDialog = Dialog(this)

            customDialog.setContentView(R.layout.dialog_custom)
            customDialog.customBtnHin.setOnClickListener {
                changeLang("hi", this)
                saveLanguage("hin")
                customDialog.dismiss()
                onStart()
                val i = Intent(this, MainActivity::class.java)
                this!!.overridePendingTransition(0, 0)
                startActivity(i)
                this!!.overridePendingTransition(0, 0)
                this!!.finish()
            }

            customDialog.customBtnEng.setOnClickListener {
                changeLang("en", this)
                saveLanguage("eng")
                customDialog.dismiss()
                onStart()
                val i = Intent(this, MainActivity::class.java)
                this!!.overridePendingTransition(0, 0)
                startActivity(i)
                this!!.overridePendingTransition(0, 0)
                this!!.finish()
            }

            customDialog.show()

        }
        return true
    }

    fun changeLang(str: String, context: Context){

        val locale = Locale(str)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.locale = locale
        context.createConfigurationContext(configuration)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)

    }

    fun saveLanguage(type: String?) {
        val editor = getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString("myLanguage", type)
        editor.apply()
        editor.commit()
    }

    fun setLayout(str: String){
        if(str == "eng"){
            changeLang("en", this)

        }
        else{
            changeLang("hi", this)
        }

        ibUpload.text = getString(R.string.upload_btn)
        ibSave.text = getString(R.string.save_offline_btn)
        tvResult.text = getString(R.string.health_status_text)


    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if(item!!.itemId == R.id.menuReset){
            // Toast.makeText(context, "reset clicked", Toast.LENGTH_SHORT).show()
            /*val intent = Intent(activity, LangSelActivity::class.java)
            startActivity(intent)*/

            val customDialog = Dialog(this)

            customDialog.setContentView(R.layout.dialog_custom)
            customDialog.customBtnHin.setOnClickListener {
                changeLang("hi", this)
                saveLanguage("hin")
                customDialog.dismiss()
                onStart()
                val i = Intent(this, MainActivity::class.java)
                this!!.overridePendingTransition(0, 0)
                startActivity(i)
                this!!.overridePendingTransition(0, 0)
                this!!.finish()
            }

            customDialog.customBtnEng.setOnClickListener {
                changeLang("en", this)
                saveLanguage("eng")
                customDialog.dismiss()
                onStart()
                val i = Intent(this, MainActivity::class.java)
                this!!.overridePendingTransition(0, 0)
                startActivity(i)
                this!!.overridePendingTransition(0, 0)
                this!!.finish()
            }

            customDialog.show()

        }
        return true
    }

    /*override fun onStart() {
        super.onStart()
        if(isPermissionGranted())
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        else{
            takePermission()
        }
    }*/

}