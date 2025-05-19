package org.apache.seata.server.raft;

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SessionCheckSizeTest {

    @Mock
    private Configuration mockConfig;

    @Test
    public void testCheckSizeNotCalledInRaftMode() {
        try (MockedStatic<ConfigurationFactory> mockedConfigFactory = Mockito.mockStatic(ConfigurationFactory.class);
             MockedStatic<RaftServerManager> mockedRaftManager = Mockito.mockStatic(RaftServerManager.class)) {

            mockedConfigFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            mockedRaftManager.when(RaftServerManager::isRaftMode).thenReturn(true);

            BranchSession branchSession = new BranchSession(BranchType.AT);
            StringBuilder largeString = new StringBuilder();
            for (int i = 0; i < 1000000; i++) {
                largeString.append("test");
            }
            branchSession.setResourceId(largeString.toString());
            branchSession.setLockKey(largeString.toString());

            byte[] encoded = branchSession.encode();
            assertNotNull(encoded);

            GlobalSession globalSession = new GlobalSession(
                largeString.toString(),
                largeString.toString(),
                "testTransaction",
                60000
            );

            encoded = globalSession.encode();
            assertNotNull(encoded);
        }
    }

    @Test
    public void testCheckSizeCalledInNonRaftMode() {
        try (MockedStatic<ConfigurationFactory> mockedConfigFactory = Mockito.mockStatic(ConfigurationFactory.class);
             MockedStatic<RaftServerManager> mockedRaftManager = Mockito.mockStatic(RaftServerManager.class)) {

            mockedConfigFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            mockedRaftManager.when(RaftServerManager::isRaftMode).thenReturn(false);

            BranchSession branchSession = new BranchSession(BranchType.AT);
            StringBuilder largeString = new StringBuilder();
            for (int i = 0; i < 1000000; i++) {
                largeString.append("test");
            }
            branchSession.setResourceId(largeString.toString());

            RuntimeException branchException = assertThrows(RuntimeException.class, branchSession::encode);
            assertTrue(branchException.getMessage().contains("branch session size exceeded"));

            GlobalSession globalSession = new GlobalSession(
                largeString.toString(),
                largeString.toString(),
                "testTransaction",
                60000
            );

            RuntimeException globalException = assertThrows(RuntimeException.class, globalSession::encode);
            assertTrue(globalException.getMessage().contains("global session size exceeded"));
        }
    }
}
