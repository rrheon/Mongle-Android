package com.mongle.android.di

import com.mongle.android.data.remote.ApiAnswerRepository
import com.mongle.android.data.remote.ApiAuthRepository
import com.mongle.android.data.remote.ApiFamilyRepository
import com.mongle.android.data.remote.ApiNudgeRepository
import com.mongle.android.data.remote.ApiQuestionRepository
import com.mongle.android.data.remote.ApiTreeRepository
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.domain.repository.AnswerRepository
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.NudgeRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import com.mongle.android.domain.repository.UserRepository
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
    abstract fun bindAuthRepository(impl: ApiAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMongleRepository(impl: ApiFamilyRepository): MongleRepository

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: ApiQuestionRepository): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindAnswerRepository(impl: ApiAnswerRepository): AnswerRepository

    @Binds
    @Singleton
    abstract fun bindTreeRepository(impl: ApiTreeRepository): TreeRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: ApiUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindNudgeRepository(impl: ApiNudgeRepository): NudgeRepository
}
