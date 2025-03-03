package org.apache.seata.core.rpc.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NettyClientBootstrapTest {

    @Mock
    private NettyClientConfig nettyClientConfig;
    private DefaultEventExecutorGroup eventExecutorGroup;

    @BeforeEach
    void init() {
        eventExecutorGroup = new DefaultEventExecutorGroup(1);
    }

    @Test
    void testSharedEventLoopGroupEnabled() {
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance();
        RmNettyRemotingClient rmNettyRemotingClient = RmNettyRemotingClient.getInstance();

        tmNettyRemotingClient.init();
        rmNettyRemotingClient.init();

        NettyClientBootstrap tmBootstrap = getNettyClientBootstrap(tmNettyRemotingClient);
        NettyClientBootstrap rmBootstrap = getNettyClientBootstrap(rmNettyRemotingClient);

        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmBootstrap);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmBootstrap);

        Assertions.assertEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    @Test
    void testSharedEventLoopGroupDisabled() {
        when(nettyClientConfig.getEnableClientSharedEventLoop()).thenReturn(false);
        NettyClientBootstrap tmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, eventExecutorGroup, NettyPoolKey.TransactionRole.TMROLE);
        EventLoopGroup tmEventLoopGroupWorker = getEventLoopGroupWorker(tmNettyClientBootstrap);

        NettyClientBootstrap rmNettyClientBootstrap = new NettyClientBootstrap(nettyClientConfig, eventExecutorGroup, NettyPoolKey.TransactionRole.RMROLE);
        EventLoopGroup rmEventLoopGroupWorker = getEventLoopGroupWorker(rmNettyClientBootstrap);

        Assertions.assertNotEquals(tmEventLoopGroupWorker, rmEventLoopGroupWorker);
    }

    private NettyClientBootstrap getNettyClientBootstrap(AbstractNettyRemotingClient remotingClient) {
        try {
            java.lang.reflect.Field field = AbstractNettyRemotingClient.class.getDeclaredField("clientBootstrap");
            field.setAccessible(true);
            return (NettyClientBootstrap) field.get(remotingClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private EventLoopGroup getEventLoopGroupWorker(NettyClientBootstrap bootstrap) {
        try {
            java.lang.reflect.Field field = NettyClientBootstrap.class.getDeclaredField("eventLoopGroupWorker");
            field.setAccessible(true);
            return (EventLoopGroup) field.get(bootstrap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}