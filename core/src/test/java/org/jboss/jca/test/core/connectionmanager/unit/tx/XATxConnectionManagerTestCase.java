/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jca.test.core.connectionmanager.unit.tx;

import org.jboss.jca.core.api.ConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.PoolParams;
import org.jboss.jca.core.connectionmanager.pool.strategy.OnePool;
import org.jboss.jca.core.connectionmanager.transaction.TransactionSynchronizer;
import org.jboss.jca.core.connectionmanager.tx.TxConnectionManager;
import org.jboss.jca.embedded.EmbeddedJCA;
import org.jboss.jca.test.core.connectionmanager.common.MockConnectionRequestInfo;
import org.jboss.jca.test.core.connectionmanager.common.MockHandle;
import org.jboss.jca.test.core.connectionmanager.common.MockManagedConnectionFactory;
import org.jboss.jca.test.core.workmanager.unit.WorkManagerTestCase;

import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.TransactionManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * XATxConnectionManagerTestCase.
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a> 
 * @version $Rev$ $Date$
 *
 */
public class XATxConnectionManagerTestCase
{
   /**Embedded JCA*/
   private static EmbeddedJCA embedded = null;
   
   /**
    * testTxAllocateConnection.
    * @throws Throwable for exception
    */
   @Test
   public void testAllocateConnection() throws Throwable
   {
      ConnectionManager connectionManager = embedded.lookup("ConnectionManagerTx", ConnectionManager.class);
      assertNotNull(connectionManager);
      
      assertTrue(connectionManager.getRealConnectionManager() instanceof TxConnectionManager);
      
      TxConnectionManager txConnectionManager = (TxConnectionManager)connectionManager.getRealConnectionManager();
      txConnectionManager.setLocalTransactions(false);
      
      TransactionManager transactionManager = txConnectionManager.getTransactionManager();      
      assertNotNull(transactionManager);
      
      TransactionSynchronizer.setTransactionManager(transactionManager);
      
      transactionManager.begin();
      
      ManagedConnectionFactory mcf = new MockManagedConnectionFactory();
      PoolParams params = new PoolParams();      
      
      OnePool onePool = new OnePool(mcf, params, true);
      onePool.setConnectionListenerFactory(txConnectionManager);
      
      txConnectionManager.setPoolingStrategy(onePool);
      
      Object handle = connectionManager.allocateConnection(mcf, new MockConnectionRequestInfo());
      assertNotNull(handle);
      
      assertTrue(handle instanceof MockHandle);
      
      transactionManager.commit();
      
   }
   
   /**
    * testEnlistInExistingTx.
    * @throws Exception for exception
    */
   @Test
   public void testEnlistInExistingTx() throws Exception
   {
      
   }
   
   /**
    * testEnlistCheckedOutConnectionInNewTx.
    * @throws Exception for exception
    */
   @Test
   public void testEnlistCheckedOutConnectionInNewTx() throws Exception
   {
      
   }
   
   /**
    * testReconnectConnectionHandlesOnNotification.
    * @throws Exception for exception.
    */
   @Test
   public void testReconnectConnectionHandlesOnNotification() throws Exception
   {
      
   }
   
   /**
    * testEnlistAfterMarkRollback.
    * @throws Exception for exception
    */
   @Test
   public void testEnlistAfterMarkRollback() throws Exception
   {
      
   }
   
   /**
    * testBrokenConnectionAndTrackByTx.
    * @throws Exception for exception.
    */
   @Test
   public void testBrokenConnectionAndTrackByTx() throws Exception
   {
      
   }
   
   /**
    * testFailedStartTx.
    * @throws Exception for exception.
    */
   @Test   
   public void testFailedStartTx() throws Exception
   {
      
   }
   
   /**
    * testFailedEndTx.
    * @throws Exception for exception.
    */
   @Test   
   public void testFailedEndTx() throws Exception
   {
      
   }
   
   
   /**
    * Lifecycle start, before the suite is executed
    * @throws Throwable throwable exception 
    */
   @BeforeClass
   public static void beforeClass() throws Throwable
   {
      // Create and set an embedded JCA instance
      embedded = new EmbeddedJCA(false);

      // Startup
      embedded.startup();

      // Deploy Naming, Transaction and WorkManager
      embedded.deploy(WorkManagerTestCase.class.getClassLoader(), "naming-jboss-beans.xml");
      embedded.deploy(WorkManagerTestCase.class.getClassLoader(), "transaction-jboss-beans.xml");
      embedded.deploy(WorkManagerTestCase.class.getClassLoader(), "connectionmanager-jboss-beans.xml");
   }
   
   /**
    * Lifecycle stop, after the suite is executed
    * @throws Throwable throwable exception 
    */
   @AfterClass
   public static void afterClass() throws Throwable
   {
      // Undeploy WorkManager, Transaction and Naming
      embedded.undeploy(WorkManagerTestCase.class.getClassLoader(), "connectionmanager-jboss-beans.xml");
      embedded.undeploy(WorkManagerTestCase.class.getClassLoader(), "transaction-jboss-beans.xml");
      embedded.undeploy(WorkManagerTestCase.class.getClassLoader(), "naming-jboss-beans.xml");

      // Shutdown embedded
      embedded.shutdown();

      // Set embedded to null
      embedded = null;
   }   
   
}