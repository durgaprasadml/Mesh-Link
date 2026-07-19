package com.meshlink.transfer;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.meshlink.di.IoDispatcher"
})
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TransferManager_Factory implements Factory<TransferManager> {
  private final Provider<Context> contextProvider;

  private final Provider<TransferScheduler> schedulerProvider;

  private final Provider<TransferCache> cacheProvider;

  private final Provider<ChunkManager> chunkManagerProvider;

  private final Provider<FileMetadataManager> metaManagerProvider;

  private final Provider<IntegrityVerifier> verifierProvider;

  private final Provider<TransferAnalytics> analyticsProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public TransferManager_Factory(Provider<Context> contextProvider,
      Provider<TransferScheduler> schedulerProvider, Provider<TransferCache> cacheProvider,
      Provider<ChunkManager> chunkManagerProvider,
      Provider<FileMetadataManager> metaManagerProvider,
      Provider<IntegrityVerifier> verifierProvider, Provider<TransferAnalytics> analyticsProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.contextProvider = contextProvider;
    this.schedulerProvider = schedulerProvider;
    this.cacheProvider = cacheProvider;
    this.chunkManagerProvider = chunkManagerProvider;
    this.metaManagerProvider = metaManagerProvider;
    this.verifierProvider = verifierProvider;
    this.analyticsProvider = analyticsProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public TransferManager get() {
    return newInstance(contextProvider.get(), schedulerProvider.get(), cacheProvider.get(), chunkManagerProvider.get(), metaManagerProvider.get(), verifierProvider.get(), analyticsProvider.get(), ioDispatcherProvider.get());
  }

  public static TransferManager_Factory create(Provider<Context> contextProvider,
      Provider<TransferScheduler> schedulerProvider, Provider<TransferCache> cacheProvider,
      Provider<ChunkManager> chunkManagerProvider,
      Provider<FileMetadataManager> metaManagerProvider,
      Provider<IntegrityVerifier> verifierProvider, Provider<TransferAnalytics> analyticsProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new TransferManager_Factory(contextProvider, schedulerProvider, cacheProvider, chunkManagerProvider, metaManagerProvider, verifierProvider, analyticsProvider, ioDispatcherProvider);
  }

  public static TransferManager newInstance(Context context, TransferScheduler scheduler,
      TransferCache cache, ChunkManager chunkManager, FileMetadataManager metaManager,
      IntegrityVerifier verifier, TransferAnalytics analytics, CoroutineDispatcher ioDispatcher) {
    return new TransferManager(context, scheduler, cache, chunkManager, metaManager, verifier, analytics, ioDispatcher);
  }
}
