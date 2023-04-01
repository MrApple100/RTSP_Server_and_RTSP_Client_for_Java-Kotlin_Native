package mrapple100.WaysDetect.Vuforia

open class VuforiaWay() {

    companion object{

        init {
            try {
                System.loadLibrary("VuforiaSDK\\build\\bin\\x64\\VuforiaEngine");
            }catch (e: Exception){

            }
        }

    }
    external fun initEngine():Int;
    external fun shutdownEngine():Int;

}