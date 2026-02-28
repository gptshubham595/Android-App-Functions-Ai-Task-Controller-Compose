package com.shubham.todojetpackcompose.di

import android.content.Context
import androidx.room.Room
import com.shubham.todojetpackcompose.common.TODO_DATABASE_NAME
import com.shubham.todojetpackcompose.data.database.TodoRoomDatabase
import com.shubham.todojetpackcompose.data.repo.TodoDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTodoDatabase(@ApplicationContext context: Context): TodoRoomDatabase =
        Room.databaseBuilder(context, TodoRoomDatabase::class.java, TODO_DATABASE_NAME)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideTodoDao(db: TodoRoomDatabase): TodoDataSource =
        db.getTodoDao()  // TodoRoomDao must implement TodoDataSource
}

