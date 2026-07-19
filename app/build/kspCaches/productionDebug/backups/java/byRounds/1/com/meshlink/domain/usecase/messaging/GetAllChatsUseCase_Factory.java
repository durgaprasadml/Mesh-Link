package com.meshlink.domain.usecase.messaging;

import com.meshlink.domain.repository.ChatRepository;
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
public final class GetAllChatsUseCase_Factory implements Factory<GetAllChatsUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetAllChatsUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetAllChatsUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetAllChatsUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new GetAllChatsUseCase_Factory(chatRepositoryProvider);
  }

  public static GetAllChatsUseCase newInstance(ChatRepository chatRepository) {
    return new GetAllChatsUseCase(chatRepository);
  }
}
