package com.mongle.android.di

import com.mongle.android.data.mock.MockAnswerRepository
import com.mongle.android.data.mock.MockAuthRepository
import com.mongle.android.data.mock.MockMongleRepository
import com.mongle.android.data.mock.MockQuestionRepository
import com.mongle.android.data.mock.MockTreeRepository
import com.mongle.android.domain.repository.AnswerRepository
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: MockAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMongleRepository(impl: MockMongleRepository): MongleRepository

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: MockQuestionRepository): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindAnswerRepository(impl: MockAnswerRepository): AnswerRepository

    @Binds
    @Singleton
    abstract fun bindTreeRepository(impl: MockTreeRepository): TreeRepository
}
