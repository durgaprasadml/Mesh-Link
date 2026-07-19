package com.meshlink.ui.home;

import com.meshlink.database.data.local.ChatDao;
import com.meshlink.domain.repository.MeshRepository;
import com.meshlink.domain.repository.UserRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<MeshRepository> meshRepositoryProvider;

  private final Provider<ChatDao> chatDaoProvider;

  public HomeViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<MeshRepository> meshRepositoryProvider, Provider<ChatDao> chatDaoProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
    this.chatDaoProvider = chatDaoProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(userRepositoryProvider.get(), meshRepositoryProvider.get(), chatDaoProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<MeshRepository> meshRepositoryProvider, Provider<ChatDao> chatDaoProvider) {
    return new HomeViewModel_Factory(userRepositoryProvider, meshRepositoryProvider, chatDaoProvider);
  }

  public static HomeViewModel newInstance(UserRepository userRepository,
      MeshRepository meshRepository, ChatDao chatDao) {
    return new HomeViewModel(userRepository, meshRepository, chatDao);
  }
}
