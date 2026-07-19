package com.meshlink.routing.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class RoutingEngine_Factory implements Factory<RoutingEngine> {
  private final Provider<RouteManager> routeManagerProvider;

  private final Provider<QoSManager> qosManagerProvider;

  private final Provider<CongestionMonitor> congestionMonitorProvider;

  private final Provider<RouteHealthMonitor> routeHealthMonitorProvider;

  private final Provider<NetworkTopologyEngine> topologyEngineProvider;

  private final Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider;

  private final Provider<IntelligentTransportManager> transportManagerProvider;

  private final Provider<IntelligentRetryEngine> retryEngineProvider;

  private final Provider<QueueOptimizer> queueOptimizerProvider;

  private final Provider<RouteOptimizer> routeOptimizerProvider;

  public RoutingEngine_Factory(Provider<RouteManager> routeManagerProvider,
      Provider<QoSManager> qosManagerProvider,
      Provider<CongestionMonitor> congestionMonitorProvider,
      Provider<RouteHealthMonitor> routeHealthMonitorProvider,
      Provider<NetworkTopologyEngine> topologyEngineProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider,
      Provider<IntelligentTransportManager> transportManagerProvider,
      Provider<IntelligentRetryEngine> retryEngineProvider,
      Provider<QueueOptimizer> queueOptimizerProvider,
      Provider<RouteOptimizer> routeOptimizerProvider) {
    this.routeManagerProvider = routeManagerProvider;
    this.qosManagerProvider = qosManagerProvider;
    this.congestionMonitorProvider = congestionMonitorProvider;
    this.routeHealthMonitorProvider = routeHealthMonitorProvider;
    this.topologyEngineProvider = topologyEngineProvider;
    this.batteryAwareNetworkingProvider = batteryAwareNetworkingProvider;
    this.transportManagerProvider = transportManagerProvider;
    this.retryEngineProvider = retryEngineProvider;
    this.queueOptimizerProvider = queueOptimizerProvider;
    this.routeOptimizerProvider = routeOptimizerProvider;
  }

  @Override
  public RoutingEngine get() {
    return newInstance(routeManagerProvider.get(), qosManagerProvider.get(), congestionMonitorProvider.get(), routeHealthMonitorProvider.get(), topologyEngineProvider.get(), batteryAwareNetworkingProvider.get(), transportManagerProvider.get(), retryEngineProvider.get(), queueOptimizerProvider.get(), routeOptimizerProvider.get());
  }

  public static RoutingEngine_Factory create(Provider<RouteManager> routeManagerProvider,
      Provider<QoSManager> qosManagerProvider,
      Provider<CongestionMonitor> congestionMonitorProvider,
      Provider<RouteHealthMonitor> routeHealthMonitorProvider,
      Provider<NetworkTopologyEngine> topologyEngineProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider,
      Provider<IntelligentTransportManager> transportManagerProvider,
      Provider<IntelligentRetryEngine> retryEngineProvider,
      Provider<QueueOptimizer> queueOptimizerProvider,
      Provider<RouteOptimizer> routeOptimizerProvider) {
    return new RoutingEngine_Factory(routeManagerProvider, qosManagerProvider, congestionMonitorProvider, routeHealthMonitorProvider, topologyEngineProvider, batteryAwareNetworkingProvider, transportManagerProvider, retryEngineProvider, queueOptimizerProvider, routeOptimizerProvider);
  }

  public static RoutingEngine newInstance(RouteManager routeManager, QoSManager qosManager,
      CongestionMonitor congestionMonitor, RouteHealthMonitor routeHealthMonitor,
      NetworkTopologyEngine topologyEngine, BatteryAwareNetworking batteryAwareNetworking,
      IntelligentTransportManager transportManager, IntelligentRetryEngine retryEngine,
      QueueOptimizer queueOptimizer, RouteOptimizer routeOptimizer) {
    return new RoutingEngine(routeManager, qosManager, congestionMonitor, routeHealthMonitor, topologyEngine, batteryAwareNetworking, transportManager, retryEngine, queueOptimizer, routeOptimizer);
  }
}
