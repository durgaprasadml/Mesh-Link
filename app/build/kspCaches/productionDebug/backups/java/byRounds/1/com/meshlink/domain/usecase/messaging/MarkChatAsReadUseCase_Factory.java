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
public final class MarkChatAsReadUseCase_Factory implements Factory<MarkChatAsReadUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public MarkChatAsReadUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public MarkChatAsReadUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static MarkChatAsReadUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new MarkChatAsReadUseCase_Factory(chatRepositoryProvider);
  }

  public static MarkChatAsReadUseCase newInstance(ChatRepository chatRepository) {
    return new MarkChatAsReadUseCase(chatRepository);
  }
}
