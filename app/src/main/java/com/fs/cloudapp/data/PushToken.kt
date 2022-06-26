package com.fs.cloudapp.data

import com.huawei.agconnect.cloud.database.CloudDBZoneObject

data class PushToken(val token: String,
                     val user_id: String,
                     val platform: Int)
