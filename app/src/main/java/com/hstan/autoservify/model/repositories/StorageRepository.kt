package com.hstan.autoservify.model.repositories


import com.hstan.autoservify.DataSource.CloudinaryUploadHelper

class StorageRepository {
    fun uploadFile(filePath:String,onComplete: (Boolean,String?) -> Unit){
        CloudinaryUploadHelper().uploadFile(filePath,onComplete)
    }

}