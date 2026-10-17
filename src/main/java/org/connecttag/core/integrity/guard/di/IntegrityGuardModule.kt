package org.connecttag.core.integrity.guard.di

import org.connecttag.core.integrity.guard.IIntegrityGuard
import org.connecttag.core.integrity.guard.IntegrityGuardImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public abstract class IntegrityGuardModule {

    @Binds
    @Singleton
    internal abstract fun bindIntegrityGuard(
        impl: IntegrityGuardImpl
    ): IIntegrityGuard
}
