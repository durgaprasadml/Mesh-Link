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
public final class GetChatMessagesUseCase_Factory implements Factory<GetChatMessagesUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetChatMessagesUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetChatMessagesUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetChatMessagesUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new GetChatMessagesUseCase_Factory(chatRepositoryProvider);
  }

  public static GetChatMessagesUseCase newInstance(ChatRepository chatRepository) {
    return new GetChatMessagesUseCase(chatRepository);
  }
}
