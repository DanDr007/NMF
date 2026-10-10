package com.example.nmf.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [BlockedSite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedSiteDao(): BlockedSiteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_filter_db",
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).blockedSiteDao()
                                val initialDomains = listOf(

                                    // Sitios de video para adultos más transitados
                                    "pornhub.com",
                                    "xvideos.com",
                                    "xnxx.com",
                                    "xhamster.com",
                                    "redtube.com",
                                    "youporn.com",
                                    "spankbang.com",
                                    "eporner.com",
                                    "tube8.com",
                                    "daftsex.com",
                                    "tnaflix.com",

                                    // Plataformas de suscripción y creadores
                                    "onlyfans.com",
                                    "fansly.com",

                                    // Cams y streaming
                                    "chaturbate.com",
                                    "stripchat.com",
                                    "livejasmin.com",
                                    "bongacams.com",
                                    "camsoda.com",

                                    // Comunidades e imágenes
                                    "rule34.xxx",
                                    "danbooru.donmai.us",
                                    "gelbooru.com",
                                    "toonx.net",
                                    "chochox.com",
                                    "allporncomics.com"
                                )
                                initialDomains.forEach { domain ->
                                    dao.insertSite(BlockedSite(domain = domain))
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}