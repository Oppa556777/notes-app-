package com.securenotes.app.di

import android.content.Context
import androidx.room.Room
import com.securenotes.app.core.crypto.CryptoManager
import com.securenotes.app.data.link.LinkPreviewRepositoryImpl
import com.securenotes.app.data.local.SecureNotesDatabase
import com.securenotes.app.data.local.dao.LinkPreviewDao
import com.securenotes.app.data.local.dao.NoteDao
import com.securenotes.app.data.local.dao.NotebookDao
import com.securenotes.app.data.local.dao.TagDao
import com.securenotes.app.data.repository.NoteRepositoryImpl
import com.securenotes.app.data.repository.NotebookRepositoryImpl
import com.securenotes.app.data.repository.TagRepositoryImpl
import com.securenotes.app.domain.repository.LinkPreviewRepository
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.NotebookRepository
import com.securenotes.app.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager,
    ): SecureNotesDatabase {
        // SQLCipher: the whole database file is AES-256 encrypted at rest with a
        // key that only exists inside the Android Keystore-backed prefs.
        SQLiteDatabase.loadLibs(context)
        val passphrase = SQLiteDatabase.getBytes(cryptoManager.databasePassphrase())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            SecureNotesDatabase::class.java,
            SecureNotesDatabase.NAME,
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideNoteDao(db: SecureNotesDatabase): NoteDao = db.noteDao()

    @Provides fun provideNotebookDao(db: SecureNotesDatabase): NotebookDao = db.notebookDao()

    @Provides fun provideTagDao(db: SecureNotesDatabase): TagDao = db.tagDao()

    @Provides fun provideLinkPreviewDao(db: SecureNotesDatabase): LinkPreviewDao =
        db.linkPreviewDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindNotebookRepository(impl: NotebookRepositoryImpl): NotebookRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindLinkPreviewRepository(impl: LinkPreviewRepositoryImpl): LinkPreviewRepository
}
