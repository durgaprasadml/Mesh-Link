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
public final class GetBroadcastMessagesUseCase_Factory implements Factory<GetBroadcastMessagesUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetBroadcastMessagesUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetBroadcastMessagesUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetBroadcastMessagesUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new GetBroadcastMessagesUseCase_Factory(chatRepositoryProvider);
  }

  public static GetBroadcastMessagesUseCase newInstance(ChatRepository chatRepository) {
    return new GetBroadcastMessagesUseCase(chatRepository);
  }
}
