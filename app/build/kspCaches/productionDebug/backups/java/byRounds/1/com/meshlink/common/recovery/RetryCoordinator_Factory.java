package com.meshlink.common.recovery;

import android.content.Context;
import com.meshlink.database.data.local.ChatDao;
import com.meshlink.database.data.local.RelayDao;
import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.routing.data.MeshRouter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RetryCoordinator_Factory implements Factory<RetryCoordinator> {
  private final Provider<Context> contextProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<RelayDao> relayDaoProvider;

  public RetryCoordinator_Factory(Provider<Context> contextProvider,
      Provider<MeshRepository> meshRepositoryProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<RelayDao> relayDaoProvider) {
    this.contextProvider = contextProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.meshRouterProvider = meshRouterProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.relayDaoProvider = relayDaoProvider;
  }

  @Override
  public RetryCoordinator get() {
    return newInstance(contextProvider.get(), meshRepositoryProvider.get(), meshRouterProvider.get(), chatDaoProvider.get(), relayDaoProvider.get());
  }

  public static RetryCoordinator_Factory create(Provider<Context> contextProvider,
      Provider<MeshRepository> meshRepositoryProvider, Provider<MeshRouter> meshRouterProvider,
      Provider<ChatDao> chatDaoProvider, Provider<RelayDao> relayDaoProvider) {
    return new RetryCoordinator_Factory(contextProvider, meshRepositoryProvider, meshRouterProvider, chatDaoProvider, relayDaoProvider);
  }

  public static RetryCoordinator newInstance(Context context, MeshRepository meshRepository,
      MeshRouter meshRouter, ChatDao chatDao, RelayDao relayDao) {
    return new RetryCoordinator(context, meshRepository, meshRouter, chatDao, relayDao);
  }
}
