package ru.jollypanda.robocv

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_test.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc




class TestActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val REQUEST_APP_SETTINGS = 100
    private val TRASH_LEVEL = 50.0
    private val MAX_TRASH_LEVEL = 255.0
    private val PERMISSION_KEY = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        jcv.setCvCameraViewListener(this)
    }

    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA), PERMISSION_KEY)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_KEY) {
            for (i in 0..permissions.size - 1) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    AlertDialog.Builder(this)
                            .setTitle(getString(R.string.permis_alert_title))
                            .setMessage(getString(R.string.permis_alert_text))
                            .setPositiveButton(getString(R.string.settings), { dialog, which ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivityForResult(intent, REQUEST_APP_SETTINGS)
                            })
                            .setNegativeButton(getString(R.string.cancel), { dialogInterface, i ->
                                dialogInterface.dismiss()
                                this.onBackPressed()
                            })
                            .create()
                            .show()
                }
            }
        }
    }

    fun setCameraDisplayOrientation(activity: Activity,
                                    cameraId: Int, camera: android.hardware.Camera) {
        val info = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay
                .rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }


    override fun onResume() {
        super.onResume()
        OpenCVLoader.initDebug()
        jcv?.enableView()
    }

    override fun onPause() {
        jcv?.disableView()
        super.onPause()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
//        return detectedEdgesMat(inputFrame)
//        return detectedBounds(inputFrame)
        return detectedLines(inputFrame)
    }

    private fun detectedLines(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame == null) return Mat()
        val canny = Mat()
        var ret = Mat()
        inputFrame.rgba().copyTo(ret)
        val src = inputFrame.rgba()

        if (Build.MANUFACTURER == "LGE" && Build.MODEL == "Nexus 5X")
            Core.flip(ret, ret, -1)

        ret.copyTo(src)

        Imgproc.Canny(src, canny, 50.0 , 255.0)
//        Imgproc.threshold(canny, src, TRASH_LEVEL, MAX_TRASH_LEVEL, Imgproc.THRESH_TOZERO)
        val lines = Mat()
        Imgproc.HoughLinesP(canny, lines, 80.0, Math.PI / 180, 128, 10.0, 50.0)

        for (i in 0..lines.cols() - 1) {
            val vec = lines.get(0, i)
            val x1 = vec[0]
            val y1 = vec[1]
            val x2 = vec[2]
            val y2 = vec[3]
            val start = Point(x1, y1)
            val end = Point(x2, y2)

            Imgproc.line(ret, start, end, Scalar(255.0, 0.0, 0.0), 3)
        }
        return ret
    }

    private fun detectedBounds(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame == null) return Mat()
        val src = inputFrame.gray()

        if (Build.MANUFACTURER == "LGE" && Build.MODEL == "Nexus 5X")
            Core.flip(src, src, -1)

        val thresholdOutput = Mat()
        val blurOut = Mat()
        val contours = mutableListOf(MatOfPoint())
        val hierarchy = Mat()

        Imgproc.blur(src, blurOut, Size(3.0, 3.0))
        Imgproc.threshold(blurOut, thresholdOutput, TRASH_LEVEL, MAX_TRASH_LEVEL, Imgproc.THRESH_BINARY)
        Imgproc.findContours(thresholdOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, Point(0.0, 0.0))
        hierarchy.release()
        Imgproc.drawContours(thresholdOutput, contours, -1, Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255))
        return thresholdOutput
    }

    private fun detectedEdgesMat(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val detectedEdges = Mat()
        val src = inputFrame?.gray()
        Imgproc.blur(src, detectedEdges, Size(3.0, 3.0))
        Imgproc.Canny(detectedEdges, detectedEdges, 10.0, 10.0 * 3, 3, false)
        val dst = Mat.zeros(src!!.size(), src.type())
        src.copyTo(dst, detectedEdges)
        return dst
    }
}
