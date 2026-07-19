package com.meshlink.service.work;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = RetryWorker.class
)
public interface RetryWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.meshlink.service.work.RetryWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(RetryWorker_AssistedFactory factory);
}
