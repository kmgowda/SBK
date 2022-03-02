package io.sbk.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.40.0)",
    comments = "Source: sbk.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ServiceGrpc {

  private ServiceGrpc() {}

  public static final String SERVICE_NAME = "Service";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.sbk.grpc.Config> getGetConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getConfig",
      requestType = com.google.protobuf.Empty.class,
      responseType = io.sbk.grpc.Config.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.sbk.grpc.Config> getGetConfigMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, io.sbk.grpc.Config> getGetConfigMethod;
    if ((getGetConfigMethod = ServiceGrpc.getGetConfigMethod) == null) {
      synchronized (ServiceGrpc.class) {
        if ((getGetConfigMethod = ServiceGrpc.getGetConfigMethod) == null) {
          ServiceGrpc.getGetConfigMethod = getGetConfigMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, io.sbk.grpc.Config>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.sbk.grpc.Config.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceMethodDescriptorSupplier("getConfig"))
              .build();
        }
      }
    }
    return getGetConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.sbk.grpc.Config,
      io.sbk.grpc.ClientID> getRegisterClientMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "registerClient",
      requestType = io.sbk.grpc.Config.class,
      responseType = io.sbk.grpc.ClientID.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.sbk.grpc.Config,
      io.sbk.grpc.ClientID> getRegisterClientMethod() {
    io.grpc.MethodDescriptor<io.sbk.grpc.Config, io.sbk.grpc.ClientID> getRegisterClientMethod;
    if ((getRegisterClientMethod = ServiceGrpc.getRegisterClientMethod) == null) {
      synchronized (ServiceGrpc.class) {
        if ((getRegisterClientMethod = ServiceGrpc.getRegisterClientMethod) == null) {
          ServiceGrpc.getRegisterClientMethod = getRegisterClientMethod =
              io.grpc.MethodDescriptor.<io.sbk.grpc.Config, io.sbk.grpc.ClientID>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "registerClient"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.sbk.grpc.Config.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.sbk.grpc.ClientID.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceMethodDescriptorSupplier("registerClient"))
              .build();
        }
      }
    }
    return getRegisterClientMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.sbk.grpc.LatenciesRecord,
      com.google.protobuf.Empty> getAddLatenciesRecordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "addLatenciesRecord",
      requestType = io.sbk.grpc.LatenciesRecord.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.sbk.grpc.LatenciesRecord,
      com.google.protobuf.Empty> getAddLatenciesRecordMethod() {
    io.grpc.MethodDescriptor<io.sbk.grpc.LatenciesRecord, com.google.protobuf.Empty> getAddLatenciesRecordMethod;
    if ((getAddLatenciesRecordMethod = ServiceGrpc.getAddLatenciesRecordMethod) == null) {
      synchronized (ServiceGrpc.class) {
        if ((getAddLatenciesRecordMethod = ServiceGrpc.getAddLatenciesRecordMethod) == null) {
          ServiceGrpc.getAddLatenciesRecordMethod = getAddLatenciesRecordMethod =
              io.grpc.MethodDescriptor.<io.sbk.grpc.LatenciesRecord, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "addLatenciesRecord"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.sbk.grpc.LatenciesRecord.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceMethodDescriptorSupplier("addLatenciesRecord"))
              .build();
        }
      }
    }
    return getAddLatenciesRecordMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.sbk.grpc.ClientID,
      com.google.protobuf.Empty> getCloseClientMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "closeClient",
      requestType = io.sbk.grpc.ClientID.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.sbk.grpc.ClientID,
      com.google.protobuf.Empty> getCloseClientMethod() {
    io.grpc.MethodDescriptor<io.sbk.grpc.ClientID, com.google.protobuf.Empty> getCloseClientMethod;
    if ((getCloseClientMethod = ServiceGrpc.getCloseClientMethod) == null) {
      synchronized (ServiceGrpc.class) {
        if ((getCloseClientMethod = ServiceGrpc.getCloseClientMethod) == null) {
          ServiceGrpc.getCloseClientMethod = getCloseClientMethod =
              io.grpc.MethodDescriptor.<io.sbk.grpc.ClientID, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "closeClient"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.sbk.grpc.ClientID.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ServiceMethodDescriptorSupplier("closeClient"))
              .build();
        }
      }
    }
    return getCloseClientMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceStub>() {
        @java.lang.Override
        public ServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceStub(channel, callOptions);
        }
      };
    return ServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceBlockingStub>() {
        @java.lang.Override
        public ServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceBlockingStub(channel, callOptions);
        }
      };
    return ServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ServiceFutureStub>() {
        @java.lang.Override
        public ServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ServiceFutureStub(channel, callOptions);
        }
      };
    return ServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void getConfig(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.sbk.grpc.Config> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetConfigMethod(), responseObserver);
    }

    /**
     */
    public void registerClient(io.sbk.grpc.Config request,
        io.grpc.stub.StreamObserver<io.sbk.grpc.ClientID> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterClientMethod(), responseObserver);
    }

    /**
     */
    public void addLatenciesRecord(io.sbk.grpc.LatenciesRecord request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddLatenciesRecordMethod(), responseObserver);
    }

    /**
     */
    public void closeClient(io.sbk.grpc.ClientID request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCloseClientMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                io.sbk.grpc.Config>(
                  this, METHODID_GET_CONFIG)))
          .addMethod(
            getRegisterClientMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.sbk.grpc.Config,
                io.sbk.grpc.ClientID>(
                  this, METHODID_REGISTER_CLIENT)))
          .addMethod(
            getAddLatenciesRecordMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.sbk.grpc.LatenciesRecord,
                com.google.protobuf.Empty>(
                  this, METHODID_ADD_LATENCIES_RECORD)))
          .addMethod(
            getCloseClientMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.sbk.grpc.ClientID,
                com.google.protobuf.Empty>(
                  this, METHODID_CLOSE_CLIENT)))
          .build();
    }
  }

  /**
   */
  public static final class ServiceStub extends io.grpc.stub.AbstractAsyncStub<ServiceStub> {
    private ServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceStub(channel, callOptions);
    }

    /**
     */
    public void getConfig(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.sbk.grpc.Config> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void registerClient(io.sbk.grpc.Config request,
        io.grpc.stub.StreamObserver<io.sbk.grpc.ClientID> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterClientMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addLatenciesRecord(io.sbk.grpc.LatenciesRecord request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddLatenciesRecordMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void closeClient(io.sbk.grpc.ClientID request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCloseClientMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ServiceBlockingStub> {
    private ServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.sbk.grpc.Config getConfig(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetConfigMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.sbk.grpc.ClientID registerClient(io.sbk.grpc.Config request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterClientMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty addLatenciesRecord(io.sbk.grpc.LatenciesRecord request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddLatenciesRecordMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty closeClient(io.sbk.grpc.ClientID request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCloseClientMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ServiceFutureStub> {
    private ServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.sbk.grpc.Config> getConfig(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetConfigMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.sbk.grpc.ClientID> registerClient(
        io.sbk.grpc.Config request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterClientMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> addLatenciesRecord(
        io.sbk.grpc.LatenciesRecord request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddLatenciesRecordMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> closeClient(
        io.sbk.grpc.ClientID request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCloseClientMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_CONFIG = 0;
  private static final int METHODID_REGISTER_CLIENT = 1;
  private static final int METHODID_ADD_LATENCIES_RECORD = 2;
  private static final int METHODID_CLOSE_CLIENT = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_CONFIG:
          serviceImpl.getConfig((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<io.sbk.grpc.Config>) responseObserver);
          break;
        case METHODID_REGISTER_CLIENT:
          serviceImpl.registerClient((io.sbk.grpc.Config) request,
              (io.grpc.stub.StreamObserver<io.sbk.grpc.ClientID>) responseObserver);
          break;
        case METHODID_ADD_LATENCIES_RECORD:
          serviceImpl.addLatenciesRecord((io.sbk.grpc.LatenciesRecord) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_CLOSE_CLIENT:
          serviceImpl.closeClient((io.sbk.grpc.ClientID) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.sbk.grpc.SbkGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Service");
    }
  }

  private static final class ServiceFileDescriptorSupplier
      extends ServiceBaseDescriptorSupplier {
    ServiceFileDescriptorSupplier() {}
  }

  private static final class ServiceMethodDescriptorSupplier
      extends ServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ServiceFileDescriptorSupplier())
              .addMethod(getGetConfigMethod())
              .addMethod(getRegisterClientMethod())
              .addMethod(getAddLatenciesRecordMethod())
              .addMethod(getCloseClientMethod())
              .build();
        }
      }
    }
    return result;
  }
}
