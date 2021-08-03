package com.app.potatodiseasedetection


import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.potatodiseasedetection.ml.Model
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_custom.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(Color.parseColor("#141414"))
        toolbar.title=null
        toolbar.setTitleTextColor(Color.parseColor("#141414"))

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

            layAnim.visibility = View.GONE

            //function to check for the permission before opening gallery
                if(isPermissionGranted())
                openGallery()//if function returns true gallery is opened
            else{
                takePermission()
                }



        }

        btnCapture.setOnClickListener(View.OnClickListener {
            btnCam = true
            btnGal = false

            layAnim.visibility = View.GONE

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
                if(name == "Invalid") {
                    Toast.makeText(this, getString(R.string.invalid), Toast.LENGTH_SHORT).show()

                }

                else if(photoFile != null && name != null && name != getString(R.string.invalid))
                    saveImageInDevice(photoFile)

                else
                    Toast.makeText(this, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()
            }

            ivImg.setImageResource(R.drawable.noimage2)
            tvResult.text = ""
        }

        ibUpload.setOnClickListener {
            layAnim.visibility = View.GONE//potato animation removed
            //to upload all the images at once async task is used
            uploadAsync(this, date, list).execute()
            tvResult.text = ""
            ivImg.setImageResource(R.drawable.noimage2)

        }
    }



    private fun openGallery() {

            pressGal = false //set false at this point and make it true in onActivityResult so that if user doesn't click image this is not unnecessarily true

        //if there is any image or text set it null
            ivImg.setImageResource(R.drawable.noimage2)
            tvResult.text = ""

        //intent call to get content that is image, type is set image
            var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, 97)

    }

    private fun takePermission() {

        //if the permission is not granted a dialog occurs asking for the required permission
        //its result is executed in onRequestPermissionResult function
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), 2)
        
    }

    private fun isPermissionGranted(): Boolean {

        //checks for all the 3 permissions required
            val readExternalStoragePermission = ContextCompat.checkSelfPermission(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())
            return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED //returns true if the permissions are already granted

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //checks if the grantsResults array is empty or not and the request code matches or not
        if(grantResults.size > 0 && requestCode ==2){
            val readExtStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED // if the permission is granted it returns true else false
            if(readExtStorage){

                //if statements, since permission is to be checked before opening gallery and camera therefore check which button was clicked and then open the required thing

                if(btnCam)
                    openCamera()
                if(btnGal)
                    openGallery()
            }
            else{
                //if users denies then again display the dialog to test permission
                takePermission()
            }
        }

    }

    fun openCamera()
    {
        pressCam = false // this is set false so that if user doesn't click any image and returns it is not set true unnecessarily

        ivImg.setImageResource(R.drawable.noimage2)
        tvResult.text = ""

        var camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getPhotoFile(FILE_NAME) // FILE_NAME is a string with value "pic" is passed into a function returns a temporary file with the name pic
        //we are using filename to access the image so that the quality of image remains same

        val editor = getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE).edit() //the file name is also saved in shared preferences
        editor.putString("myFile", photoFile.toString())
        editor.apply()
        editor.commit()


        //file provider facilitates secure sharing of files associated with an app by creating content uri
        //before doing this create fileprovider.xml in xml in res and see changes in manifest
        fileProvider = FileProvider.getUriForFile(this, "com.app.potatodiseasedetection.fileprovider", photoFile!!)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider) //this means that the output of the camera intent i.e., the image is stored in the fileprovider

        //pfile = photoFile

        startActivityForResult(camIntent, 99)

    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

         /*if(resultCode == Activity.RESULT_OK){
             if(requestCode == 1){
                 if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
                     if(Environment.isExternalStorageManager()){
                         Toast.makeText(this, "Permissions Granted.", Toast.LENGTH_SHORT).show()
                     }
                 }
             }
         }*/

         if(requestCode == 99 && resultCode == Activity.RESULT_OK) {

             pressCam = true //since camera is giving the image therefore it is set true
             // and will be used to see which file or uri is to be used further while storing image in device

             //setlayout function so that when app returns to the activity the lang remains same
             setLayout(getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).getString("myLanguage", "eng").toString())

             //retrieves photofile from shared preferences
             val editor = getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
             photoFile = File(editor.getString("myFile", "").toString())

             //set bitmap from path of file
             bitmap = BitmapFactory.decodeFile(photoFile.path)
             ivImg.setImageBitmap(bitmap)

             //since the image size is quite large we use media resizer library to compress it without loosing its quality
             //first create a class called BaseApplication and declare its name in application in manifest

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
                     .setTargetPath(photoFile.absolutePath) //the target location and the output path are same so that image gets replaced with with its compressed version
                     .setOutputPath(photoFile.absolutePath)
                     .build()

             MediaResizer.process(option)

            //uri is extracted from file
             uri = Uri.fromFile(photoFile)
             bitmap = BitmapFactory.decodeFile(photoFile.path)

             // call for predict func
             predictName()

         }

         if(resultCode == RESULT_OK && requestCode == 97 && data!=null) {

             pressGal = true

             ivImg.setImageURI(data!!.data) //this is the data returned from the intent, we don't use this in camera intent because image quality gets poor
             uri = data!!.data

             val s = FileUtil.getPath(uri!!, this) //to convert the uri of gallery image in file, output is a string
             photoFile = File(s)
             //media resizer not used as image size of gallery images is not that large

             /*val resizeOption = ImageResizeOption.Builder()
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

             MediaResizer.process(option)*/

             predictName()

         }

    }

    private  fun predictName(){

            val a = FileUtil.getPath(uri!!, applicationContext) //fetch the file name from given uri

            bitmap = BitmapFactory.decodeFile(File(a.toString()).absolutePath) //fetch bitmap from given file
            fileName = if(currentLang == "hin"){ //if the app lang is hindi then file name for displaying output is different
                "classHindi.txt"
            }

            else {
                "plant_labels.txt"
            }
            var max = 0
            val inpString = application.assets.open(fileName).bufferedReader().use { it.readText() } //opens the file fileName and read text and convert it into string
            val cropList = inpString.split("\n") //split string at new line to get array of strings

            var resized: Bitmap = Bitmap.createScaledBitmap(bitmap, 190, 190, true) //resize bitmap according to the image specifications given in model

            val model = Model.newInstance(this@MainActivity) //create instance of model

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 190, 190, 3), DataType.FLOAT32)

        //converts image to byte buffer
            val tensorImage = TensorImage(DataType.FLOAT32) //FLOAT_32 as given in model specification
            tensorImage.load(resized)
            var byteBuffer = tensorImage.buffer

            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        //returns index of maximum probability
            max = getMax(outputFeature0.floatArray)

            if (max == -1 || max == 2) { //2 index belongs to invalid
                tvResult.setTextColor(Color.BLACK)
                tvResult.text = " ${getString(R.string.invalid)}"
                name = "Invalid"

            } else {
                name = cropList[max]
                if(max==0){ //0 to diseased

                    layAnim.visibility = View.VISIBLE
                    potato.setImageResource(R.drawable.potatosad)

                    potato.animation = AnimationUtils.loadAnimation(this, R.anim.shake)
                    tvResult.setTextColor(Color.RED)
                }
                if(max==1){ //1 to healthy

                    layAnim.visibility = View.VISIBLE

                    potato.setImageResource(R.drawable.potatohappy)
                    potato.animation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

                    tvResult.setTextColor(Color.GREEN)
                }
                tvResult.text = "${cropList[max]}"
            }

        //close the model
            model.close()


    }

    fun getMax(arr: FloatArray): Int{

        var ind = -1
        var min = 0.0f
       // Toast.makeText(this, "values: ${arr[0]} ${arr[1]} ${arr[2]}", Toast.LENGTH_LONG).show()
        for (i in 0..2) {


            if (arr[i] > min) {

                ind = i
                min = arr[i]

            }

        }

        return ind
    }

    override fun onBackPressed() {
        super.onBackPressed()

        ivImg.setImageBitmap(null)
    }

    class uploadAsync(var context: Context, var date: String, var list: MutableList<String>): AsyncTask<Void, Void, Boolean>(){

        var customProgressBar = CustomProgressBar()
        var imageRef = FirebaseStorage.getInstance().reference.child("Images") //firebase storage difference
        var check = false
        var dUrl =""

        override fun onPreExecute() {
            super.onPreExecute()
            customProgressBar.show(context, context.getString(R.string.please_wait))
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun doInBackground(vararg params: Void?): Boolean {

            for (l in list) {
                val file = File(context.getExternalFilesDir(l).toString()).listFiles() //get list of files in the folder l

                if (file.isNotEmpty()) { //if there are images in folder then iterate through each of them and upload it

                    check = true
                    for (f in file) {

                        var listUri = Uri.fromFile(f) //uri of file

                       // var i = context.contentResolver.openInputStream(listUri)

                        var s = UUID.randomUUID().toString() // random string for pushing images to storage

                        //upload image to firebase storage
                        imageRef.child(s).putFile(listUri).addOnSuccessListener {
                            f.delete() //delete image from device
                            val task = it.metadata!!.reference!!.downloadUrl //it contains dowload url of image
                            task.addOnSuccessListener {
                                dUrl = it.toString() //convert url to string
                                var ref = FirebaseDatabase.getInstance().reference.child("Data")
                                        .child(l.toUpperCase()) //create reference to realtime database
                                var hashMap: HashMap<String, String> = HashMap<String, String>()

                                hashMap.put("Date", date)
                                hashMap.put("Type", l)
                                hashMap.put("Image URL", dUrl)

                                //store the createdhasmap in realtime database
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


            return check //this check is returned to postExecute and if it is true it indicates that folder was not empty
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            if(check)
                Toast.makeText(context, context.getString(R.string.uploaded_toast), Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, context.getString(R.string.no_image_found_in_storage), Toast.LENGTH_SHORT).show()

            customProgressBar.dialog.dismiss() //dismiss the progressbar

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

        //check if the folder of name exists in external directory or not
        if(File(getExternalFilesDir(name!!.toUpperCase()).toString()).exists()) {
            count = getExternalFilesDir(name!!.toUpperCase())?.let { getNumberOfFiles(it) }!! //if yes then count number of fies in it
        }
        count++
        myExternalFile = File(getExternalFilesDir(name!!.toUpperCase()), "${name!!.toLowerCase()}${count}.jpg") //name for file to be saved



        val fileOutPutStream = FileOutputStream(myExternalFile) //output stream is used to write something to the file

        try {
            val bitmap = BitmapFactory.decodeFile(ph.absolutePath)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)// bitmap is written on the file utput stream

            fileOutPutStream.close() //close the stream
            ph.delete() //delete ph cause file is now permanently saved in its correct storage


            Toast.makeText(this, getString(R.string.saved_device_toast), Toast.LENGTH_SHORT).show()
            layAnim.visibility = View.GONE
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "${applicationContext.getString(R.string.could_not_save_toast)} " + e.message, Toast.LENGTH_SHORT).show()

        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        //to place the lang change option
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.menuReset){

            val customDialog = Dialog(this)

            customDialog.setContentView(R.layout.dialog_custom)
            customDialog.customBtnHin.setOnClickListener {
                changeLang("hi", this)
                saveLanguage("hin")
                customDialog.dismiss()
                //start again the present activity with changed language
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

}