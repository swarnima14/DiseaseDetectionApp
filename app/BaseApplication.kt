class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MediaResizerGlobal.initializeApplication(this)
    }
}