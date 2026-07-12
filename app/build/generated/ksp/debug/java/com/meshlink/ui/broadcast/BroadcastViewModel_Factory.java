package com.meshlink.ui.broadcast;

import com.meshlink.data.local.ChatDao;
import com.meshlink.data.repository.BleRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class BroadcastViewModel_Factory implements Factory<BroadcastViewModel> {
  private final Provider<BleRepository> bleRepositoryProvider;

  private final Provider<ChatDao> chatDaoProvider;

  public BroadcastViewModel_Factory(Provider<BleRepository> bleRepositoryProvider,
      Provider<ChatDao> chatDaoProvider) {
    this.bleRepositoryProvider = bleRepositoryProvider;
    this.chatDaoProvider = chatDaoProvider;
  }

  @Override
  public BroadcastViewModel get() {
    return newInstance(bleRepositoryProvider.get(), chatDaoProvider.get());
  }

  public static BroadcastViewModel_Factory create(Provider<BleRepository> bleRepositoryProvider,
      Provider<ChatDao> chatDaoProvider) {
    return new BroadcastViewModel_Factory(bleRepositoryProvider, chatDaoProvider);
  }

  public static BroadcastViewModel newInstance(BleRepository bleRepository, ChatDao chatDao) {
    return new BroadcastViewModel(bleRepository, chatDao);
  }
}
