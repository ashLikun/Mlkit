package com.king.mlkit.vision.barcode.utils

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * @author　　: 李坤
 * 创建时间: 2022/5/3 13:38
 * 邮箱　　：496546144@qq.com
 *
 * 功能介绍：
 */


object Utils {


    fun readFile(fileName: String?): ByteArray? {
        var len: Int
        val stream: FileInputStream
        var stream2: ByteArrayOutputStream? = null
        try {
            stream = FileInputStream(fileName)
            stream2 = ByteArrayOutputStream()
            val buffer = ByteArray(5)
            //先读后写,循环读写
            while (stream.read(buffer).also { len = it } != -1) {
                stream2.write(buffer, 0, len)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stream2?.toByteArray()
    }


    fun checkPermissionRW(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            return true
        }
    }

    /***
     * path转Uri兼容Android10
     */
    fun getMediaUriFromPath(context: Context, path: String): Uri {
        val mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = context.contentResolver.query(
            mediaUri,
            null,
            MediaStore.Images.Media.DISPLAY_NAME + "= ?", arrayOf(path.substring(path.lastIndexOf("/") + 1)),
            null
        )
        var uri: Uri? = null
        cursor?.let {
            it.moveToFirst()
            val index = it.getColumnIndex(MediaStore.Images.Media._ID)
            if (index >= 0) {
                uri = ContentUris.withAppendedId(mediaUri, it.getLong(index))
            }
        }
        cursor?.close()
        return uri ?: Uri.EMPTY
    }

    fun requstPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context as? Activity)?.requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 200
            )
        }
    }

    fun checkPermissionCamera(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

}