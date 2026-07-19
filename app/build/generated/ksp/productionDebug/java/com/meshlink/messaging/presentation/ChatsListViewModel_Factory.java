package com.meshlink.messaging.presentation;

import com.meshlink.domain.usecase.messaging.GetAllChatsUseCase;
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
public final class ChatsListViewModel_Factory implements Factory<ChatsListViewModel> {
  private final Provider<GetAllChatsUseCase> getAllChatsUseCaseProvider;

  public ChatsListViewModel_Factory(Provider<GetAllChatsUseCase> getAllChatsUseCaseProvider) {
    this.getAllChatsUseCaseProvider = getAllChatsUseCaseProvider;
  }

  @Override
  public ChatsListViewModel get() {
    return newInstance(getAllChatsUseCaseProvider.get());
  }

  public static ChatsListViewModel_Factory create(
      Provider<GetAllChatsUseCase> getAllChatsUseCaseProvider) {
    return new ChatsListViewModel_Factory(getAllChatsUseCaseProvider);
  }

  public static ChatsListViewModel newInstance(GetAllChatsUseCase getAllChatsUseCase) {
    return new ChatsListViewModel(getAllChatsUseCase);
  }
}
